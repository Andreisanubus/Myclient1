package dev.myclient.config;

public class Theme {
    public static final int[] PRESETS = {0xFF3B82F6, 0xFFEF4444, 0xFFA855F7, 0xFF22C55E, 0xFFF59E0B, 0xFFEC4899};
    public static int accent = PRESETS[0];
    public static int opacity = 92;
    public static int radius = 6;

    public static int panel() { return ((opacity * 255 / 100) << 24) | 0x0C0C0E; }
    public static void reset() { accent = PRESETS[0]; opacity = 92; radius = 6; }
}
