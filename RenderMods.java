package dev.myclient.module.modules;

import dev.myclient.config.FriendManager;
import dev.myclient.config.Theme;
import dev.myclient.config.WaypointManager;
import dev.myclient.module.Category;
import dev.myclient.module.Module;
import dev.myclient.module.ModeSetting;
import dev.myclient.module.NumberSetting;
import dev.myclient.module.Setting;
import dev.myclient.render.Proj;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public final class RenderMods {
    private RenderMods() {}

    private static final int GREEN = 0xFF22C55E, RED = 0xFFEF4444, WHITE = 0xFFFFFFFF;

    private static void text(DrawContext c, net.minecraft.client.MinecraftClient mc, String s, double cx, double y, int col) {
        c.drawText(mc.textRenderer, s, (int) (cx - mc.textRenderer.getWidth(s) / 2.0), (int) y, col, true);
    }

    public static class GlowEsp extends Module {
        private final NumberSetting range = num("Range", 96, 16, 256, 8);
        private final ModeSetting style = mode("Style", "Box", "Box", "Corners");
        private final NumberSetting fillA = num("Fill Alpha %", 18, 0, 70, 2);
        private final Setting<Boolean> names = bool("Names", true);
        private final Setting<Boolean> health = bool("Health", true);
        private final Setting<Boolean> distance = bool("Distance", true);
        private final Setting<Boolean> friends = bool("Highlight Friends", true);

        public GlowEsp() {
            super("Glow ESP", Category.RENDER);
            health.visible = () -> names.value;
            distance.visible = () -> names.value;
        }

        @Override public void onRender(DrawContext c, int sw, int sh) {
            for (PlayerEntity pl : mc.world.getPlayers()) {
                if (pl == mc.player) continue;
                double d = mc.player.distanceTo(pl);
                if (d > range.get()) continue;
                boolean fr = friends.value && FriendManager.isFriend(pl.getName().getString());
                int pc = fr ? GREEN : Theme.accent;
                if (fillA.get() > 0) Proj.fillBox(c, pl.getBoundingBox(), Proj.alpha(pc, (int) (fillA.get() * 2.55)));
                double[] r = Proj.box(c, pl.getBoundingBox(), pc, style.is("Corners"));
                if (r == null || !names.value) continue;
                String s = pl.getName().getString();
                if (health.value) s += " " + Math.round(pl.getHealth() + pl.getAbsorptionAmount()) + "hp";
                if (distance.value) s += " " + Math.round(d) + "m";
                text(c, mc, s, (r[0] + r[2]) / 2, r[1] - 11, fr ? GREEN : WHITE);
            }
        }
    }

    public static class Tracers extends Module {
        private final NumberSetting range = num("Range", 128, 16, 256, 8);
        private final Setting<Boolean> mobs = bool("Include Mobs", false);
        private final Setting<Boolean> friends = bool("Highlight Friends", true);

        public Tracers() { super("Tracers", Category.RENDER); }

        @Override public void onRender(DrawContext c, int sw, int sh) {
            for (Entity e : mc.world.getEntities()) {
                if (e == mc.player || !(e instanceof LivingEntity)) continue;
                if (!(e instanceof PlayerEntity) && !mobs.value) continue;
                if (mc.player.distanceTo(e) > range.get()) continue;
                double[] p = Proj.to(e.getX(), e.getY() + e.getHeight() / 2, e.getZ());
                if (p == null) continue;
                boolean fr = friends.value && e instanceof PlayerEntity pl && FriendManager.isFriend(pl.getName().getString());
                Proj.line2(c, sw / 2.0, sh, p[0], p[1], fr ? GREEN : Theme.accent);
            }
        }
    }

    public static class Nametags extends Module {
        private final NumberSetting range = num("Range", 64, 8, 256, 8);
        private final Setting<Boolean> hp = bool("Show Health", true);
        private final Setting<Boolean> armor = bool("Show Armor Points", true);
        private final Setting<Boolean> held = bool("Show Held Item", true);

        public Nametags() { super("Nametags", Category.RENDER); }

        @Override public void onRender(DrawContext c, int sw, int sh) {
            for (PlayerEntity pl : mc.world.getPlayers()) {
                if (pl == mc.player || mc.player.distanceTo(pl) > range.get()) continue;
                Box b = pl.getBoundingBox();
                double[] p = Proj.to((b.minX + b.maxX) / 2, b.maxY + 0.35, (b.minZ + b.maxZ) / 2);
                if (p == null) continue;
                String s = pl.getName().getString();
                if (hp.value) s += " " + Math.round(pl.getHealth()) + "hp";
                if (armor.value) s += " [" + pl.getArmor() + "]";
                int w = mc.textRenderer.getWidth(s);
                c.fill((int) p[0] - w / 2 - 2, (int) p[1] - 2, (int) p[0] + w / 2 + 2, (int) p[1] + 10, 0x90000000);
                text(c, mc, s, p[0], p[1], FriendManager.isFriend(pl.getName().getString()) ? GREEN : WHITE);
                ItemStack it = pl.getMainHandStack();
                if (held.value && !it.isEmpty()) text(c, mc, it.getName().getString(), p[0], p[1] - 11, 0xFFBBBBBB);
            }
        }
    }

    public static class LowHealthWarning extends Module {
        private final NumberSetting hearts = num("Hearts Threshold", 6, 1, 19, 1);
        private final NumberSetting intensity = num("Intensity", 60, 10, 100, 5);
        private final Setting<Boolean> pulse = bool("Pulse", true);

        public LowHealthWarning() { super("Low Health Warning", Category.RENDER); }

        @Override public void onRender(DrawContext c, int sw, int sh) {
            var p = mc.player;
            if (p.getHealth() > hearts.get() * 2 || p.getHealth() <= 0) return;
            double k = pulse.value ? 0.6 + 0.4 * Math.sin(System.currentTimeMillis() / 150.0) : 1.0;
            int layers = 14;
            for (int i = 0; i < layers; i++) {
                int a = (int) (intensity.get() * 2.55 * k * (layers - i) / layers * 0.5);
                int col = Proj.alpha(RED, Math.max(0, Math.min(255, a)));
                c.fill(0, i * 3, sw, i * 3 + 3, col);
                c.fill(0, sh - i * 3 - 3, sw, sh - i * 3, col);
                c.fill(i * 3, 0, i * 3 + 3, sh, col);
                c.fill(sw - i * 3 - 3, 0, sw - i * 3, sh, col);
            }
        }
    }

    public static class Trajectories extends Module {
        private final NumberSetting steps = num("Steps", 100, 20, 300, 10);
        private final Setting<Boolean> impact = bool("Impact Marker", true);
        private final Setting<Boolean> pearls = bool("Throwables", true);

        public Trajectories() { super("Trajectories", Category.RENDER); }

        @Override public void onRender(DrawContext c, int sw, int sh) {
            var p = mc.player;
            ItemStack st = p.getMainHandStack();
            double speed, grav;
            if (st.isOf(Items.BOW)) { speed = 3.0; grav = 0.05; }
            else if (st.isOf(Items.CROSSBOW)) { speed = 3.15; grav = 0.05; }
            else if (st.isOf(Items.TRIDENT)) { speed = 2.5; grav = 0.05; }
            else if (pearls.value && (st.isOf(Items.ENDER_PEARL) || st.isOf(Items.SNOWBALL) || st.isOf(Items.EGG))) { speed = 1.5; grav = 0.03; }
            else return;

            Vec3d pos = p.getEyePos().subtract(0, 0.1, 0);
            Vec3d vel = p.getRotationVec(1.0f).multiply(speed);
            Vec3d hit = null;
            for (int i = 0; i < steps.asInt(); i++) {
                Vec3d next = pos.add(vel);
                if (!mc.world.getBlockState(BlockPos.ofFloored(next)).isAir()) { hit = next; Proj.line3(c, pos, next, Theme.accent); break; }
                Proj.line3(c, pos, next, Theme.accent);
                pos = next;
                vel = vel.multiply(0.99).subtract(0, grav, 0);
            }
            if (impact.value && hit != null) {
                Proj.box(c, new Box(hit.x - 0.25, hit.y - 0.25, hit.z - 0.25, hit.x + 0.25, hit.y + 0.25, hit.z + 0.25), RED, false);
                double[] s = Proj.to(hit.x, hit.y + 0.5, hit.z);
                if (s != null) text(c, mc, Math.round(p.getEyePos().distanceTo(hit)) + "m", s[0], s[1] - 10, WHITE);
            }
        }
    }

    public static class Waypoints extends Module {
        private final Setting<Boolean> addHere = bool("Add Here (click)", false);
        private final Setting<Boolean> clear = bool("Clear All (click)", false);
        private final Setting<Boolean> deaths = bool("Save Death Points", true);
        private final Setting<Boolean> showDist = bool("Show Distance", true);
        private final NumberSetting maxDist = num("Max Distance (0 = any)", 0, 0, 5000, 100);
        private boolean dead;

        public Waypoints() { super("Waypoints", Category.RENDER); }

        private String dim() { return mc.world.getRegistryKey().getValue().toString(); }

        @Override public void onTick() {
            var p = mc.player;
            if (p == null || mc.world == null) return;
            if (addHere.value) {
                addHere.value = false;
                WaypointManager.list.add(new WaypointManager.Waypoint("WP" + (WaypointManager.list.size() + 1), p.getX(), p.getY(), p.getZ(), dim()));
            }
            if (clear.value) { clear.value = false; WaypointManager.list.clear(); }
            if (deaths.value) {
                if (p.getHealth() <= 0 && !dead) {
                    dead = true;
                    WaypointManager.list.add(new WaypointManager.Waypoint("Death", p.getX(), p.getY(), p.getZ(), dim()));
                } else if (p.getHealth() > 0) dead = false;
            }
        }

        @Override public void onRender(DrawContext c, int sw, int sh) {
            for (WaypointManager.Waypoint w : WaypointManager.list) {
                if (!w.dim().equals(dim())) continue;
                double d = mc.player.getEyePos().distanceTo(new Vec3d(w.x(), w.y(), w.z()));
                if (maxDist.get() > 0 && d > maxDist.get()) continue;
                double[] p = Proj.to(w.x(), w.y() + 1, w.z());
                if (p == null) continue;
                c.fill((int) p[0] - 2, (int) p[1] - 2, (int) p[0] + 3, (int) p[1] + 3, Theme.accent);
                text(c, mc, showDist.value ? w.name() + " " + Math.round(d) + "m" : w.name(), p[0], p[1] - 12, WHITE);
            }
        }
    }
}
