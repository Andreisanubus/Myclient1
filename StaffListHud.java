package dev.myclient.module.modules;

import dev.myclient.config.StaffManager;
import dev.myclient.config.Theme;
import dev.myclient.gui.RenderUtil;
import dev.myclient.module.Category;
import dev.myclient.module.HudModule;
import dev.myclient.module.NumberSetting;
import dev.myclient.module.Setting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import java.util.*;

/** HUD cu staff-ul aflat pe server. Numele se adauga din tabul Staff. Se muta din tabul HUD. */
public class StaffListHud extends HudModule {
    private final Setting<Boolean> title, count, accentBar, alpha, joinChat;
    private final NumberSetting bg, maxEntries;
    private Set<String> prevOnline = new HashSet<>();
    private Object lastWorld;
    private final Map<String, Long> joined = new HashMap<>();

    public StaffListHud() {
        super("Staff List", Category.DONUT, 2, 30);
        header("Display");
        title = bool("Show Title", true);
        count = bool("Show Count", true);
        accentBar = bool("Accent Bar", true);
        alpha = bool("Background", true);
        bg = num("Background Alpha %", 70, 10, 100, 5);
        maxEntries = num("Max Entries", 10, 1, 30, 1);
        header("Alerts");
        joinChat = bool("Chat Alert On Join", true);
        bg.visible = () -> alpha.value;
    }

    @Override protected void onDisable() { prevOnline = new HashSet<>(); lastWorld = null; }

    private List<String> online() {
        List<String> out = new ArrayList<>();
        var h = mc.getNetworkHandler();
        if (h == null) return out;
        for (String s : StaffManager.staff) if (h.getPlayerListEntry(s) != null) out.add(s);
        Collections.sort(out, String.CASE_INSENSITIVE_ORDER);
        return out;
    }

    @Override public void onTick() {
        if (mc.world == null || mc.player == null) return;
        Set<String> now = new HashSet<>(online());
        if (lastWorld != mc.world) { lastWorld = mc.world; prevOnline = now; return; }
        for (String n : now) {
            if (prevOnline.contains(n)) continue;
            joined.put(n, System.currentTimeMillis());
            if (joinChat.value) mc.player.sendMessage(Text.literal("[Staff] " + n + " a intrat pe server"), false);
        }
        prevOnline = now;
    }

    @Override protected int[] draw(DrawContext c, int x, int y, int sw, int sh) {
        List<String> on = online();
        boolean editing = mc.currentScreen != null;
        if (on.isEmpty() && !editing) return new int[]{70, 14};
        List<String> shown = on.isEmpty() ? List.of("(niciun staff online)") : on.subList(0, Math.min(on.size(), maxEntries.asInt()));
        String head = "Staff" + (count.value ? " (" + on.size() + ")" : "");
        int w = mc.textRenderer.getWidth(head);
        for (String s : shown) w = Math.max(w, mc.textRenderer.getWidth(s) + 12);
        w += 16;
        int h = (title.value ? 16 : 6) + shown.size() * 11 + 4;
        if (alpha.value) RenderUtil.roundedRect(c, x, y, w, h, Theme.radius, ((int) (bg.get() * 2.55) << 24) | 0x0C0C0E);
        if (accentBar.value) c.fill(x + 3, y + 3, x + 5, y + h - 3, Theme.accent);
        int ty = y + 5;
        if (title.value) { c.drawText(mc.textRenderer, head, x + 9, ty, 0xFFFFFFFF, true); ty += 11; }
        long now = System.currentTimeMillis();
        for (String s : shown) {
            boolean fresh = joined.containsKey(s) && now - joined.get(s) < 4000;
            c.drawText(mc.textRenderer, on.isEmpty() ? s : "\u25CF " + s, x + 9, ty, on.isEmpty() ? 0xFF6B7280 : (fresh ? 0xFFFCA5A5 : 0xFF86EFAC), true);
            ty += 11;
        }
        return new int[]{w, h};
    }
}
