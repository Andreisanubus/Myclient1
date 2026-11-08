package dev.myclient.module.modules;

import dev.myclient.module.Category;
import dev.myclient.module.Module;
import dev.myclient.module.ModeSetting;
import dev.myclient.module.NumberSetting;
import dev.myclient.module.Setting;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;

/** Te deconecteaza cand treci de Y-ul ales (sub sau peste) si te reconecteaza dupa cateva secunde pe acelasi server. */
public class AutoRelog extends Module {
    private final ModeSetting trigger;
    private final NumberSetting yLevel, delay, attempts;
    private final Setting<Boolean> overworldOnly, once, notify;
    private ServerInfo server;
    private int phase, tries;
    private long at;
    private boolean used;

    public AutoRelog() {
        super("Auto Relog", Category.DONUT);
        header("Trigger");
        trigger = mode("Trigger", "Below Y", "Below Y", "Above Y");
        yLevel = num("Y Level", 0, -64, 320, 1);
        overworldOnly = bool("Overworld Only", true);
        once = bool("Once Per Session", true);
        header("Reconnect");
        delay = num("Reconnect Delay (s)", 3, 1, 60, 1);
        attempts = num("Retry Attempts", 3, 1, 10, 1);
        notify = bool("Notify Chat", true);
    }

    @Override protected void onEnable() { used = false; phase = 0; }
    @Override protected void onDisable() { phase = 0; }

    @Override public void onTick() {
        if (phase != 0 || mc.player == null || mc.world == null || mc.getCurrentServerEntry() == null) return;
        if (once.value && used) return;
        if (overworldOnly.value && !mc.world.getRegistryKey().getValue().toString().equals("minecraft:overworld")) return;
        double y = mc.player.getY();
        boolean hit = trigger.is("Below Y") ? y < yLevel.get() : y > yLevel.get();
        if (!hit) return;
        server = mc.getCurrentServerEntry();
        used = true;
        tries = 0;
        phase = 1;
        at = System.currentTimeMillis() + (long) (delay.get() * 1000);
        if (notify.value) mc.player.sendMessage(Text.literal("[Auto Relog] Y " + (int) y + " -> relog in " + delay.asInt() + "s"), false);
        mc.getNetworkHandler().getConnection().disconnect(Text.literal("Auto Relog"));
    }

    @Override public void onGlobalTick() {
        if (phase == 0 || server == null) return;
        long now = System.currentTimeMillis();
        if (phase == 2 && mc.world != null && mc.player != null) { phase = 0; return; }
        if (now < at || mc.world != null) return;
        if (phase == 2 && tries >= attempts.asInt()) { phase = 0; return; }
        tries++;
        phase = 2;
        at = now + 20000;
        ConnectScreen.connect(new TitleScreen(), mc, ServerAddress.parse(server.address), server, false, null);
    }
}
