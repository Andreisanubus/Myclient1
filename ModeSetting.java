package dev.myclient.module;

import java.util.List;

public class ModeSetting extends Setting<String> {
    public final List<String> modes;

    public ModeSetting(String name, String def, String... modes) {
        super(name, def);
        this.modes = List.of(modes);
    }

    public void next() { value = modes.get((modes.indexOf(value) + 1) % modes.size()); }
    public void prev() { value = modes.get((modes.indexOf(value) - 1 + modes.size()) % modes.size()); }
    public boolean is(String m) { return value.equals(m); }
}
