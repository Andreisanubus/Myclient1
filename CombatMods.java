package dev.myclient.module.modules;

import dev.myclient.module.Category;
import dev.myclient.module.Module;
import dev.myclient.module.ModeSetting;
import dev.myclient.module.NumberSetting;
import dev.myclient.module.Setting;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;

public final class CombatMods {
    private CombatMods() {}

    /** Scoate cristalul imediat dupa lovitura (pe client), ca urmatorul sa poata fi pus fara asteptare. */
    public static class CrystalOptimizer extends Module {
        private final Setting<Boolean> debug;
        private final Setting<Boolean> onlyHoldingWeapon;

        public CrystalOptimizer() {
            super("Crystal Optimizer", Category.COMBAT);
            header("Behaviour");
            onlyHoldingWeapon = bool("Only When Not Holding Crystal", false);
            debug = bool("Debug Message", false);
            AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
                if (isEnabled() && world.isClient() && entity instanceof EndCrystalEntity) {
                    if (onlyHoldingWeapon.value && player.getMainHandStack().isOf(Items.END_CRYSTAL)) return ActionResult.PASS;
                    entity.discard();
                    if (debug.value) player.sendMessage(Text.literal("[Crystal Optimizer] cristal scos"), true);
                }
                return ActionResult.PASS;
            });
        }
    }

    /**
     * Totem in offhand. Timpul de reactie e aleator intre Min si Max (nu o valoare fixa), iar metoda poate fi swap sau click-uri ca pe cursor.
     * Un anti-cheat poate totusi detecta; nimic de aici nu garanteaza invizibilitate.
     */
    public static class AutoTotem extends Module {
        private final NumberSetting minMs, maxMs, health;
        private final ModeSetting method;
        private final Setting<Boolean> hotbarFirst, onlyAfterPop, containerSafe, extraJitter;
        private boolean hadTotem;
        private long due;

        public AutoTotem() {
            super("Auto Totem", Category.COMBAT);
            header("Timing");
            minMs = num("Reaction Min (ms)", 90, 0, 500, 5);
            maxMs = num("Reaction Max (ms)", 180, 0, 500, 5);
            extraJitter = bool("Extra Random Delay", true);
            header("Method");
            method = mode("Method", "Swap", "Swap", "Pickup Clicks");
            hotbarFirst = bool("Use Hotbar Totems First", true);
            onlyAfterPop = bool("Only After A Pop", false);
            health = num("Only Below Hearts (0 = always)", 0, 0, 20, 1);
            containerSafe = bool("Pause In Other Containers", true);
            extraJitter.visible = () -> true;
        }

        @Override public void onTick() {
            PlayerEntity p = mc.player;
            var im = mc.interactionManager;
            if (p == null || im == null) return;
            boolean has = p.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING);
            long now = System.currentTimeMillis();
            if (has) { hadTotem = true; due = 0; return; }
            boolean popped = hadTotem;
            hadTotem = false;
            if (onlyAfterPop.value && !popped && due == 0) return;
            if (health.asInt() > 0 && p.getHealth() + p.getAbsorptionAmount() > health.asInt() * 2) return;
            if (due == 0) {
                double lo = Math.min(minMs.get(), maxMs.get()), hi = Math.max(minMs.get(), maxMs.get());
                double r = lo + Math.random() * (hi - lo);
                if (extraJitter.value) r += Math.random() * 25;
                due = now + (long) r;
                return;
            }
            if (now < due) return;
            if (containerSafe.value && mc.currentScreen != null && !(mc.currentScreen instanceof InventoryScreen)) return;
            int slot = find(p);
            due = 0;
            if (slot < 0) return;
            int id = p.playerScreenHandler.syncId;
            if (method.is("Swap")) {
                im.clickSlot(id, slot, 40, SlotActionType.SWAP, p);
            } else {
                im.clickSlot(id, slot, 0, SlotActionType.PICKUP, p);
                im.clickSlot(id, 45, 0, SlotActionType.PICKUP, p);
                if (!p.playerScreenHandler.getCursorStack().isEmpty()) im.clickSlot(id, slot, 0, SlotActionType.PICKUP, p);
            }
        }

        private int find(PlayerEntity p) {
            for (int n = 0; n < 36; n++) {
                int i = hotbarFirst.value ? n : (n + 9) % 36;
                if (p.getInventory().getStack(i).isOf(Items.TOTEM_OF_UNDYING)) return i < 9 ? 36 + i : i;
            }
            return -1;
        }
    }
}
