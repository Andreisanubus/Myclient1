package dev.myclient.module.modules;

import dev.myclient.config.FriendManager;
import dev.myclient.config.StaffManager;
import dev.myclient.module.Category;
import dev.myclient.module.Module;
import dev.myclient.module.NumberSetting;
import dev.myclient.module.Setting;
import dev.myclient.util.Names;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import java.util.*;

/**
 * Detectie de vanish din semnale pe care le vede clientul:
 * 1) un nume dispare din tab list fara niciun mesaj de iesire in chat;
 * 2) un jucator aflat aproape dispare din lume, dar ramane in tab list.
 * Nu exista o metoda sigura 100%: serverul poate ascunde si tab list-ul. Alerta arata numele jucatorului pe ecran.
 */
public class AntiVanish extends Module {
    private record Msg(String text, int tick) {}
    private record Seen(String name, double x, double y, double z, float hp) {}
    private record Alert(String name, String reason, long until, boolean staff) {}

    private final Setting<Boolean> checkTab, checkNear, onlyStaff, ignoreFriends, chat, screen, sound;
    private final NumberSetting window, close, seconds;

    private final Deque<Msg> msgs = new ArrayDeque<>();
    private final Deque<Alert> alerts = new ArrayDeque<>();
    private final Map<String, Integer> missing = new HashMap<>();
    private final Map<UUID, Seen> seen = new HashMap<>();
    private final Map<String, Integer> pending = new HashMap<>();
    private Set<String> prevTab = new HashSet<>();
    private Object lastWorld;
    private double lx, lz;
    private int tick;

    public AntiVanish() {
        super("Anti Vanish", Category.DONUT);
        header("Detection");
        checkTab = bool("Check Tab List", true);
        window = num("Leave Message Window (ticks)", 60, 20, 200, 5);
        checkNear = bool("Check Disappear Near", true);
        close = num("Near Range (blocks)", 24, 6, 64, 2);
        onlyStaff = bool("Only Staff List Names", false);
        ignoreFriends = bool("Ignore Friends", true);
        header("Alerts");
        chat = bool("Chat Alert", true);
        screen = bool("Screen Alert", true);
        seconds = num("Alert Seconds", 6, 2, 20, 1);
        sound = bool("Sound", true);
        window.visible = () -> checkTab.value;
        close.visible = () -> checkNear.value;
        seconds.visible = () -> screen.value;
        ClientReceiveMessageEvents.GAME.register((msg, overlay) -> {
            if (!isEnabled()) return;
            msgs.addLast(new Msg(msg.getString().toLowerCase(Locale.ROOT), tick));
            while (msgs.size() > 80) msgs.removeFirst();
        });
    }

    @Override protected void onEnable() { reset(); }
    @Override protected void onDisable() { reset(); alerts.clear(); }

    private void reset() { missing.clear(); seen.clear(); pending.clear(); prevTab = new HashSet<>(); msgs.clear(); }

    private boolean leftMessage(String name, int since) {
        String n = name.toLowerCase(Locale.ROOT);
        for (Msg m : msgs) {
            if (m.tick() < since || !m.text().contains(n)) continue;
            String t = m.text();
            if (t.contains("left") || t.contains("quit") || t.contains("disconnect") || t.contains("logged out") || t.contains("leave")) return true;
        }
        return false;
    }

    @Override public void onTick() {
        var p = mc.player;
        if (mc.world == null || p == null) return;
        tick++;
        if (lastWorld != mc.world || Math.hypot(p.getX() - lx, p.getZ() - lz) > 12) { reset(); lastWorld = mc.world; }
        lx = p.getX(); lz = p.getZ();
        String self = p.getName().getString();
        Set<String> tab = Names.tab(mc);

        if (checkTab.value) {
            if (!prevTab.isEmpty())
                for (String n : prevTab) if (!tab.contains(n) && !n.equals(self)) missing.putIfAbsent(n, tick);
            for (Iterator<Map.Entry<String, Integer>> it = missing.entrySet().iterator(); it.hasNext(); ) {
                var e = it.next();
                if (tab.contains(e.getKey())) { it.remove(); continue; }
                if (tick - e.getValue() >= window.asInt()) {
                    it.remove();
                    if (!leftMessage(e.getKey(), e.getValue() - 10)) alert(e.getKey(), "disparut din tab list fara mesaj de iesire");
                }
            }
        }
        prevTab = tab;

        if (checkNear.value) {
            Set<UUID> now = new HashSet<>();
            for (PlayerEntity pl : mc.world.getPlayers()) {
                if (pl == p) continue;
                now.add(pl.getUuid());
                seen.put(pl.getUuid(), new Seen(pl.getName().getString(), pl.getX(), pl.getY(), pl.getZ(), pl.getHealth()));
            }
            for (Iterator<Map.Entry<UUID, Seen>> it = seen.entrySet().iterator(); it.hasNext(); ) {
                var e = it.next();
                if (now.contains(e.getKey())) continue;
                it.remove();
                Seen s = e.getValue();
                double d = Math.hypot(p.getX() - s.x(), p.getZ() - s.z());
                if (d <= close.get() && s.hp() > 0 && tab.contains(s.name())) pending.put(s.name(), tick);
            }
            for (Iterator<Map.Entry<String, Integer>> it = pending.entrySet().iterator(); it.hasNext(); ) {
                var e = it.next();
                boolean visibleAgain = mc.world.getPlayers().stream().anyMatch(x -> x.getName().getString().equals(e.getKey()));
                if (visibleAgain || !tab.contains(e.getKey())) { it.remove(); continue; }
                if (tick - e.getValue() >= 4) { it.remove(); alert(e.getKey(), "a disparut din apropiere, dar e inca in tab list"); }
            }
        }
    }

    private void alert(String name, String reason) {
        boolean staff = StaffManager.isStaff(name);
        if (onlyStaff.value && !staff) return;
        if (ignoreFriends.value && FriendManager.isFriend(name)) return;
        for (Alert a : alerts) if (a.name().equals(name) && a.until() > System.currentTimeMillis() + 2000) return;
        alerts.addLast(new Alert(name, reason, System.currentTimeMillis() + (long) (seconds.get() * 1000), staff));
        while (alerts.size() > 3) alerts.removeFirst();
        if (chat.value && mc.player != null) mc.player.sendMessage(Text.literal("[Anti Vanish] " + name + (staff ? " (STAFF)" : "") + ": " + reason), false);
        if (sound.value && mc.player != null) mc.player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 0.6f);
    }

    @Override public void onRender(DrawContext c, int sw, int sh) {
        if (!screen.value) return;
        long now = System.currentTimeMillis();
        alerts.removeIf(a -> a.until() < now);
        int y = 40;
        for (Alert a : alerts) {
            String title = "VANISH  " + a.name() + (a.staff() ? "  [STAFF]" : "");
            int w = Math.max(mc.textRenderer.getWidth(title), mc.textRenderer.getWidth(a.reason())) + 30;
            int pulse = (int) (140 + 90 * Math.sin(now / 120.0));
            c.fill(sw / 2 - w / 2 - 1, y - 1, sw / 2 + w / 2 + 1, y + 31, (pulse << 24) | 0xEF4444);
            c.fill(sw / 2 - w / 2, y, sw / 2 + w / 2, y + 30, 0xE0100810);
            int tx = sw / 2 - mc.textRenderer.getWidth(title) / 2;
            c.drawText(mc.textRenderer, title, tx + 1, y + 6, 0xFFFFFFFF, true);
            c.drawText(mc.textRenderer, title, tx, y + 6, 0xFFFFFFFF, true);
            c.drawText(mc.textRenderer, a.reason(), sw / 2 - mc.textRenderer.getWidth(a.reason()) / 2, y + 20, 0xFFFCA5A5, false);
            y += 36;
        }
    }
}
