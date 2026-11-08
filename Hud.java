package dev.myclient.hud;

import dev.myclient.config.Theme;
import dev.myclient.module.Module;
import dev.myclient.module.ModuleManager;
import dev.myclient.render.Proj;
import dev.myclient.util.Keys;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import java.util.*;

public class Hud {
    public static final Map<String, Boolean> toggles = new LinkedHashMap<>();
    static {
        toggles.put("Arraylist", true);
        toggles.put("Hotkeys", false);
        toggles.put("Coords", false);
    }

    public static void render(DrawContext c) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null || mc.options.hudHidden) return;
        TextRenderer f = mc.textRenderer;
        int sw = c.getScaledWindowWidth(), sh = c.getScaledWindowHeight();

        Proj.begin(mc, sw, sh);
        ModuleManager.get().render(c, sw, sh);

        if (toggles.get("Arraylist")) {
            List<Module> on = ModuleManager.get().all().stream().filter(Module::isEnabled)
                    .sorted(Comparator.comparingInt((Module m) -> -f.getWidth(m.name))).toList();
            int y = 4;
            for (Module m : on) {
                int w = f.getWidth(m.name);
                c.fill(sw - w - 8, y - 1, sw - 2, y + 10, 0x90000000);
                c.fill(sw - 2, y - 1, sw, y + 10, Theme.accent);
                c.drawText(f, m.name, sw - w - 5, y + 1, 0xFFFFFFFF, true);
                y += 11;
            }
        }
        if (toggles.get("Coords")) {
            c.drawText(f, String.format("XYZ: %.0f %.0f %.0f", mc.player.getX(), mc.player.getY(), mc.player.getZ()),
                    4, sh - 12, 0xFFFFFFFF, true);
        }
        if (toggles.get("Hotkeys")) {
            int y = 40;
            for (Module m : ModuleManager.get().all()) {
                if (m.key < 0) continue;
                c.drawText(f, m.name + " [" + Keys.name(m.key) + "]", 4, y, m.isEnabled() ? Theme.accent : 0xFF8A8A8A, true);
                y += 10;
            }
        }
    }
}
