package dev.myclient.module.modules;

import dev.myclient.config.Theme;
import dev.myclient.gui.RenderUtil;
import dev.myclient.module.Category;
import dev.myclient.module.HudModule;
import dev.myclient.module.NumberSetting;
import dev.myclient.module.Setting;
import net.minecraft.client.gui.DrawContext;
import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Se conecteaza singur la Spotify (doar Windows): citeste titlul ferestrei Spotify, care contine "Artist - Melodie" cand ruleaza.
 * Nu cere cont sau login. Butoanele Play/Next/Previous trimit tastele media. Durata melodiei nu e disponibila prin aceasta metoda.
 */
public class SpotifyHud extends HudModule {
    private static final boolean WIN = System.getProperty("os.name", "").toLowerCase().contains("win");
    private static volatile String title = "", artist = "";
    private static volatile boolean playing, running, stop;
    private static Thread reader;

    private final Setting<Boolean> showArtist, showTimer, equalizer, compact, playPause, next, prev;
    private final NumberSetting bg, marquee;
    private String lastKey = "";
    private long elapsed, lastMs;

    public SpotifyHud() {
        super("Spotify HUD", Category.HUD, 1, 96);
        header("Display");
        showArtist = bool("Show Artist", true);
        showTimer = bool("Show Timer", true);
        equalizer = bool("Equalizer", true);
        compact = bool("Compact", false);
        bg = num("Background Alpha %", 75, 10, 100, 5);
        marquee = num("Scroll Speed", 25, 5, 80, 5);
        header("Controls (click to run)");
        playPause = bool("Play / Pause", false);
        next = bool("Next Track", false);
        prev = bool("Previous Track", false);
    }

    @Override protected void onEnable() { startReader(); }
    @Override protected void onDisable() { stop = true; }

    private static void startReader() {
        if (!WIN || (reader != null && reader.isAlive())) { stop = false; return; }
        stop = false;
        reader = new Thread(() -> {
            while (!stop) {
                poll();
                try { Thread.sleep(2000); } catch (InterruptedException e) { return; }
            }
        }, "myclient-spotify");
        reader.setDaemon(true);
        reader.start();
    }

    private static void poll() {
        try {
            Process p = new ProcessBuilder("powershell", "-NoProfile", "-NonInteractive", "-Command",
                    "(Get-Process -Name Spotify -ErrorAction SilentlyContinue | Where-Object { $_.MainWindowTitle } | Select-Object -First 1).MainWindowTitle")
                    .redirectErrorStream(true).start();
            String line;
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) { line = r.readLine(); }
            p.destroy();
            parse(line == null ? "" : line.trim());
        } catch (Exception ignored) {}
    }

    private static void parse(String t) {
        if (t.isEmpty()) { running = false; playing = false; return; }
        running = true;
        int i = t.indexOf(" - ");
        if (i < 0 && t.toLowerCase().startsWith("spotify")) { playing = false; return; }
        playing = true;
        if (i > 0) { artist = t.substring(0, i); title = t.substring(i + 3); } else { artist = ""; title = t; }
    }

    private static void sendKey(int code) {
        if (!WIN) return;
        Thread th = new Thread(() -> {
            try {
                new ProcessBuilder("powershell", "-NoProfile", "-NonInteractive", "-Command",
                        "(New-Object -ComObject WScript.Shell).SendKeys([char]" + code + ")").start();
            } catch (Exception ignored) {}
        }, "myclient-media");
        th.setDaemon(true);
        th.start();
    }

    @Override public void onTick() {
        if (playPause.value) { playPause.value = false; sendKey(179); }
        if (next.value) { next.value = false; sendKey(176); }
        if (prev.value) { prev.value = false; sendKey(177); }
    }

    private static String fmt(long ms) { long s = ms / 1000; return (s / 60) + ":" + String.format("%02d", s % 60); }

    @Override protected int[] draw(DrawContext c, int x, int y, int sw, int sh) {
        long now = System.currentTimeMillis();
        int w = compact.value ? 150 : 200, h = compact.value ? 30 : 44;
        int bgc = ((int) (bg.get() * 2.55) << 24) | 0x0C0C0E;
        RenderUtil.roundedRect(c, x, y, w, h, Theme.radius, bgc);

        if (!WIN) {
            c.drawText(mc.textRenderer, "Spotify HUD: doar Windows", x + 8, y + h / 2 - 4, 0xFF9CA3AF, false);
            return new int[]{w, h};
        }
        String key = artist + "|" + title;
        if (!key.equals(lastKey)) { lastKey = key; elapsed = 0; }
        if (playing && lastMs != 0) elapsed += now - lastMs;
        lastMs = now;

        int cs = h - 12, cx = x + 6, cy = y + 6;
        RenderUtil.roundedRect(c, cx, cy, cs, cs, 4, Theme.accent);
        RenderUtil.roundedRect(c, cx + 2, cy + 2, cs - 4, cs - 4, 3, 0x66000000);
        c.drawText(mc.textRenderer, "\u266A", cx + cs / 2 - 2, cy + cs / 2 - 4, 0xFFFFFFFF, false);

        int tx = cx + cs + 8, tw = w - (tx - x) - (equalizer.value ? 26 : 8);
        boolean on = running && !title.isEmpty();
        c.drawText(mc.textRenderer, !running ? "SPOTIFY" : playing ? "NOW PLAYING" : "PAUSED", tx, y + 5, 0xFF9CA3AF, false);

        String t = on ? title : "Spotify nu ruleaza";
        int textW = mc.textRenderer.getWidth(t);
        c.enableScissor(tx, y + 14, tx + tw, y + 25);
        if (textW <= tw) c.drawText(mc.textRenderer, t, tx, y + 15, 0xFFFFFFFF, false);
        else {
            int gap = 30, off = (int) ((now / 1000.0 * marquee.get()) % (textW + gap));
            c.drawText(mc.textRenderer, t, tx - off, y + 15, 0xFFFFFFFF, false);
            c.drawText(mc.textRenderer, t, tx - off + textW + gap, y + 15, 0xFFFFFFFF, false);
        }
        c.disableScissor();

        if (!compact.value) {
            String sub = on && showArtist.value ? artist : "";
            if (on && showTimer.value) sub += (sub.isEmpty() ? "" : "  \u2022  ") + fmt(elapsed);
            c.drawText(mc.textRenderer, mc.textRenderer.trimToWidth(sub, tw), tx, y + 28, 0xFF9CA3AF, false);
        }

        if (equalizer.value) {
            for (int i = 0; i < 4; i++) {
                int bh = playing && on ? 4 + (int) (Math.abs(Math.sin(now / 160.0 + i * 1.7)) * (h / 2 - 6)) : 3;
                c.fill(x + w - 22 + i * 4, y + h - 8 - bh, x + w - 20 + i * 4, y + h - 8, playing && on ? Theme.accent : 0xFF4B5563);
            }
        }
        return new int[]{w, h};
    }
}
