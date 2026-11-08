package dev.myclient.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import java.util.ArrayList;
import java.util.List;

/**
 * Proiectie lume -> ecran facuta manual (fara API de randare 3D), desenata pe HUD.
 * Fiindca se deseneaza pe HUD, tot ce e proiectat se vede automat prin pereti si prin pamant.
 */
public final class Proj {
    private static double cx, cy, cz, fx, fy, fz, rx, rz, ux, uy, uz, scale;
    public static int sw, sh;
    private static int budget;

    private Proj() {}

    public static void begin(MinecraftClient mc, int w, int h) {
        Vec3d e = mc.player.getEyePos();
        cx = e.x; cy = e.y; cz = e.z;
        double yaw = Math.toRadians(mc.player.getYaw()), pit = Math.toRadians(mc.player.getPitch());
        fx = -Math.sin(yaw) * Math.cos(pit); fy = -Math.sin(pit); fz = Math.cos(yaw) * Math.cos(pit);
        rx = -Math.cos(yaw); rz = -Math.sin(yaw);
        ux = -rz * fy; uy = rz * fx - rx * fz; uz = rx * fy;
        sw = w; sh = h;
        scale = (h / 2.0) / Math.tan(Math.toRadians(mc.options.getFov().getValue()) / 2.0);
        budget = 9000; // limita de dreptunghiuri umplute pe cadru
    }

    public static int alpha(int col, int a) { return (Math.max(0, Math.min(255, a)) << 24) | (col & 0xFFFFFF); }

    public static double depth(Vec3d v) { return (v.x - cx) * fx + (v.y - cy) * fy + (v.z - cz) * fz; }

    /** {x, y, adancime} sau null daca e in spatele camerei. */
    public static double[] to(double x, double y, double z) {
        double dx = x - cx, dy = y - cy, dz = z - cz;
        double zc = dx * fx + dy * fy + dz * fz;
        if (zc < 0.05) return null;
        double xc = dx * rx + dz * rz;
        double yc = dx * ux + dy * uy + dz * uz;
        return new double[]{sw / 2.0 + xc / zc * scale, sh / 2.0 - yc / zc * scale, zc};
    }

    /** Dreptunghiul 2D care cuprinde cutia 3D, {x0,y0,x1,y1} sau null. */
    public static double[] boxRect(Box b) {
        double x0 = 1e9, y0 = 1e9, x1 = -1e9, y1 = -1e9;
        for (int i = 0; i < 8; i++) {
            double[] p = to((i & 1) == 0 ? b.minX : b.maxX, (i & 2) == 0 ? b.minY : b.maxY, (i & 4) == 0 ? b.minZ : b.maxZ);
            if (p == null) return null;
            x0 = Math.min(x0, p[0]); y0 = Math.min(y0, p[1]); x1 = Math.max(x1, p[0]); y1 = Math.max(y1, p[1]);
        }
        if (x1 - x0 > sw * 4 || y1 - y0 > sh * 4) return null;
        return new double[]{x0, y0, x1, y1};
    }

    public static void outline(DrawContext c, double x0, double y0, double x1, double y1, int col, boolean corners) {
        int a = (int) x0, b = (int) y0, d = (int) x1, e = (int) y1;
        if (corners) {
            int l = Math.max(2, Math.min(8, (d - a) / 3)), m = Math.max(2, Math.min(8, (e - b) / 3));
            c.fill(a, b, a + l, b + 1, col); c.fill(a, b, a + 1, b + m, col);
            c.fill(d - l, b, d, b + 1, col); c.fill(d - 1, b, d, b + m, col);
            c.fill(a, e - 1, a + l, e, col); c.fill(a, e - m, a + 1, e, col);
            c.fill(d - l, e - 1, d, e, col); c.fill(d - 1, e - m, d, e, col);
        } else {
            c.fill(a, b, d, b + 1, col); c.fill(a, e - 1, d, e, col);
            c.fill(a, b, a + 1, e, col); c.fill(d - 1, b, d, e, col);
        }
    }

    /** Cutie ESP (contur 2D). Returneaza dreptunghiul sau null. */
    public static double[] box(DrawContext c, Box b, int col, boolean corners) {
        double[] r = boxRect(b);
        if (r == null) return null;
        outline(c, r[0], r[1], r[2], r[3], col, corners);
        return r;
    }

    // ------------------------------------------------------------ umplere (culoare transparenta peste bloc)
    private static void fillConvex(DrawContext c, double[] xs, double[] ys, int n, int col) {
        double minY = 1e9, maxY = -1e9;
        for (int i = 0; i < n; i++) { minY = Math.min(minY, ys[i]); maxY = Math.max(maxY, ys[i]); }
        int y0 = (int) Math.max(0, Math.floor(minY)), y1 = (int) Math.min(sh, Math.ceil(maxY));
        if (y1 <= y0) return;
        int step = (y1 - y0) > 220 ? 3 : (y1 - y0) > 90 ? 2 : 1;
        for (int y = y0; y < y1; y += step) {
            double yc = y + step * 0.5, lo = 1e9, hi = -1e9;
            for (int i = 0; i < n; i++) {
                int j = (i + 1) % n;
                double ya = ys[i], yb = ys[j];
                if ((ya <= yc && yb > yc) || (yb <= yc && ya > yc)) {
                    double x = xs[i] + (yc - ya) / (yb - ya) * (xs[j] - xs[i]);
                    lo = Math.min(lo, x); hi = Math.max(hi, x);
                }
            }
            if (hi > lo) {
                int a = (int) Math.max(0, lo), b = (int) Math.min(sw, hi);
                if (b > a) { c.fill(a, y, b, Math.min(y + step, y1), col); if (--budget <= 0) return; }
            }
        }
    }

    /** Invelisul convex al punctelor proiectate, umplut cu culoare. */
    private static void fillHull(DrawContext c, double[][] pts, int col) {
        double[][] s = pts.clone();
        java.util.Arrays.sort(s, (p, q) -> p[0] != q[0] ? Double.compare(p[0], q[0]) : Double.compare(p[1], q[1]));
        double[][] h = new double[s.length * 2][];
        int k = 0;
        for (double[] p : s) {
            while (k >= 2 && cross(h[k - 2], h[k - 1], p) <= 0) k--;
            h[k++] = p;
        }
        for (int i = s.length - 2, t = k + 1; i >= 0; i--) {
            while (k >= t && cross(h[k - 2], h[k - 1], s[i]) <= 0) k--;
            h[k++] = s[i];
        }
        int n = k - 1;
        if (n < 3) return;
        double[] xs = new double[n], ys = new double[n];
        for (int i = 0; i < n; i++) { xs[i] = h[i][0]; ys[i] = h[i][1]; }
        fillConvex(c, xs, ys, n, col);
    }

    private static double cross(double[] o, double[] a, double[] b) {
        return (a[0] - o[0]) * (b[1] - o[1]) - (a[1] - o[1]) * (b[0] - o[0]);
    }

    /** Coloreaza blocul (sau cutia) cu o culoare transparenta, prin pereti. */
    public static boolean fillBox(DrawContext c, Box b, int argb) {
        if (budget <= 0) return false;
        double[][] p = new double[8][];
        for (int i = 0; i < 8; i++) {
            p[i] = to((i & 1) == 0 ? b.minX : b.maxX, (i & 2) == 0 ? b.minY : b.maxY, (i & 4) == 0 ? b.minZ : b.maxZ);
            if (p[i] == null) return false;
        }
        fillHull(c, p, argb);
        return true;
    }

    /** Poligon convex in lume (de ex. placa de chunk), cu taiere la planul din fata camerei. */
    public static void fillPoly(DrawContext c, Vec3d[] pts, int argb) {
        if (budget <= 0) return;
        double near = 0.1;
        List<Vec3d> out = new ArrayList<>();
        for (int i = 0; i < pts.length; i++) {
            Vec3d a = pts[i], b = pts[(i + 1) % pts.length];
            double da = depth(a) - near, db = depth(b) - near;
            if (da >= 0) out.add(a);
            if ((da >= 0) != (db >= 0)) out.add(a.add(b.subtract(a).multiply(da / (da - db))));
        }
        if (out.size() < 3) return;
        double[] xs = new double[out.size()], ys = new double[out.size()];
        for (int i = 0; i < out.size(); i++) {
            Vec3d v = out.get(i);
            double[] p = to(v.x, v.y, v.z);
            if (p == null) return;
            xs[i] = p[0]; ys[i] = p[1];
        }
        fillConvex(c, xs, ys, out.size(), argb);
    }

    public static void line2(DrawContext c, double x0, double y0, double x1, double y1, int col) {
        double dx = x1 - x0, dy = y1 - y0;
        int n = (int) Math.min(1500, Math.max(Math.abs(dx), Math.abs(dy)));
        if (n == 0) { c.fill((int) x0, (int) y0, (int) x0 + 1, (int) y0 + 1, col); return; }
        for (int i = 0; i <= n; i++) {
            int x = (int) (x0 + dx * i / n), y = (int) (y0 + dy * i / n);
            if (x < -20 || y < -20 || x > sw + 20 || y > sh + 20) continue;
            c.fill(x, y, x + 1, y + 1, col);
        }
    }

    /** Linie 3D cu taiere la planul din fata camerei. */
    public static void line3(DrawContext c, Vec3d a, Vec3d b, int col) {
        double za = depth(a), zb = depth(b), n = 0.1;
        if (za < n && zb < n) return;
        Vec3d p = a, q = b;
        if (za < n) p = a.add(b.subtract(a).multiply((n - za) / (zb - za)));
        else if (zb < n) q = a.add(b.subtract(a).multiply((n - za) / (zb - za)));
        double[] s = to(p.x, p.y, p.z), e = to(q.x, q.y, q.z);
        if (s == null || e == null) return;
        line2(c, s[0], s[1], e[0], e[1], col);
    }
}
