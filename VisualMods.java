package dev.myclient.module.modules;

import dev.myclient.module.Category;
import dev.myclient.module.Module;
import dev.myclient.module.ModeSetting;
import dev.myclient.module.NumberSetting;
import dev.myclient.module.Setting;
import dev.myclient.render.Proj;
import dev.myclient.util.Colors;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import java.util.*;

public final class VisualMods {
    private VisualMods() {}

    private static void dot(DrawContext c, double[] p, int size, int argb) {
        int s = Math.max(1, size), x = (int) p[0] - s / 2, y = (int) p[1] - s / 2;
        c.fill(x, y, x + s, y + s, argb);
    }

    /** Orbe luminoase cu dara care se rotesc in jurul tau, in 8 stiluri. */
    public static class FloatingRelics extends Module {
        private final ModeSetting style;
        private final NumberSetting count, radius, height, speed, trail, size;
        private final Setting<Boolean> glow, firstPerson;
        private final List<ArrayDeque<Vec3d>> trails = new ArrayList<>();
        private int ticks;

        public FloatingRelics() {
            super("Floating Relics", Category.VISUALS);
            header("Style");
            style = mode("Style", "Comet", "Comet", "Fireball", "Rainbow", "Neon", "Electric", "Sakura", "Void", "Aurora");
            glow = bool("Glow", true);
            trail = num("Trail Length", 16, 4, 40, 1);
            size = num("Size", 3, 1, 8, 1);
            header("Orbit");
            count = num("Count", 5, 1, 12, 1);
            radius = num("Radius", 1.3, 0.6, 3.0, 0.1);
            height = num("Height", 0.3, -1.0, 1.5, 0.1);
            speed = num("Speed", 1.0, 0.2, 3.0, 0.1);
            firstPerson = bool("Show In First Person", true);
        }

        @Override protected void onDisable() { trails.clear(); }

        private Vec3d orb(int i, int n) {
            var p = mc.player;
            double a = ticks * 0.06 * speed.get() + i * Math.PI * 2 / n;
            double r = radius.get() + Math.sin(ticks * 0.03 + i) * 0.15;
            double y = p.getY() + p.getHeight() * 0.5 + height.get() + Math.sin(a * 1.4 + i) * 0.35;
            return new Vec3d(p.getX() + Math.cos(a) * r, y, p.getZ() + Math.sin(a) * r);
        }

        @Override public void onTick() {
            if (mc.player == null) return;
            ticks++;
            int n = count.asInt();
            while (trails.size() < n) trails.add(new ArrayDeque<>());
            while (trails.size() > n) trails.remove(trails.size() - 1);
            for (int i = 0; i < n; i++) {
                ArrayDeque<Vec3d> q = trails.get(i);
                q.addLast(orb(i, n));
                while (q.size() > trail.asInt()) q.removeFirst();
            }
        }

        private int colorFor(int i, double frac, double t) {
            switch (style.value) {
                case "Fireball": return Colors.mix(0xFFFACC15, 0xFFDC2626, 1 - frac);
                case "Rainbow": return Colors.hsv((float) ((t * 0.25 + i * 0.13 + (1 - frac) * 0.25) % 1.0), 0.85f, 1f);
                case "Neon": return Colors.mix(0xFF22D3EE, 0xFFEC4899, (Math.sin(t * 2 + i) + 1) / 2);
                case "Electric": return Colors.mix(0xFF60A5FA, 0xFFFFFFFF, Math.random() * (1 - frac) + 0.2);
                case "Sakura": return Colors.mix(0xFFFBCFE8, 0xFFEC4899, 1 - frac);
                case "Void": return Colors.mix(0xFF1E1B4B, 0xFFA855F7, frac);
                case "Aurora": return Colors.hsv((float) (0.33 + 0.35 * ((Math.sin(t + i + (1 - frac) * 2) + 1) / 2)), 0.7f, 1f);
                default: return Colors.mix(0xFF22D3EE, 0xFFFFFFFF, frac);
            }
        }

        @Override public void onRender(DrawContext c, int sw, int sh) {
            if (!firstPerson.value && mc.options.getPerspective().isFirstPerson()) return;
            double t = System.currentTimeMillis() / 1000.0;
            int n = trails.size();
            for (int i = 0; i < n; i++) {
                ArrayDeque<Vec3d> q = trails.get(i);
                int len = q.size(), j = 0;
                for (Vec3d v : q) {
                    double frac = len <= 1 ? 1 : (double) j / (len - 1);
                    j++;
                    double dy = style.is("Sakura") ? -(1 - frac) * 0.25 : 0;
                    double[] p = Proj.to(v.x, v.y + dy, v.z);
                    if (p == null) continue;
                    if (style.is("Electric")) { p[0] += (Math.random() - 0.5) * 3 * (1 - frac); p[1] += (Math.random() - 0.5) * 3 * (1 - frac); }
                    int px = (int) Math.max(1, Math.min(12, size.get() * 5 / p[2])), col = colorFor(i, frac, t);
                    boolean head = j == len;
                    if (head && glow.value) {
                        dot(c, p, px * 4, Proj.alpha(col, 22));
                        dot(c, p, px * 3, Proj.alpha(col, 38));
                        dot(c, p, px * 2, Proj.alpha(col, 70));
                    }
                    dot(c, p, head ? px + 1 : Math.max(1, (int) (px * (0.35 + 0.65 * frac))), Proj.alpha(head ? 0xFFFFFFFF : col, (int) (60 + 195 * frac)));
                }
            }
        }
    }

    /** Marcaj animat in jurul tintei: inele care se rotesc si un inel care urca si coboara, plus colturi. */
    public static class TargetEsp extends Module {
        private final ModeSetting style, colorSel;
        private final NumberSetting speed, points, fillA;
        private final Setting<Boolean> playersOnly, name;

        public TargetEsp() {
            super("Target ESP", Category.VISUALS);
            header("Look");
            style = mode("Style", "Rings", "Rings", "Box", "Both");
            colorSel = color("Color", "Red");
            speed = num("Animation Speed", 1.0, 0.2, 4.0, 0.1);
            points = num("Ring Points", 28, 12, 60, 2);
            fillA = num("Box Fill Alpha %", 18, 0, 60, 2);
            header("Target");
            playersOnly = bool("Players Only", false);
            name = bool("Show Name", true);
            points.visible = () -> !style.is("Box");
            fillA.visible = () -> !style.is("Rings");
        }

        @Override public void onRender(DrawContext c, int sw, int sh) {
            Entity t = mc.targetedEntity;
            if (!(t instanceof LivingEntity) || (playersOnly.value && !(t instanceof PlayerEntity))) return;
            int col = Colors.of(colorSel.value);
            Box b = t.getBoundingBox();
            double time = System.currentTimeMillis() / 1000.0 * speed.get();
            if (!style.is("Box")) {
                double rad = Math.max(b.getLengthX(), b.getLengthZ()) * 0.8 + 0.25, h = b.getLengthY();
                for (int ring = 0; ring < 2; ring++) {
                    double y = ring == 0 ? b.minY + 0.05 : b.minY + h * (0.5 + 0.5 * Math.sin(time * 1.6));
                    int n = points.asInt();
                    for (int j = 0; j < n; j++) {
                        for (int tail = 0; tail < 3; tail++) {
                            double ang = time * (ring == 0 ? 2.2 : -2.8) + j * Math.PI * 2 / n - tail * 0.06;
                            double[] p = Proj.to(t.getX() + Math.cos(ang) * rad, y, t.getZ() + Math.sin(ang) * rad);
                            if (p == null) continue;
                            int s = (int) Math.max(1, Math.min(4, 7 / p[2] + 1 - tail * 0.5));
                            dot(c, p, s, Proj.alpha(col, 230 - tail * 85));
                        }
                    }
                }
            }
            if (!style.is("Rings")) {
                Box e = b.expand(0.08);
                if (fillA.get() > 0) Proj.fillBox(c, e, Proj.alpha(col, (int) (fillA.get() * 2.55)));
                Proj.box(c, e, Proj.alpha(col, 230), true);
            }
            if (name.value) {
                double[] p = Proj.to(t.getX(), b.maxY + 0.3, t.getZ());
                if (p != null) {
                    String s = t.getName().getString();
                    c.drawText(mc.textRenderer, s, (int) p[0] - mc.textRenderer.getWidth(s) / 2, (int) p[1] - 10, col, true);
                }
            }
        }
    }
}
