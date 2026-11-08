package dev.myclient.module;

import net.minecraft.client.gui.DrawContext;

/** Modul cu un element de HUD care poate fi mutat cu mouse-ul din tabul HUD. */
public abstract class HudModule extends Module {
    public final NumberSetting posX, posY;
    public int lastX, lastY, w = 80, h = 24;

    protected HudModule(String name, Category cat, double defX, double defY) {
        super(name, cat);
        header("Position");
        posX = num("Position X %", defX, 0, 100, 0.5);
        posY = num("Position Y %", defY, 0, 100, 0.5);
    }

    /** Deseneaza la (x, y) si intoarce {latime, inaltime}. */
    protected abstract int[] draw(DrawContext c, int x, int y, int sw, int sh);

    @Override public final void onRender(DrawContext c, int sw, int sh) {
        lastX = (int) ((sw - w) * posX.get() / 100.0);
        lastY = (int) ((sh - h) * posY.get() / 100.0);
        int[] s = draw(c, lastX, lastY, sw, sh);
        if (s != null) { w = Math.max(1, s[0]); h = Math.max(1, s[1]); }
    }

    public void moveTo(int px, int py, int sw, int sh) {
        posX.set(100.0 * px / Math.max(1, sw - w));
        posY.set(100.0 * py / Math.max(1, sh - h));
    }
}
