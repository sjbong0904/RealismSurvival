package com.example.realismsurvival.event;

import com.example.realismsurvival.RealismSurvival;
import com.example.realismsurvival.water.VanillaWaterItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Converts leftover vanilla water buckets / water potions in player inventory into graded items
 * so still/filter and other RS paths never see {@code minecraft:water_bucket} as a dead stack.
 */
@EventBusSubscriber(modid = RealismSurvival.MODID)
public final class VanillaWaterItemRewriteHandler {

    private VanillaWaterItemRewriteHandler() {}

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        rewriteInventory(player);
    }

    /** Visible for GameTests. */
    public static void rewriteInventory(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            ItemStack rewritten = rewrite(stack, player);
            if (rewritten != stack) {
                inventory.setItem(i, rewritten);
            }
        }
    }

    private static ItemStack rewrite(ItemStack stack, ServerPlayer player) {
        if (VanillaWaterItems.isVanillaWaterBucket(stack)) {
            return VanillaWaterItems.replaceWaterBucket(stack, player.level(), player.blockPosition());
        }
        if (VanillaWaterItems.isVanillaWaterPotion(stack)) {
            return VanillaWaterItems.replaceWaterPotionWithDistilled(stack);
        }
        return stack;
    }
}
