package dev.myclient.module;

import dev.myclient.util.Colors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import java.util.ArrayList;
import java.util.List;

public abstract class Module {
    public final String name;
    public final Category category;
    public int key = -1;
    public final List<Setting<?>> settings = new ArrayList<>();
    protected final MinecraftClient mc = MinecraftClient.getInstance();
    private boolean enabled;

    protected Module(String name, Category category) {
        this.name = name;
        this.category = category;
    }

    protected <S extends Setting<?>> S add(S s) { settings.add(s); return s; }
    protected HeaderSetting header(String title) { return add(new HeaderSetting(title)); }
    protected Setting<Boolean> bool(String n, boolean d) { return add(new Setting<Boolean>(n, d)); }
    protected NumberSetting num(String n, double d, double min, double max, double step) { return add(new NumberSetting(n, d, min, max, step)); }
    protected ModeSetting mode(String n, String d, String... modes) { return add(new ModeSetting(n, d, modes)); }
    protected ModeSetting color(String n, String d) { return add(new ModeSetting(n, d, Colors.NAMES)); }

    public boolean isEnabled() { return enabled; }
    public void toggle() { setEnabled(!enabled); }

    public void setEnabled(boolean v) {
        if (enabled == v) return;
        enabled = v;
        if (v) onEnable(); else onDisable();
    }

    /** false = modulul exista in GUI cu setarile lui, dar logica nu e scrisa inca. */
    public boolean implemented() { return true; }

    protected void onEnable() {}
    protected void onDisable() {}
    public void onTick() {}
    /** Se apeleaza la fiecare tick, chiar si fara lume incarcata (meniu principal, deconectat). */
    public void onGlobalTick() {}
    /** Desenare peste ecran (HUD). Proiectia lume -> ecran e pregatita in Proj. */
    public void onRender(DrawContext c, int sw, int sh) {}
}
