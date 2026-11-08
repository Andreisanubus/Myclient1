package dev.myclient.gui;

import dev.myclient.config.ConfigManager;
import dev.myclient.config.FriendManager;
import dev.myclient.config.StaffManager;
import dev.myclient.config.Theme;
import dev.myclient.hud.Hud;
import dev.myclient.module.Category;
import dev.myclient.module.HeaderSetting;
import dev.myclient.module.HudModule;
import dev.myclient.module.ModeSetting;
import dev.myclient.module.Module;
import dev.myclient.module.ModuleManager;
import dev.myclient.module.NumberSetting;
import dev.myclient.module.Setting;
import dev.myclient.util.Keys;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.*;

public class ClickGuiScreen extends Screen {
    private record Hit(int x, int y, int w, int h, Runnable l, Runnable r) {}
    private record Slider(int x, int y, int w, NumberSetting s) {}
    private static class Box { String t = ""; }

    private static final String[] TABS = {"Modules", "HUD", "Friends", "Staff", "Theme", "Config"};
    private static final int GRAY = 0xFF8A8A8A, WHITE = 0xFFFFFFFF, DIM = 0xFF5A5A5A;

    private final List<Hit> hits = new ArrayList<>();
    private final List<Slider> sliders = new ArrayList<>();
    private final Set<Category> closed = new HashSet<>();
    private final Set<String> pick = new LinkedHashSet<>(ConfigManager.SECTIONS);
    private final Box search = new Box(), friendBox = new Box(), staffBox = new Box(), cfgName = new Box(), cfgCode = new Box();
    private Box focus;
    private Module binding, expanded;
    private Slider dragging;
    private HudModule dragHud;
    private int dragDX, dragDY;
    private int tab, scroll, contentH;
    private String status = "";
    private long statusUntil;

    public ClickGuiScreen() { super(Text.literal("MyClient")); }

    @Override public boolean shouldPause() { return false; }

    @Override public void close() {
        ConfigManager.save("default", new LinkedHashSet<>(ConfigManager.SECTIONS));
        super.close();
    }

    // ------------------------------------------------------------ helpers
    private static boolean in(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
    private void hit(int x, int y, int w, int h, Runnable l, Runnable r) { hits.add(new Hit(x, y, w, h, l, r)); }
    private void say(String s) { status = s; statusUntil = System.currentTimeMillis() + 2500; }
    private void txt(DrawContext c, String s, int x, int y, int color) { c.drawText(textRenderer, s, x, y, color, false); }
    private int tw(String s) { return textRenderer.getWidth(s); }

    private void box(DrawContext c, int x, int y, int w, int h, String title) {
        RenderUtil.roundedRect(c, x, y, w, h, Theme.radius, Theme.panel());
        txt(c, title, x + 8, y + 7, WHITE);
    }

    private void button(DrawContext c, int x, int y, int w, int h, String label, int mx, int my, Runnable l) {
        boolean hov = in(mx, my, x, y, w, h);
        RenderUtil.roundedRect(c, x, y, w, h, Math.max(2, Theme.radius / 2), hov ? Theme.accent : 0x33FFFFFF);
        txt(c, label, x + (w - tw(label)) / 2, y + (h - 8) / 2 + 1, WHITE);
        hit(x, y, w, h, l, null);
    }

    private void simpleRow(DrawContext c, int x, int y, int w, String label, boolean on, int mx, int my, Runnable l) {
        int r = Math.max(1, Theme.radius / 2);
        if (on) RenderUtil.roundedRect(c, x, y, w, 15, r, Theme.accent);
        else if (in(mx, my, x, y, w, 15)) RenderUtil.roundedRect(c, x, y, w, 15, r, 0x22FFFFFF);
        txt(c, textRenderer.trimToWidth(label, w - 8), x + 4, y + 4, on ? WHITE : GRAY);
        hit(x, y, w, 15, l, null);
    }

    private void field(DrawContext c, int x, int y, int w, Box b, String placeholder) {
        boolean f = focus == b;
        RenderUtil.roundedRect(c, x, y, w, 14, 3, f ? 0x44FFFFFF : 0x22FFFFFF);
        boolean ph = b.t.isEmpty() && !f;
        String s = ph ? placeholder : b.t + (f && (System.currentTimeMillis() / 500) % 2 == 0 ? "_" : "");
        txt(c, textRenderer.trimToWidth(s, w - 8, f), x + 4, y + 3, ph ? 0xFF777777 : WHITE);
        hit(x, y, w, 14, () -> focus = b, null);
    }

    // ------------------------------------------------------------ render
    @Override
    public void render(DrawContext c, int mx, int my, float delta) {
        hits.clear();
        sliders.clear();
        if (tab != 1) c.fill(0, 0, width, height, 0x99000000);
        switch (tab) {
            case 0 -> modulesTab(c, mx, my);
            case 1 -> hudTab(c, mx, my);
            case 2 -> friendsTab(c, mx, my);
            case 3 -> staffTab(c, mx, my);
            case 4 -> themeTab(c, mx, my);
            default -> configTab(c, mx, my);
        }
        tabBar(c, mx, my);
        if (System.currentTimeMillis() < statusUntil) txt(c, status, (width - tw(status)) / 2, height - 40, WHITE);
    }

    // ------------------------------------------------------------ MODULES
    private List<Module> results() {
        String q = search.t.toLowerCase();
        if (q.isEmpty()) return List.of();
        return ModuleManager.get().all().stream().filter(m -> m.name.toLowerCase().contains(q)).limit(6).toList();
    }

    private int panelHeight(Category cat) {
        int h = 22;
        if (!closed.contains(cat)) { for (Module m : ModuleManager.get().in(cat)) h += blockHeight(m); h += 4; }
        return h;
    }

    private int searchHeight() {
        int h = 44;
        for (Module m : results()) h += blockHeight(m);
        return h;
    }

    private void modulesTab(DrawContext c, int mx, int my) {
        List<Category> cats = Arrays.stream(Category.values()).filter(k -> !ModuleManager.get().in(k).isEmpty()).toList();
        int gap = 6, margin = 8, pw = 112, x = margin, y = 10 - scroll, rowH = 0, maxB = 0, total = cats.size() + 1;
        for (int i = 0; i < total; i++) {
            boolean isSearch = i == cats.size();
            int h = isSearch ? searchHeight() : panelHeight(cats.get(i));
            if (x + pw > width - margin && x > margin) { x = margin; y += rowH + gap; rowH = 0; }
            if (isSearch) searchPanel(c, x, y, pw, mx, my); else categoryPanel(c, cats.get(i), x, y, pw, mx, my);
            rowH = Math.max(rowH, h);
            maxB = Math.max(maxB, y + h + scroll);
            x += pw + gap;
        }
        contentH = maxB;
        txt(c, "* = modul inregistrat, logica nu e scrisa inca", 10, height - 56, DIM);
    }

    private void categoryPanel(DrawContext c, Category cat, int x, int y, int w, int mx, int my) {
        boolean open = !closed.contains(cat);
        RenderUtil.roundedRect(c, x, y, w, panelHeight(cat), Theme.radius, Theme.panel());
        txt(c, cat.label, x + 8, y + 7, WHITE);
        txt(c, open ? "v" : ">", x + w - 12, y + 7, GRAY);
        hit(x, y, w, 22, () -> { if (!closed.remove(cat)) closed.add(cat); }, null);
        if (!open) return;
        int ry = y + 22;
        for (Module m : ModuleManager.get().in(cat)) ry += moduleBlock(c, m, x + 4, ry, w - 8, mx, my);
    }

    private void searchPanel(DrawContext c, int x, int y, int w, int mx, int my) {
        RenderUtil.roundedRect(c, x, y, w, searchHeight(), Theme.radius, Theme.panel());
        txt(c, "Search", x + 8, y + 7, WHITE);
        field(c, x + 6, y + 22, w - 12, search, "cauta...");
        int ry = y + 42;
        for (Module m : results()) ry += moduleBlock(c, m, x + 4, ry, w - 8, mx, my);
    }

    private List<Setting<?>> vis(Module m) {
        List<Setting<?>> l = new ArrayList<>();
        for (Setting<?> s : m.settings) if (s.visible.getAsBoolean()) l.add(s);
        return l;
    }

    private int blockHeight(Module m) {
        if (expanded != m) return 16;
        int h = 16;
        for (Setting<?> s : vis(m)) h += s instanceof NumberSetting ? 22 : s instanceof HeaderSetting ? 12 : 16;
        return h + 16 + 16 + 2;
    }

    private int moduleBlock(DrawContext c, Module m, int x, int y, int w, int mx, int my) {
        boolean on = m.isEnabled(), ex = expanded == m;
        int r = Math.max(1, Theme.radius / 2);
        if (on) RenderUtil.roundedRect(c, x, y, w, 15, r, Theme.accent);
        else if (in(mx, my, x, y, w, 15)) RenderUtil.roundedRect(c, x, y, w, 15, r, 0x22FFFFFF);

        String label = m.implemented() ? m.name : m.name + "*";
        txt(c, textRenderer.trimToWidth(label, w - 16), x + 4, y + 4, on ? WHITE : (m.implemented() ? GRAY : DIM));
        if (!m.settings.isEmpty()) txt(c, ex ? "-" : "+", x + w - 9, y + 4, on ? WHITE : DIM);
        hit(x, y, w, 15, m::toggle, () -> expanded = ex ? null : m);

        if (!ex) return 16;

        int total = blockHeight(m);
        RenderUtil.roundedRect(c, x, y + 16, w, total - 18, 3, 0x22FFFFFF);
        int ix = x + 3, iw = w - 6, cy = y + 17;
        for (Setting<?> s : vis(m)) cy += settingRow(c, s, ix, cy, iw, mx, my);

        String k = binding == m ? "..." : Keys.name(m.key);
        txt(c, "Bind", ix + 3, cy + 3, GRAY);
        txt(c, k, ix + iw - tw(k) - 3, cy + 3, WHITE);
        hit(ix, cy, iw, 14, () -> binding = m, () -> m.key = -1);
        cy += 16;

        int bw = (iw - 3) / 2;
        button(c, ix, cy, bw, 13, "Copy Config", mx, my, () -> {
            Keys.setClipboard(ConfigManager.exportModule(m));
            say("Config copiat: " + m.name);
        });
        button(c, ix + bw + 3, cy, bw, 13, "Paste Config", mx, my,
                () -> say(ConfigManager.importModule(m, Keys.getClipboard()) ? "Config lipit: " + m.name : "Cod invalid pentru " + m.name));
        return total;
    }

    @SuppressWarnings("unchecked")
    private int settingRow(DrawContext c, Setting<?> s, int x, int y, int w, int mx, int my) {
        if (s instanceof HeaderSetting hs) { txt(c, hs.value.toUpperCase(), x + 3, y + 3, DIM); return 12; }
        String label = textRenderer.trimToWidth(s.name, w - 34);
        if (s instanceof NumberSetting n) {
            String v = String.format(n.step >= 1 ? "%.0f" : "%.2f", n.value);
            txt(c, textRenderer.trimToWidth(s.name, w - tw(v) - 10), x + 3, y + 2, GRAY);
            txt(c, v, x + w - tw(v) - 3, y + 2, WHITE);
            int tx = x + 3, tww = w - 6, ty = y + 14;
            RenderUtil.roundedRect(c, tx, ty, tww, 4, 2, 0x33FFFFFF);
            double t = (n.value - n.min) / (n.max - n.min);
            RenderUtil.roundedRect(c, tx, ty, Math.max(4, (int) (tww * t)), 4, 2, Theme.accent);
            sliders.add(new Slider(tx, ty, tww, n));
            return 22;
        }
        if (s instanceof ModeSetting ms) {
            txt(c, label, x + 3, y + 3, GRAY);
            String v = textRenderer.trimToWidth(ms.value, w / 2);
            txt(c, v, x + w - tw(v) - 3, y + 3, WHITE);
            hit(x, y, w, 15, ms::next, ms::prev);
            return 16;
        }
        if (s.value instanceof Boolean b) {
            txt(c, label, x + 3, y + 3, GRAY);
            RenderUtil.roundedRect(c, x + w - 24, y + 2, 21, 11, 3, b ? Theme.accent : 0x33FFFFFF);
            String t = b ? "ON" : "OFF";
            txt(c, t, x + w - 24 + (21 - tw(t)) / 2, y + 4, WHITE);
            hit(x, y, w, 15, () -> ((Setting<Boolean>) s).value = !b, null);
            return 16;
        }
        return 0;
    }

    private void updateSlider(double mx) {
        if (dragging == null) return;
        double t = Math.max(0, Math.min(1, (mx - dragging.x()) / (double) dragging.w()));
        NumberSetting n = dragging.s();
        n.set(n.min + t * (n.max - n.min));
    }

    // ------------------------------------------------------------ HUD
    private List<HudModule> hudMods() {
        List<HudModule> l = new ArrayList<>();
        for (Module m : ModuleManager.get().all()) if (m instanceof HudModule hm) l.add(hm);
        return l;
    }

    private void hudTab(DrawContext c, int mx, int my) {
        List<HudModule> hm = hudMods();
        int w = 170, x = 10, y = 10;
        box(c, x, y, w, 36 + (Hud.toggles.size() + hm.size()) * 17, "HUD");
        int ry = y + 22;
        for (String key : new ArrayList<>(Hud.toggles.keySet())) {
            simpleRow(c, x + 6, ry, w - 12, key, Hud.toggles.get(key), mx, my, () -> Hud.toggles.compute(key, (k, v) -> !v));
            ry += 17;
        }
        for (HudModule m : hm) { simpleRow(c, x + 6, ry, w - 12, m.name, m.isEnabled(), mx, my, m::toggle); ry += 17; }
        txt(c, "Trage elementele de pe ecran ca sa le muti", x + 6, ry + 3, DIM);
        for (HudModule m : hm) {
            if (!m.isEnabled()) continue;
            int a = m.lastX - 2, b = m.lastY - 2, d = m.lastX + m.w + 2, e = m.lastY + m.h + 2;
            c.fill(a, b, d, b + 1, Theme.accent); c.fill(a, e - 1, d, e, Theme.accent);
            c.fill(a, b, a + 1, e, Theme.accent); c.fill(d - 1, b, d, e, Theme.accent);
            txt(c, m.name, a, b - 10, WHITE);
        }
    }

    // ------------------------------------------------------------ FRIENDS
    private void addFriend() { FriendManager.add(friendBox.t); friendBox.t = ""; }

    private void friendsTab(DrawContext c, int mx, int my) {
        int w = 220, x = (width - w) / 2, y = 30;
        List<String> list = new ArrayList<>(FriendManager.friends);
        box(c, x, y, w, 46 + list.size() * 17, "Friends");
        field(c, x + 6, y + 22, w - 60, friendBox, "nume (Bedrock: .nume)");
        button(c, x + w - 48, y + 22, 42, 14, "Add", mx, my, this::addFriend);
        int ry = y + 42;
        for (String name : list) {
            RenderUtil.roundedRect(c, x + 6, ry, w - 12, 15, 3, 0x22FFFFFF);
            txt(c, name, x + 10, ry + 4, WHITE);
            button(c, x + w - 26, ry + 1, 16, 13, "x", mx, my, () -> FriendManager.friends.remove(name));
            ry += 17;
        }
    }

    // ------------------------------------------------------------ STAFF
    private void addStaff() { StaffManager.add(staffBox.t); staffBox.t = ""; }

    private void staffTab(DrawContext c, int mx, int my) {
        int w = 220, x = (width - w) / 2, y = 30;
        List<String> list = new ArrayList<>(StaffManager.staff);
        box(c, x, y, w, 62 + list.size() * 17, "Staff");
        field(c, x + 6, y + 22, w - 60, staffBox, "nume staff");
        button(c, x + w - 48, y + 22, 42, 14, "Add", mx, my, this::addStaff);
        int ry = y + 42;
        for (String name : list) {
            RenderUtil.roundedRect(c, x + 6, ry, w - 12, 15, 3, 0x22FFFFFF);
            txt(c, name, x + 10, ry + 4, WHITE);
            button(c, x + w - 26, ry + 1, 16, 13, "x", mx, my, () -> StaffManager.staff.remove(name));
            ry += 17;
        }
        txt(c, "Apar in Staff List (modulul din Donut) cand sunt online", x + 6, ry + 3, DIM);
    }

    // ------------------------------------------------------------ THEME
    private void themeTab(DrawContext c, int mx, int my) {
        int w = 220, x = (width - w) / 2, y = 30;
        box(c, x, y, w, 116, "Theme");
        txt(c, "Accent", x + 8, y + 24, GRAY);
        for (int i = 0; i < Theme.PRESETS.length; i++) {
            int col = Theme.PRESETS[i], sx = x + 8 + i * 22, sy = y + 36;
            if (col == Theme.accent) RenderUtil.roundedRect(c, sx - 2, sy - 2, 20, 20, 5, WHITE);
            RenderUtil.roundedRect(c, sx, sy, 16, 16, 4, col);
            hit(sx, sy, 16, 16, () -> Theme.accent = col, null);
        }
        txt(c, "Opacity: " + Theme.opacity + "%", x + 8, y + 62, GRAY);
        button(c, x + w - 52, y + 59, 20, 14, "-", mx, my, () -> Theme.opacity = Math.max(30, Theme.opacity - 5));
        button(c, x + w - 28, y + 59, 20, 14, "+", mx, my, () -> Theme.opacity = Math.min(100, Theme.opacity + 5));
        txt(c, "Corner radius: " + Theme.radius, x + 8, y + 82, GRAY);
        button(c, x + w - 52, y + 79, 20, 14, "-", mx, my, () -> Theme.radius = Math.max(0, Theme.radius - 1));
        button(c, x + w - 28, y + 79, 20, 14, "+", mx, my, () -> Theme.radius = Math.min(20, Theme.radius + 1));
        button(c, x + 8, y + 98, 70, 13, "Reset theme", mx, my, Theme::reset);
    }

    // ------------------------------------------------------------ CONFIG
    private void saveProfile() {
        String n = ConfigManager.safe(cfgName.t);
        if (n.isEmpty()) { say("Scrie un nume (litere/cifre)"); return; }
        say(ConfigManager.save(n, pick) ? "Salvat: " + n : "Eroare la salvare");
        cfgName.t = "";
    }

    private void configTab(DrawContext c, int mx, int my) {
        List<String> profiles = ConfigManager.list();
        int w = 270, x = (width - w) / 2, y = 30, sec = ConfigManager.SECTIONS.size();
        box(c, x, y, w, 42 + sec * 17 + 14 + profiles.size() * 17 + 4 + 62, "Config");
        field(c, x + 6, y + 22, w - 60, cfgName, "nume profil");
        button(c, x + w - 48, y + 22, 42, 14, "Save", mx, my, this::saveProfile);

        int ry = y + 42;
        for (String s : ConfigManager.SECTIONS) {
            simpleRow(c, x + 6, ry, w - 12, s, pick.contains(s), mx, my, () -> { if (!pick.remove(s)) pick.add(s); });
            ry += 17;
        }
        txt(c, "Profiles", x + 8, ry + 2, GRAY);
        ry += 14;
        for (String p : profiles) {
            RenderUtil.roundedRect(c, x + 6, ry, w - 12, 15, 3, 0x22FFFFFF);
            txt(c, p, x + 10, ry + 4, WHITE);
            button(c, x + w - 66, ry + 1, 36, 13, "Load", mx, my, () -> say(ConfigManager.load(p) ? "Incarcat: " + p : "Eroare"));
            button(c, x + w - 26, ry + 1, 16, 13, "x", mx, my, () -> ConfigManager.delete(p));
            ry += 17;
        }
        ry += 4;
        txt(c, "Share code", x + 8, ry, GRAY);
        field(c, x + 6, ry + 10, w - 12, cfgCode, "cod (Ctrl+V ca sa lipesti)");
        int bw = (w - 12 - 6) / 3, by = ry + 28;
        button(c, x + 6, by, bw, 14, "Export", mx, my, () -> {
            cfgCode.t = ConfigManager.export(pick);
            Keys.setClipboard(cfgCode.t);
            say("Cod exportat si copiat");
        });
        button(c, x + 6 + bw + 3, by, bw, 14, "Copy", mx, my, () -> { Keys.setClipboard(cfgCode.t); say("Copiat"); });
        button(c, x + 6 + 2 * (bw + 3), by, bw, 14, "Import", mx, my, () -> {
            String code = cfgCode.t.isEmpty() ? Keys.getClipboard() : cfgCode.t;
            say(ConfigManager.importCode(code) ? "Importat" : "Cod invalid");
        });
    }

    // ------------------------------------------------------------ TAB BAR
    private void tabBar(DrawContext c, int mx, int my) {
        int pad = 10, gap = 4, total = -gap;
        for (String t : TABS) total += tw(t) + 2 * pad + gap;
        int bx = (width - total) / 2 - 5, by = height - 26;
        RenderUtil.roundedRect(c, bx, by, total + 10, 20, 8, 0xF00C0C0E);
        int x = bx + 5;
        for (int i = 0; i < TABS.length; i++) {
            int w = tw(TABS[i]) + 2 * pad, idx = i;
            if (i == tab) RenderUtil.roundedRect(c, x, by + 3, w, 14, 6, Theme.accent);
            txt(c, TABS[i], x + pad, by + 6, i == tab ? WHITE : GRAY);
            hit(x, by + 3, w, 14, () -> { tab = idx; focus = null; binding = null; }, null);
            x += w + gap;
        }
    }

    // ------------------------------------------------------------ input (API 1.21.9+: Click / KeyInput / CharInput)
    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mx = click.x(), my = click.y();
        int button = click.button();
        focus = null;
        binding = null;
        if (button == 0) {
            for (Slider s : sliders)
                if (in(mx, my, s.x(), s.y() - 3, s.w(), 10)) { dragging = s; updateSlider(mx); return true; }
        }
        for (Hit h : hits) {
            if (!in(mx, my, h.x(), h.y(), h.w(), h.h())) continue;
            Runnable r = button == 0 ? h.l() : (button == 1 ? h.r() : null);
            if (r != null) { r.run(); return true; }
        }
        if (tab == 1 && button == 0) {
            for (HudModule m : hudMods()) {
                if (m.isEnabled() && in(mx, my, m.lastX - 2, m.lastY - 2, m.w + 4, m.h + 4)) {
                    dragHud = m; dragDX = (int) mx - m.lastX; dragDY = (int) my - m.lastY;
                    return true;
                }
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(Click click, double dx, double dy) {
        if (dragHud != null) { dragHud.moveTo((int) click.x() - dragDX, (int) click.y() - dragDY, width, height); return true; }
        if (dragging != null) { updateSlider(click.x()); return true; }
        return super.mouseDragged(click, dx, dy);
    }

    @Override
    public boolean mouseReleased(Click click) {
        dragging = null;
        dragHud = null;
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hAmount, double vAmount) {
        if (tab == 0) scroll = Math.max(0, Math.min(Math.max(0, contentH - (height - 44)), scroll - (int) (vAmount * 20)));
        return true;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int key = input.key();
        if (binding != null) {
            if (key == GLFW.GLFW_KEY_ESCAPE) { /* anuleaza */ }
            else if (key == GLFW.GLFW_KEY_BACKSPACE || key == GLFW.GLFW_KEY_DELETE) binding.key = -1;
            else binding.key = key;
            binding = null;
            return true;
        }
        if (focus != null) {
            if (key == GLFW.GLFW_KEY_ESCAPE) { focus = null; return true; }
            if (key == GLFW.GLFW_KEY_BACKSPACE) {
                if (!focus.t.isEmpty()) focus.t = focus.t.substring(0, focus.t.length() - 1);
                return true;
            }
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                if (focus == friendBox) addFriend();
                else if (focus == staffBox) addStaff();
                else if (focus == cfgName) saveProfile();
                return true;
            }
            if (key == GLFW.GLFW_KEY_V && input.hasCtrl()) {
                focus.t += Keys.getClipboard().replaceAll("[\\r\\n]", "");
                return true;
            }
            return true;
        }
        if (key == GLFW.GLFW_KEY_RIGHT_SHIFT) { close(); return true; }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharInput input) {
        if (focus != null && input.isValidChar() && focus.t.length() < 4000) {
            focus.t += input.asString();
            return true;
        }
        return super.charTyped(input);
    }
}
