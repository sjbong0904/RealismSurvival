package com.example.realismsurvival.event;

import com.example.realismsurvival.RealismSurvival;
import com.example.realismsurvival.registry.ModItems;
import com.example.realismsurvival.water.GradedIceItems;
import com.example.realismsurvival.water.WetSpongeItems;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

@EventBusSubscriber(modid = RealismSurvival.MODID)
public final class ModCreativeTabs {

    private ModCreativeTabs() {}

    @SubscribeEvent
    static void onBuildCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(ModItems.WATER_FILTER);
            event.accept(ModItems.WATER_STILL);
            event.accept(ModItems.FILTER_CARTRIDGE);
            event.accept(ModItems.SUPPLY_POST_MARKER);
            event.accept(ModItems.CLINIC_TABLE);
        }
        if (event.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) {
            for (ItemStack ice : GradedIceItems.allQualityStacks()) {
                event.accept(ice);
            }
        }
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(ModItems.SALT_BLOCK);
            for (ItemStack sponge : WetSpongeItems.allQualityStacks()) {
                event.accept(sponge);
            }
        }
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ModItems.THERMOMETER);
        }
        if (event.getTabKey() != CreativeModeTabs.FOOD_AND_DRINKS) {
            return;
        }
        event.accept(ModItems.SALT);
        event.accept(ModItems.DISTILLED_WATER_BOTTLE);
        event.accept(ModItems.FRESH_WATER_BOTTLE);
        event.accept(ModItems.STAGNANT_WATER_BOTTLE);
        event.accept(ModItems.SALT_WATER_BOTTLE);
        event.accept(ModItems.DISTILLED_WATER_BUCKET);
        event.accept(ModItems.FRESH_WATER_BUCKET);
        event.accept(ModItems.STAGNANT_WATER_BUCKET);
        event.accept(ModItems.SALT_WATER_BUCKET);
        event.accept(ModItems.DISTILLED_ICE_BUCKET);
        event.accept(ModItems.FRESH_ICE_BUCKET);
        event.accept(ModItems.STAGNANT_ICE_BUCKET);
        event.accept(ModItems.SNOW_BUCKET);
    }
}
