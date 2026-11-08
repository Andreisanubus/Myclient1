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
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import java.util.*;

/**
 * ESP pe blocuri alese: coloreaza blocul cu o culoare transparenta, prin pereti.
 * Scaneaza fiecare chunk o data (apoi la cateva secunde) intre Min Y si Max Y, cu limita pe tick.
 * ESP, Storage ESP si Xray folosesc aceeasi clasa cu liste diferite de blocuri.
 */
public class BlockSetEsp extends Module {
    private record Entry(String name, int color, Setting<Boolean> on) {}
    private record Hit(BlockPos pos, Entry e) {}

    private static final int RESCAN_TICKS = 400;

    private final NumberSetting range, minY, maxY, maxBoxes, perTick, fillA, outlineA;
    private final Setting<Boolean> onlySus, labels, distance, fade;
    private final ModeSetting renderMode, colorMode, single;
    private final Map<Block, Entry> map = new HashMap<>();
    private Entry shulker;
    private final Map<Long, List<Hit>> cache = new HashMap<>();
    private final Map<Long, Integer> stamp = new HashMap<>();
    private List<Hit> visible = new ArrayList<>();
    private int tick;

    private BlockSetEsp(String name, Object[][] defs) {
        super(name, Category.RENDER);
        header("Targets");
        for (Object[] d : defs) {
            Entry e = new Entry((String) d[1], (Integer) d[2], bool((String) d[1], (Boolean) d[3]));
            for (Block b : (Block[]) d[0]) map.put(b, e);
            if (e.name().equals("Shulker Box")) shulker = e;
        }
        header("Area");
        range = num("Range (blocks)", 48, 8, 128, 4);
        minY = num("Min Y", -64, -64, 320, 1);
        maxY = num("Max Y", 200, -64, 320, 1);
        maxBoxes = num("Max Blocks", 300, 20, 1000, 20);
        perTick = num("Chunks Per Tick", 2, 1, 8, 1);
        onlySus = bool("Only In Sus Chunks", false);
        header("Look");
        renderMode = mode("Render Mode", "Fill + Outline", "Fill", "Fill + Outline", "Outline");
        fillA = num("Fill Alpha %", 40, 5, 90, 5);
        outlineA = num("Outline Alpha %", 90, 10, 100, 5);
        fade = bool("Fade With Distance", true);
        labels = bool("Labels", true);
        distance = bool("Distance", true);
        colorMode = mode("Color Mode", "Per Block", "Per Block", "Single");
        single = color("Single Color", "Cyan");
        fillA.visible = () -> !renderMode.is("Outline");
        outlineA.visible = () -> !renderMode.is("Fill");
        distance.visible = () -> labels.value;
        single.visible = () -> colorMode.is("Single");
    }

    private Entry find(Block b) {
        Entry e = map.get(b);
        if (e == null && shulker != null && b instanceof ShulkerBoxBlock) e = shulker;
        return e;
    }

    @Override protected void onDisable() { cache.clear(); stamp.clear(); visible = new ArrayList<>(); }

    @Override public void onTick() {
        if (mc.world == null || mc.player == null) return;
        tick++;
        ChunkPos pc = mc.player.getChunkPos();
        int rc = (int) Math.ceil(range.get() / 16.0);
        int budget = perTick.asInt();
        for (int pass = 0; pass < 2 && budget > 0; pass++) {
            for (int dx = -rc; dx <= rc && budget > 0; dx++)
                for (int dz = -rc; dz <= rc && budget > 0; dz++) {
                    int cx = pc.x + dx, cz = pc.z + dz;
                    long key = ChunkPos.toLong(cx, cz);
                    Integer st = stamp.get(key);
                    boolean need = pass == 0 ? st == null : (st != null && tick - st > RESCAN_TICKS);
                    if (!need || !mc.world.getChunkManager().isChunkLoaded(cx, cz)) continue;
                    scanChunk(cx, cz, key);
                    budget--;
                }
        }
        if (tick % 100 == 0) {
            stamp.keySet().removeIf(k -> Math.abs(ChunkPos.getPackedX(k) - pc.x) > rc + 3 || Math.abs(ChunkPos.getPackedZ(k) - pc.z) > rc + 3);
            cache.keySet().retainAll(stamp.keySet());
        }
        if (tick % 5 == 0) rebuildVisible();
    }

    private void scanChunk(int cx, int cz, long key) {
        WorldChunk ch = mc.world.getChunk(cx, cz);
        ChunkSection[] secs = ch.getSectionArray();
        int bottom = ch.getBottomY(), lo = minY.asInt(), hi = maxY.asInt();
        List<Hit> out = new ArrayList<>();
        for (int i = 0; i < secs.length; i++) {
            int by = bottom + i * 16;
            ChunkSection s = secs[i];
            if (by + 15 < lo || by > hi || s == null || s.isEmpty()) continue;
            for (int y = 0; y < 16; y++) {
                int wy = by + y;
                if (wy < lo || wy > hi) continue;
                for (int z = 0; z < 16; z++)
                    for (int x = 0; x < 16; x++) {
                        BlockState st = s.getBlockState(x, y, z);
                        Block b = st.getBlock();
                        if (b == Blocks.AIR) continue;
                        Entry e = find(b);
                        if (e != null) out.add(new Hit(new BlockPos(cx * 16 + x, wy, cz * 16 + z), e));
                    }
            }
        }
        cache.put(key, out);
        stamp.put(key, tick);
    }

    private void rebuildVisible() {
        double r2 = range.get() * range.get();
        var p = mc.player;
        List<Hit> all = new ArrayList<>();
        for (List<Hit> l : cache.values()) {
            for (Hit h : l) {
                if (!h.e().on().value) continue;
                if (h.pos().getY() < minY.asInt() || h.pos().getY() > maxY.asInt()) continue;
                double dx = h.pos().getX() + 0.5 - p.getX(), dy = h.pos().getY() + 0.5 - p.getY(), dz = h.pos().getZ() + 0.5 - p.getZ();
                if (dx * dx + dy * dy + dz * dz > r2) continue;
                if (onlySus.value && !SusFinder.isSus(h.pos().getX() >> 4, h.pos().getZ() >> 4)) continue;
                all.add(h);
            }
        }
        all.sort(Comparator.comparingDouble(h -> p.squaredDistanceTo(h.pos().getX() + 0.5, h.pos().getY() + 0.5, h.pos().getZ() + 0.5)));
        visible = all.size() > maxBoxes.asInt() ? new ArrayList<>(all.subList(0, maxBoxes.asInt())) : all;
    }

    @Override public void onRender(DrawContext c, int sw, int sh) {
        var p = mc.player;
        boolean fill = !renderMode.is("Outline"), outline = !renderMode.is("Fill");
        for (Hit h : visible) {
            double d = Math.sqrt(p.squaredDistanceTo(h.pos().getX() + 0.5, h.pos().getY() + 0.5, h.pos().getZ() + 0.5));
            double k = fade.value ? Math.max(0.35, 1.0 - d / range.get() * 0.65) : 1.0;
            int col = colorMode.is("Single") ? Colors.of(single.value) : h.e().color();
            Box box = new Box(h.pos());
            if (fill) Proj.fillBox(c, box, Proj.alpha(col, (int) (fillA.get() * 2.55 * k)));
            double[] r = outline ? Proj.box(c, box, Proj.alpha(col, (int) (outlineA.get() * 2.55 * k)), false) : Proj.boxRect(box);
            if (r == null || !labels.value || d > 40) continue;
            String s = h.e().name() + (distance.value ? " " + Math.round(d) + "m" : "");
            c.drawText(mc.textRenderer, s, (int) ((r[0] + r[2]) / 2 - mc.textRenderer.getWidth(s) / 2.0), (int) r[1] - 10, 0xFFFFFFFF, true);
        }
    }

    private static Object[] d(Block[] b, String n, int col, boolean on) { return new Object[]{b, n, col, on}; }
    private static Block[] b(Block... x) { return x; }

    public static BlockSetEsp esp() {
        return new BlockSetEsp("ESP", new Object[][]{
                d(b(Blocks.SPAWNER), "Spawner", 0xFF22D3EE, true),
                d(b(Blocks.CHEST, Blocks.TRAPPED_CHEST), "Chest", 0xFFF59E0B, true),
                d(b(Blocks.ENDER_CHEST), "Ender Chest", 0xFFA855F7, true),
                d(b(Blocks.BARREL), "Barrel", 0xFFB45309, true),
                d(b(Blocks.SHULKER_BOX), "Shulker Box", 0xFFEC4899, true),
                d(b(Blocks.HOPPER), "Hopper", 0xFF9CA3AF, false),
                d(b(Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE), "Diamond Ore", 0xFF38BDF8, false),
                d(b(Blocks.ANCIENT_DEBRIS), "Ancient Debris", 0xFFB45309, false),
                d(b(Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE), "Emerald Ore", 0xFF22C55E, false),
                d(b(Blocks.AMETHYST_CLUSTER), "Amethyst Cluster", 0xFFA855F7, false),
                d(b(Blocks.BUDDING_AMETHYST), "Budding Amethyst", 0xFFC084FC, false),
                d(b(Blocks.BEE_NEST), "Bee Nest", 0xFFF97316, false)});
    }

    public static BlockSetEsp storageEsp() {
        return new BlockSetEsp("Storage ESP", new Object[][]{
                d(b(Blocks.CHEST), "Chest", 0xFFF59E0B, true),
                d(b(Blocks.TRAPPED_CHEST), "Trapped Chest", 0xFFEF4444, true),
                d(b(Blocks.ENDER_CHEST), "Ender Chest", 0xFFA855F7, true),
                d(b(Blocks.BARREL), "Barrel", 0xFFB45309, true),
                d(b(Blocks.SHULKER_BOX), "Shulker Box", 0xFFEC4899, true),
                d(b(Blocks.HOPPER), "Hopper", 0xFF9CA3AF, false)});
    }

    /** Nu e xray real (acela schimba randarea blocurilor): coloreaza minereurile prin pereti. */
    public static BlockSetEsp xray() {
        return new BlockSetEsp("Xray", new Object[][]{
                d(b(Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE), "Coal", 0xFF6B7280, false),
                d(b(Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE), "Iron", 0xFFD4A27F, false),
                d(b(Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE), "Copper", 0xFFEA8A4C, false),
                d(b(Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE, Blocks.NETHER_GOLD_ORE), "Gold", 0xFFFACC15, true),
                d(b(Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE), "Redstone", 0xFFEF4444, false),
                d(b(Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE), "Lapis", 0xFF3B82F6, false),
                d(b(Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE), "Diamond", 0xFF38BDF8, true),
                d(b(Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE), "Emerald", 0xFF22C55E, true),
                d(b(Blocks.ANCIENT_DEBRIS), "Ancient Debris", 0xFFB45309, true),
                d(b(Blocks.NETHER_QUARTZ_ORE), "Quartz", 0xFFE5E7EB, false)});
    }
}
