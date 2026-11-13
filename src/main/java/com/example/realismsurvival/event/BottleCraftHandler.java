package com.example.realismsurvival.event;

import com.example.realismsurvival.RealismSurvival;
import com.example.realismsurvival.api.water.WaterApi;
import com.example.realismsurvival.water.BottleEnchantmentTransfer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * When crafting a graded water bottle from a bucket + glass bottle, keep warming/cooling
 * enchantments from the glass bottle ingredient.
 */
@EventBusSubscriber(modid = RealismSurvival.MODID)
public final class BottleCraftHandler {

    private BottleCraftHandler() {}

    @SubscribeEvent
    static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        ItemStack result = event.getCrafting();
        if (!WaterApi.isGradedWaterBottle(result.getItem())) {
            return;
        }
        Container matrix = event.getInventory();
        for (int i = 0; i < matrix.getContainerSize(); i++) {
            ItemStack ingredient = matrix.getItem(i);
            if (ingredient.is(Items.GLASS_BOTTLE)) {
                BottleEnchantmentTransfer.copyEnchantments(ingredient, result);
                return;
            }
        }
    }
}
