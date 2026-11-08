package dev.myclient.module.modules;

import dev.myclient.module.Category;
import dev.myclient.module.Module;
import dev.myclient.module.ModeSetting;
import dev.myclient.module.NumberSetting;
import dev.myclient.module.Setting;
import dev.myclient.render.Proj;
import dev.myclient.util.Colors;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import java.util.*;

/**
 * Cauta dovezi de crestere naturala (kelp complet crescut, cuib de albine, vines, culturi coapte, geode de ametist).
 * Cand numarul de dovezi atinge Sensitivity, marcheaza chunk-ul cu o placa transparenta cu margini late si colturi taiate,
 * colorata dupa dovada gasita. Scaneaza o singura data fiecare chunk si pastreaza rezultatul (marcajele raman cand chunk-ul se descarca).
 * E o estimare facuta din datele primite de la server, nu o certitudine.
 */
public class SusFinder extends Module {
    public static final List<SusFinder> INSTANCES = new ArrayList<>();

    /** Folosit de modulul ESP ("Only In Sus Chunks"). */
    public static boolean isSus(int cx, int cz) {
        long k = ChunkPos.toLong(cx, cz);
        for (SusFinder f : INSTANCES) {
            if (!f.isEnabled()) continue;
            Info i = f.map.get(k);
            if (i != null && i.flagged) return true;
        }
        return false;
    }

    private static final String[] NAMES = {"Kelp", "Bee Nest", "Vines", "Mature Crops", "Amethyst Geode"};
    private static final String[] DEF_COL = {"Green", "Orange", "Yellow", "Red", "Purple"};
    private static final int[] DEF_MIN = {1, 1, 8, 4, 6};
    private static final int[] MAX_MIN = {16, 16, 64, 64, 60};

    private static class Info {
        int cx, cz, main = -1;
        final int[] cnt = new int[5];
        final List<Integer> ev = new ArrayList<>();
        boolean flagged, notified;
    }

    private record Toast(String text, int color, long until) {}

    private final boolean[] has = new boolean[5];
    private final List<Setting<Boolean>> on = new ArrayList<>(Collections.nCopies(5, null));
    private final List<ModeSetting> col = new ArrayList<>(Collections.nCopies(5, null));
    private final List<NumberSetting> min = new ArrayList<>(Collections.nCopies(5, null));
    private final NumberSetting sens, radius, perTick, minY, maxY, markerY, fillA, borderA, borderW, corner, maxMarkers;
    private final Setting<Boolean> surfaceOpt, label, notifyChat, notifyScreen;
    private final ModeSetting style;

    private final Map<Long, Info> map = new HashMap<>();
    private final Deque<Toast> toasts = new ArrayDeque<>();
    private List<int[]> offsets = new ArrayList<>();
    private int offRadius = -1, tick;

    private SusFinder(String name, boolean kelp, boolean bee, boolean vines, boolean crops, boolean amethyst) {
        super(name, Category.BASE_RAID);
        INSTANCES.add(this);
        boolean[] t = {kelp, bee, vines, crops, amethyst};
        int n = 0;
        header("Evidence");
        for (int i = 0; i < 5; i++) {
            has[i] = t[i];
            if (!t[i]) continue;
            n++;
            Setting<Boolean> s = bool(NAMES[i], true);
            on.set(i, s);
            ModeSetting cm = color(NAMES[i] + " Color", DEF_COL[i]);
            cm.visible = () -> s.value;
            col.set(i, cm);
            NumberSetting mn = num("Min " + NAMES[i], DEF_MIN[i], 1, MAX_MIN[i], 1);
            mn.visible = () -> s.value;
            min.set(i, mn);
        }
        header("Detection");
        sens = num("Sensitivity (evidences needed)", 1, 1, Math.max(2, n), 1);
        radius = num("Scan Radius (chunks)", 10, 2, 32, 1);
        perTick = num("Chunks Per Tick", 1, 1, 4, 1);
        minY = num("Min Y", -64, -64, 320, 1);
        maxY = num("Max Y", 200, -64, 320, 1);
        surfaceOpt = bool("Surface Optimization", true);
        header("Marker");
        markerY = num("Marker Y", -20, -64, 320, 1);
        style = mode("Style", "Soft Plate", "Soft Plate", "Outline Only");
        fillA = num("Fill Alpha %", 25, 5, 80, 5);
        borderA = num("Border Alpha %", 65, 10, 100, 5);
        borderW = num("Border Width", 2, 1, 4, 1);
        corner = num("Corner Cut", 3, 0, 6, 1);
        maxMarkers = num("Max Markers", 20, 1, 100, 1);
        label = bool("Show Label", true);
        header("Alerts");
        notifyChat = bool("Notify Chat", true);
        notifyScreen = bool("Notify On Screen", true);
        fillA.visible = () -> style.is("Soft Plate");
    }

    public static SusFinder betterSusChunk() { return new SusFinder("Better Sus Chunk", true, true, true, true, false); }
    public static SusFinder susChunkFinder() { return new SusFinder("Sus Chunk Finder", true, true, true, true, true); }
    public static SusFinder amethystBypass() { return new SusFinder("Amethyst Bypass", false, false, false, false, true); }

    @Override protected void onDisable() { map.clear(); toasts.clear(); }

    private int enabledTypes() {
        int n = 0;
        for (int i = 0; i < 5; i++) if (has[i] && on.get(i).value) n++;
        return n;
    }

    @Override public void onTick() {
        if (mc.world == null || mc.player == null) return;
        tick++;
        scanNew();
        if (tick % 10 == 0) for (Info in : map.values()) evaluate(in);
    }

    private void scanNew() {
        int r = Math.min(radius.asInt(), Math.max(2, mc.options.getViewDistance().getValue()));
        if (r != offRadius) {
            offRadius = r;
            offsets = new ArrayList<>();
            for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) offsets.add(new int[]{dx, dz});
            offsets.sort(Comparator.comparingInt(o -> o[0] * o[0] + o[1] * o[1]));
        }
        ChunkPos pc = mc.player.getChunkPos();
        int done = 0;
        for (int[] o : offsets) {
            int cx = pc.x + o[0], cz = pc.z + o[1];
            long key = ChunkPos.toLong(cx, cz);
            if (map.containsKey(key) || !mc.world.getChunkManager().isChunkLoaded(cx, cz)) continue;
            Info in = new Info();
            in.cx = cx; in.cz = cz;
            scan(in);
            evaluate(in);
            map.put(key, in);
            if (++done >= perTick.asInt()) break;
        }
    }

    private void scan(Info in) {
        WorldChunk ch = mc.world.getChunk(in.cx, in.cz);
        ChunkSection[] secs = ch.getSectionArray();
        int bottom = ch.getBottomY();
        boolean surf = has[0] || has[1] || has[2] || has[3], am = has[4];
        int lo = minY.asInt(), hi = maxY.asInt();
        int surfLo = surfaceOpt.value ? Math.max(lo, 30) : lo;      // kelp, cuiburi, vines, culturi sunt aproape de suprafata
        int amHi = surfaceOpt.value ? Math.min(hi, 40) : hi;        // geodele sunt in subteran
        for (int i = 0; i < secs.length; i++) {
            ChunkSection s = secs[i];
            int by = bottom + i * 16;
            boolean wantSurf = surf && by + 15 >= surfLo && by <= hi;
            boolean wantAm = am && by + 15 >= lo && by <= amHi;
            if ((!wantSurf && !wantAm) || s == null || s.isEmpty()) continue;
            for (int y = 0; y < 16; y++) {
                int wy = by + y;
                boolean doSurf = wantSurf && wy >= surfLo && wy <= hi, doAm = wantAm && wy >= lo && wy <= amHi;
                if (!doSurf && !doAm) continue;
                for (int z = 0; z < 16; z++)
                    for (int x = 0; x < 16; x++) {
                        BlockState st = s.getBlockState(x, y, z);
                        Block b = st.getBlock();
                        if (b == Blocks.AIR) continue;
                        if (doAm && b == Blocks.AMETHYST_CLUSTER) in.cnt[4]++;
                        else if (doSurf) {
                            if (b == Blocks.KELP) { if (st.get(Properties.AGE_25) == 25) in.cnt[0]++; }
                            else if (b == Blocks.BEE_NEST) in.cnt[1]++;
                            else if (b == Blocks.VINE) in.cnt[2]++;
                            else if (b instanceof CropBlock cb) { if (cb.isMature(st)) in.cnt[3]++; }
                        }
                    }
            }
        }
    }

    private void evaluate(Info in) {
        in.ev.clear();
        int present = 0, best = -1;
        double bestR = -1;
        for (int i = 0; i < 5; i++) {
            if (!has[i] || !on.get(i).value) continue;
            int mn = min.get(i).asInt();
            if (in.cnt[i] >= mn) {
                present++;
                in.ev.add(i);
                double r = (double) in.cnt[i] / mn;
                if (r > bestR) { bestR = r; best = i; }
            }
        }
        in.main = best;
        in.flagged = present > 0 && present >= Math.min(sens.asInt(), Math.max(1, enabledTypes()));
        if (in.flagged && !in.notified) {
            in.notified = true;
            StringBuilder sb = new StringBuilder();
            for (int i : in.ev) sb.append(sb.length() > 0 ? ", " : "").append(NAMES[i]).append(" x").append(in.cnt[i]);
            String pos = "X " + (in.cx * 16 + 8) + "  Z " + (in.cz * 16 + 8);
            if (notifyChat.value && mc.player != null) mc.player.sendMessage(Text.literal("[" + name + "] " + pos + "  (" + sb + ")"), false);
            if (notifyScreen.value) {
                toasts.addLast(new Toast(name + ": " + pos + "  " + sb, Colors.of(col.get(best).value), System.currentTimeMillis() + 6000));
                while (toasts.size() > 3) toasts.removeFirst();
            }
        }
    }

    // ------------------------------------------------------------ desenare
    private static Vec3d[] octagon(double x0, double z0, double y, double k) {
        double x1 = x0 + 16, z1 = z0 + 16;
        return new Vec3d[]{new Vec3d(x0 + k, y, z0), new Vec3d(x1 - k, y, z0), new Vec3d(x1, y, z0 + k), new Vec3d(x1, y, z1 - k),
                new Vec3d(x1 - k, y, z1), new Vec3d(x0 + k, y, z1), new Vec3d(x0, y, z1 - k), new Vec3d(x0, y, z0 + k)};
    }

    @Override public void onRender(DrawContext c, int sw, int sh) {
        long now = System.currentTimeMillis();
        toasts.removeIf(t -> t.until() < now);
        int ty = 24;
        for (Toast t : toasts) {
            int w = mc.textRenderer.getWidth(t.text()) + 14;
            c.fill(sw / 2 - w / 2, ty, sw / 2 + w / 2, ty + 14, 0xB0101014);
            c.fill(sw / 2 - w / 2, ty, sw / 2 - w / 2 + 3, ty + 14, t.color());
            c.drawText(mc.textRenderer, t.text(), sw / 2 - w / 2 + 8, ty + 3, 0xFFFFFFFF, false);
            ty += 16;
        }

        double maxD = (radius.get() + 4) * 16.0;
        double px = mc.player.getX(), pz = mc.player.getZ();
        List<Info> list = new ArrayList<>();
        for (Info in : map.values()) if (in.flagged && Math.hypot(px - (in.cx * 16 + 8), pz - (in.cz * 16 + 8)) <= maxD) list.add(in);
        list.sort(Comparator.comparingDouble(in -> Math.hypot(px - (in.cx * 16 + 8), pz - (in.cz * 16 + 8))));
        int shown = 0;
        for (Info in : list) {
            if (shown++ >= maxMarkers.asInt()) break;
            drawPlate(c, in);
        }
    }

    private void drawPlate(DrawContext c, Info in) {
        double x0 = in.cx * 16.0, z0 = in.cz * 16.0, y = markerY.get() + 1.0;
        int base = Colors.of(col.get(in.main).value);
        int bc = Proj.alpha(base, borderA.asInt() * 255 / 100);
        Vec3d[] outer = octagon(x0, z0, y, corner.get());
        double s = Math.max(0.1, (16 - 2 * borderW.get()) / 16.0);
        Vec3d ctr = new Vec3d(x0 + 8, y, z0 + 8);
        Vec3d[] inner = new Vec3d[8];
        for (int i = 0; i < 8; i++) inner[i] = ctr.add(outer[i].subtract(ctr).multiply(s));
        if (style.is("Soft Plate")) Proj.fillPoly(c, inner, Proj.alpha(base, fillA.asInt() * 255 / 100));
        for (int i = 0; i < 8; i++) {
            int j = (i + 1) % 8;
            Proj.fillPoly(c, new Vec3d[]{outer[i], outer[j], inner[j], inner[i]}, bc);
        }
        if (!label.value) return;
        double[] p = Proj.to(ctr.x, ctr.y, ctr.z);
        if (p == null) return;
        int dist = (int) Math.hypot(mc.player.getX() - ctr.x, mc.player.getZ() - ctr.z);
        StringBuilder all = new StringBuilder("SUS ");
        for (int i : in.ev) all.append(NAMES[i]).append(' ').append(in.cnt[i]).append("  ");
        all.append(dist).append('m');
        int w = mc.textRenderer.getWidth(all.toString());
        int x = (int) p[0] - w / 2, yy = (int) p[1] - 5;
        c.fill(x - 3, yy - 2, x + w + 3, yy + 10, 0xA0101014);
        c.drawText(mc.textRenderer, "SUS ", x, yy, 0xFFFFFFFF, false);
        x += mc.textRenderer.getWidth("SUS ");
        for (int i : in.ev) {
            String t = NAMES[i] + " " + in.cnt[i] + "  ";
            c.drawText(mc.textRenderer, t, x, yy, Colors.of(col.get(i).value), false);
            x += mc.textRenderer.getWidth(t);
        }
        c.drawText(mc.textRenderer, dist + "m", x, yy, 0xFF9CA3AF, false);
    }
}
