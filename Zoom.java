package dev.myclient.module.modules;

import dev.myclient.module.Category;
import dev.myclient.module.Module;
import dev.myclient.module.NumberSetting;
import dev.myclient.module.Setting;

public class Zoom extends Module {
    private final NumberSetting fov = num("FOV", 30, 30, 110, 1);
    private final Setting<Boolean> hideHud = bool("Hide HUD", false);
    private int oldFov = 70;
    private boolean oldHud;

    public Zoom() { super("Zoom", Category.RENDER); }

    @Override protected void onEnable() {
        oldFov = mc.options.getFov().getValue();
        oldHud = mc.options.hudHidden;
    }

    @Override protected void onDisable() {
        mc.options.getFov().setValue(oldFov);
        mc.options.hudHidden = oldHud;
    }

    @Override public void onTick() {
        mc.options.getFov().setValue(fov.asInt());
        mc.options.hudHidden = hideHud.value || oldHud;
    }
}
