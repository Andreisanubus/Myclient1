package dev.myclient.module.modules;

import dev.myclient.config.FriendManager;
import dev.myclient.module.Category;
import dev.myclient.module.Module;
import dev.myclient.module.Setting;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;

public class FriendGuard extends Module {
    private final Setting<Boolean> notify = bool("Notify", true);
    private final Setting<Boolean> sneakBypass = bool("Allow When Sneaking", false);

    public FriendGuard() {
        super("Friend Guard", Category.COMBAT);
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (!isEnabled() || !world.isClient()) return ActionResult.PASS;
            if (entity instanceof PlayerEntity pe && FriendManager.isFriend(pe.getName().getString())) {
                if (sneakBypass.value && player.isSneaking()) return ActionResult.PASS;
                if (notify.value) player.sendMessage(Text.literal("[Friend Guard] hit blocked: " + pe.getName().getString()), true);
                return ActionResult.FAIL;
            }
            return ActionResult.PASS;
        });
    }
}
