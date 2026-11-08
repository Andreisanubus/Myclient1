package dev.myclient.module.modules;

import dev.myclient.module.Category;
import dev.myclient.module.Module;
import dev.myclient.module.NumberSetting;
import dev.myclient.module.Setting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.AbstractBoatEntity;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;

/**
 * Boat Fly si Boat No Clip. Anti-cheat-urile verifica viteza si plutirea vehiculului; setarile de mai jos (viteza mica,
 * coborare periodica, variatie aleatoare) reduc tiparele evidente, dar nu pot garanta ca un anumit server nu le detecteaza.
 */
public final class BoatMods {
    private BoatMods() {}

    private static AbstractBoatEntity boat(net.minecraft.client.MinecraftClient mc) {
        if (mc.player == null) return null;
        Entity v = mc.player.getVehicle();
        return v instanceof AbstractBoatEntity b ? b : null;
    }

    private static double[] dir(net.minecraft.client.MinecraftClient mc, double speed) {
        var o = mc.options;
        double f = (o.forwardKey.isPressed() ? 1 : 0) - (o.backKey.isPressed() ? 1 : 0);
        double s = (o.leftKey.isPressed() ? 1 : 0) - (o.rightKey.isPressed() ? 1 : 0);
        if (f == 0 && s == 0) return new double[]{0, 0};
        double len = Math.sqrt(f * f + s * s);
        f /= len; s /= len;
        double yaw = Math.toRadians(mc.player.getYaw());
        return new double[]{(-Math.sin(yaw) * f + Math.cos(yaw) * s) * speed, (Math.cos(yaw) * f + Math.sin(yaw) * s) * speed};
    }

    public static class BoatFly extends Module {
        private final NumberSetting speed, vSpeed, fallEvery, fallAmount, jitter;
        private final Setting<Boolean> glide, randomize;
        private int t;

        public BoatFly() {
            super("Boat Fly", Category.MOVEMENT);
            header("Speed");
            speed = num("Speed (blocks/tick)", 0.45, 0.1, 2.0, 0.05);
            vSpeed = num("Vertical Speed", 0.25, 0.05, 1.5, 0.05);
            header("Bypass");
            glide = bool("Glide Down Slowly", false);
            fallEvery = num("Fall Every (ticks)", 20, 5, 80, 1);
            fallAmount = num("Fall Amount", 0.04, 0.0, 0.3, 0.01);
            randomize = bool("Randomize", true);
            jitter = num("Randomize Amount", 0.03, 0.0, 0.2, 0.01);
            jitter.visible = () -> randomize.value;
        }

        @Override public void onTick() {
            AbstractBoatEntity b = boat(mc);
            if (b == null) return;
            double[] h = dir(mc, speed.get());
            double vy = mc.options.jumpKey.isPressed() ? vSpeed.get() : mc.options.sneakKey.isPressed() ? -vSpeed.get() : 0;
            if (vy == 0) {
                if (glide.value) vy = -0.03;
                else if (++t % fallEvery.asInt() == 0) vy = -fallAmount.get();
            }
            if (randomize.value) {
                double j = jitter.get();
                h[0] += (Math.random() - 0.5) * j; h[1] += (Math.random() - 0.5) * j;
                vy += (Math.random() - 0.5) * j * 0.5;
            }
            b.setVelocity(h[0], vy, h[1]);
        }
    }

    public static class BoatNoClip extends Module {
        private final NumberSetting speed, vSpeed;
        private final Setting<Boolean> lava, down;
        private AbstractBoatEntity last;

        public BoatNoClip() {
            super("Boat No Clip", Category.MOVEMENT);
            header("Speed");
            speed = num("Speed (blocks/s)", 12, 2, 320, 1);
            vSpeed = num("Vertical Speed (blocks/s)", 8, 1, 100, 1);
            header("Safety");
            lava = bool("Lava Protection", true);
            down = bool("Hold Position When Idle", true);
        }

        @Override protected void onDisable() { if (last != null) last.noClip = false; last = null; }

        @Override public void onTick() {
            AbstractBoatEntity b = boat(mc);
            if (b == null) { if (last != null) { last.noClip = false; last = null; } return; }
            last = b;
            b.noClip = true;
            double[] h = dir(mc, speed.get() / 20.0);
            double vy = mc.options.jumpKey.isPressed() ? vSpeed.get() / 20.0 : mc.options.sneakKey.isPressed() ? -vSpeed.get() / 20.0 : 0;
            if (lava.value && mc.world.getBlockState(BlockPos.ofFloored(b.getX(), b.getY(), b.getZ())).isOf(Blocks.LAVA)) vy = Math.max(vy, 0.4);
            if (vy == 0 && !down.value) vy = -0.04;
            b.setVelocity(h[0], vy, h[1]);
        }
    }
}
