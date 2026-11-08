package dev.myclient.module;

import net.minecraft.client.gui.DrawContext;
import java.util.ArrayList;
import java.util.List;

public class ModuleManager {
    private static final ModuleManager INSTANCE = new ModuleManager();
    public static ModuleManager get() { return INSTANCE; }

    private final List<Module> mods = new ArrayList<>();

    public void init() { Catalog.registerAll(mods); }

    public List<Module> all() { return mods; }
    public List<Module> in(Category c) { return mods.stream().filter(m -> m.category == c).toList(); }

    /** Un modul cu eroare se opreste singur, ca sa nu strice tot jocul. */
    private void fail(Module m, Throwable t) {
        System.err.println("[MyClient] eroare in modulul " + m.name + ": " + t);
        t.printStackTrace();
        try { m.setEnabled(false); } catch (Throwable ignored) {}
    }

    public void tick() {
        for (Module m : new ArrayList<>(mods)) if (m.isEnabled()) { try { m.onTick(); } catch (Throwable t) { fail(m, t); } }
    }

    public void globalTick() {
        for (Module m : new ArrayList<>(mods)) if (m.isEnabled()) { try { m.onGlobalTick(); } catch (Throwable t) { fail(m, t); } }
    }

    public void render(DrawContext c, int sw, int sh) {
        for (Module m : new ArrayList<>(mods)) if (m.isEnabled()) { try { m.onRender(c, sw, sh); } catch (Throwable t) { fail(m, t); } }
    }
}
