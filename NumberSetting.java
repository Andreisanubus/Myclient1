package dev.myclient.module;

public class NumberSetting extends Setting<Double> {
    public final double min, max, step;

    public NumberSetting(String name, double def, double min, double max, double step) {
        super(name, def);
        this.min = min; this.max = max; this.step = step;
    }

    public void set(double v) {
        v = Math.max(min, Math.min(max, v));
        v = Math.round(v / step) * step;
        value = Math.round(v * 1000.0) / 1000.0;
    }

    public int asInt() { return (int) Math.round(value); }
    public double get() { return value; }
}
