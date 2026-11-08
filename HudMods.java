package dev.myclient.module.modules;

import dev.myclient.config.Theme;
import dev.myclient.gui.RenderUtil;
import dev.myclient.module.Category;
import dev.myclient.module.HudModule;
import dev.myclient.module.NumberSetting;
import dev.myclient.module.Setting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

public final class HudMods {
    private HudMods() {}

    public static class TargetHud extends HudModule {
        private final NumberSetting range, bg;
        private final Setting<Boolean> nearest, armor, dist, bar;

        public TargetHud() {
            super("Target HUD", Category.HUD, 58, 58);
            header("Target");
            range = num("Range", 8, 2, 30, 1);
            nearest = bool("Use Nearest Player", true);
            header("Display");
            armor = bool("Show Armor", true);
            dist = bool("Show Distance", true);
            bar = bool("Health Bar", true);
            bg = num("Background Alpha %", 80, 10, 100, 5);
        }

        @Override protected int[] draw(DrawContext c, int x, int y, int sw, int sh) {
            LivingEntity t = null;
            Entity e = mc.targetedEntity;
            if (e instanceof LivingEntity le) t = le;
            if (t == null && nearest.value) {
                double best = range.get();
                for (PlayerEntity p : mc.world.getPlayers()) {
                    if (p == mc.player) continue;
                    double d = mc.player.distanceTo(p);
                    if (d < best) { best = d; t = p; }
                }
            }
            boolean sample = t == null;
            if (sample && mc.currentScreen == null) return null;
            int w = 130, h = 44;
            RenderUtil.roundedRect(c, x, y, w, h, Theme.radius, ((int) (bg.get() * 2.55) << 24) | 0x0C0C0E);
            String name = sample ? "Target" : t.getName().getString();
            double hp = sample ? 0.7 : Math.max(0, Math.min(1, t.getHealth() / t.getMaxHealth()));
            c.drawText(mc.textRenderer, mc.textRenderer.trimToWidth(name, w - 12), x + 6, y + 5, 0xFFFFFFFF, false);
            if (bar.value) {
                c.fill(x + 6, y + 17, x + w - 6, y + 23, 0x33FFFFFF);
                c.fill(x + 6, y + 17, x + 6 + (int) ((w - 12) * hp), y + 23, hp < 0.3 ? 0xFFEF4444 : 0xFF22C55E);
            }
            String info = sample ? "14hp" : Math.round(t.getHealth()) + "hp";
            if (armor.value) info += "  armor " + (sample ? 12 : t.getArmor());
            if (dist.value) info += "  " + (sample ? 4 : Math.round(mc.player.distanceTo(t))) + "m";
            c.drawText(mc.textRenderer, info, x + 6, y + 28, 0xFF9CA3AF, false);
            return new int[]{w, h};
        }
    }

    public static class ArmorHud extends HudModule {
        private final Setting<Boolean> percent;

        public ArmorHud() {
            super("Armor HUD", Category.HUD, 50, 84);
            header("Display");
            percent = bool("Show Percent", true);
        }

        @Override protected int[] draw(DrawContext c, int x, int y, int sw, int sh) {
            EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
            for (int i = 0; i < 4; i++) {
                ItemStack st = mc.player.getEquippedStack(slots[i]);
                if (st.isEmpty()) continue;
                c.drawItem(st, x + i * 20, y + 10);
                if (percent.value && st.getMaxDamage() > 0) {
                    int p = 100 - st.getDamage() * 100 / st.getMaxDamage();
                    c.drawText(mc.textRenderer, p + "%", x + i * 20, y, p < 25 ? 0xFFEF4444 : 0xFFFFFFFF, true);
                }
            }
            return new int[]{80, 28};
        }
    }

    public static class HudLogo extends HudModule {
        private final Setting<Boolean> fps, accentBar;

        public HudLogo() {
            super("HUD Logo", Category.HUD, 0, 0);
            header("Display");
            fps = bool("Show FPS", true);
            accentBar = bool("Accent Bar", true);
        }

        @Override protected int[] draw(DrawContext c, int x, int y, int sw, int sh) {
            String s = "MyClient" + (fps.value ? "  " + mc.getCurrentFps() + " fps" : "");
            int w = mc.textRenderer.getWidth(s) + 10;
            c.fill(x, y, x + w, y + 12, 0x90000000);
            if (accentBar.value) c.fill(x, y, x + 2, y + 12, Theme.accent);
            c.drawText(mc.textRenderer, s, x + 5, y + 2, 0xFFFFFFFF, true);
            return new int[]{w, 12};
        }
    }
}
