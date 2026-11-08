package dev.myclient.module;

import java.util.function.BooleanSupplier;

/** Setare generica. Boolean = comutator. Numere: NumberSetting. Moduri: ModeSetting.
 *  `visible` ascunde setarea in GUI cand conditia e falsa. */
public class Setting<T> {
    public final String name;
    public final T def;
    public T value;
    public BooleanSupplier visible = () -> true;

    public Setting(String name, T def) {
        this.name = name;
        this.def = def;
        this.value = def;
    }
}
