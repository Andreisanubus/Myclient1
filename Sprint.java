package dev.myclient.module.modules;

import dev.myclient.module.Category;
import dev.myclient.module.Module;
import dev.myclient.module.Setting;
import net.minecraft.client.network.ClientPlayerEntity;

public class Sprint extends Module {
    private final Setting<Boolean> omni = bool("Omni Sprint", false);
    private final Setting<Boolean> hunger = bool("Check Hunger", true);

    public Sprint() { super("Sprint", Category.MOVEMENT); }

    @Override public void onTick() {
        ClientPlayerEntity p = mc.player;
        if (p == null) return;
        var o = mc.options;
        boolean moving = omni.value
                ? (o.forwardKey.isPressed() || o.backKey.isPressed() || o.leftKey.isPressed() || o.rightKey.isPressed())
                : o.forwardKey.isPressed();
        boolean fed = !hunger.value || p.getHungerManager().getFoodLevel() > 6;
        if (moving && fed && !p.isSneaking() && !p.horizontalCollision) p.setSprinting(true);
    }
}
