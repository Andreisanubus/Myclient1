package dev.myclient.module.modules;

import dev.myclient.mixin.LivingEntityAccessor;
import dev.myclient.module.Category;
import dev.myclient.module.Module;

public class NoJumpDelay extends Module {
    public NoJumpDelay() { super("No Jump Delay", Category.MOVEMENT); }

    @Override public void onTick() {
        if (mc.player != null) ((LivingEntityAccessor) mc.player).myclient$setJumpingCooldown(0);
    }
}
