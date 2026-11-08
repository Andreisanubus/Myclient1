package dev.myclient.util;

public final class Colors {
    private Colors() {}

    public static final String[] NAMES = {"Green", "Orange", "Yellow", "Red", "Blue", "Purple", "Cyan", "Pink", "White"};
    private static final int[] VALUES = {0xFF22C55E, 0xFFF97316, 0xFFFACC15, 0xFFEF4444, 0xFF3B82F6, 0xFFA855F7, 0xFF22D3EE, 0xFFEC4899, 0xFFFFFFFF};

    public static int of(String name) {
        for (int i = 0; i < NAMES.length; i++) if (NAMES[i].equals(name)) return VALUES[i];
        return 0xFFFFFFFF;
    }

    public static int hsv(float h, float s, float v) {
        return 0xFF000000 | java.awt.Color.HSBtoRGB(h, s, v) & 0xFFFFFF;
    }

    public static int mix(int a, int b, double t) {
        t = Math.max(0, Math.min(1, t));
        int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }
}
