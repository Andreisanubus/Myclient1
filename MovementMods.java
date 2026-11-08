package dev.myclient.module.modules;

import dev.myclient.module.Category;
import dev.myclient.module.Module;
import dev.myclient.module.NumberSetting;
import dev.myclient.module.Setting;
import net.minecraft.client.MinecraftClient;

public final class MovementMods {
    private MovementMods() {}

    static boolean moving(MinecraftClient mc) {
        var o = mc.options;
        return o.forwardKey.isPressed() || o.backKey.isPressed() || o.leftKey.isPressed() || o.rightKey.isPressed();
    }

    /** Vector orizontal {x, z} dupa tastele WASD si directia privirii. */
    static double[] dir(MinecraftClient mc, double speed) {
        var o = mc.options;
        double f = (o.forwardKey.isPressed() ? 1 : 0) - (o.backKey.isPressed() ? 1 : 0);
        double s = (o.leftKey.isPressed() ? 1 : 0) - (o.rightKey.isPressed() ? 1 : 0);
        if (f == 0 && s == 0) return new double[]{0, 0};
        double len = Math.sqrt(f * f + s * s);
        f /= len; s /= len;
        double yaw = Math.toRadians(mc.player.getYaw());
        return new double[]{(-Math.sin(yaw) * f + Math.cos(yaw) * s) * speed, (Math.cos(yaw) * f + Math.sin(yaw) * s) * speed};
    }

    public static class Fly extends Module {
        private final NumberSetting speed = num("Speed", 1.0, 0.1, 5.0, 0.1);
        private final NumberSetting vSpeed = num("Vertical Speed", 0.5, 0.1, 3.0, 0.1);
        private final Setting<Boolean> antiKick = bool("Anti Kick", true);
        private int t;

        public Fly() { super("Fly", Category.MOVEMENT); }

        @Override public void onTick() {
            var p = mc.player;
            if (p == null) return;
            double[] h = dir(mc, speed.get());
            double vy = mc.options.jumpKey.isPressed() ? vSpeed.get() : mc.options.sneakKey.isPressed() ? -vSpeed.get() : 0;
            if (antiKick.value && vy == 0 && ++t % 40 == 0) vy = -0.04;
            p.setVelocity(h[0], vy, h[1]);
        }
    }

    public static class Speed extends Module {
        private final NumberSetting speed = num("Speed", 0.35, 0.1, 1.5, 0.05);
        private final Setting<Boolean> onlyGround = bool("Only On Ground", false);

        public Speed() { super("Speed", Category.MOVEMENT); }

        @Override public void onTick() {
            var p = mc.player;
            if (p == null || !moving(mc)) return;
            if (onlyGround.value && !p.isOnGround()) return;
            double[] h = dir(mc, speed.get());
            p.setVelocity(h[0], p.getVelocity().y, h[1]);
        }
    }

    public static class AutoWalk extends Module {
        private final Setting<Boolean> stopSneak = bool("Stop When Sneaking", true);

        public AutoWalk() { super("Auto Walk", Category.MOVEMENT); }

        @Override public void onTick() {
            if (mc.player == null) return;
            mc.options.forwardKey.setPressed(!(stopSneak.value && mc.player.isSneaking()));
        }

        @Override protected void onDisable() { mc.options.forwardKey.setPressed(false); }
    }

    public static class AutoJump extends Module {
        private final Setting<Boolean> onlyMoving = bool("Only When Moving", true);
        private boolean held;

        public AutoJump() { super("Auto Jump", Category.MOVEMENT); }

        @Override public void onTick() {
            var p = mc.player;
            if (p == null) return;
            boolean go = p.isOnGround() && (!onlyMoving.value || moving(mc));
            if (go) { mc.options.jumpKey.setPressed(true); held = true; }
            else if (held) { mc.options.jumpKey.setPressed(false); held = false; }
        }

        @Override protected void onDisable() { if (held) mc.options.jumpKey.setPressed(false); held = false; }
    }
}
