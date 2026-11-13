package com.example.realismsurvival.compat.jei;

import com.example.realismsurvival.RealismSurvival;
import com.example.realismsurvival.api.water.WaterQuality;
import com.example.realismsurvival.registry.ModBlocks;
import com.example.realismsurvival.registry.ModDataComponents;
import com.example.realismsurvival.registry.ModItems;
import com.example.realismsurvival.registry.ModWaterQualities;
import com.example.realismsurvival.water.GradedIceItems;
import com.example.realismsurvival.water.WetSpongeItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.registration.IExtraIngredientRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * Soft JEI bridge: custom water crafting variants + subtypes for fill/QUALITY.
 * Discovered only when JEI is installed ({@link JeiPlugin}).
 */
@JeiPlugin
public final class RealismSurvivalJeiPlugin implements IModPlugin {

    private static final Identifier UID = RealismSurvival.id("jei_plugin");

    @Override
    public Identifier getPluginUid() {
        return UID;
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        for (WaterQuality quality : ModWaterQualities.REGISTRY) {
            Item bucket = quality.bucketItem();
            registration.registerFromDataComponentTypes(bucket, ModDataComponents.BUCKET_FILL.get());
        }
        registration.registerFromDataComponentTypes(
                ModItems.DISTILLED_ICE_BUCKET.get(),
                ModDataComponents.BUCKET_FILL.get()
        );
        registration.registerFromDataComponentTypes(
                ModItems.FRESH_ICE_BUCKET.get(),
                ModDataComponents.BUCKET_FILL.get()
        );
        registration.registerFromDataComponentTypes(
                ModItems.STAGNANT_ICE_BUCKET.get(),
                ModDataComponents.BUCKET_FILL.get()
        );
        registration.registerFromDataComponentTypes(
                ModItems.SNOW_BUCKET.get(),
                ModDataComponents.BUCKET_FILL.get()
        );
        registration.registerFromDataComponentTypes(
                ModBlocks.GRADED_ICE.asItem(),
                DataComponents.BLOCK_STATE
        );
        registration.registerFromDataComponentTypes(Items.WET_SPONGE, DataComponents.BLOCK_STATE);
    }

    @Override
    public void registerExtraIngredients(IExtraIngredientRegistration registration) {
        // QUALITY variants are not separate items — list each stack so JEI shows them.
        registration.addExtraItemStacks(GradedIceItems.allQualityStacks());
        registration.addExtraItemStacks(WetSpongeItems.allQualityStacks());
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(RecipeTypes.CRAFTING, JeiCraftingDisplays.create());

        registration.addIngredientInfo(
                ModItems.THERMOMETER.get(),
                Component.translatable("jei.realismsurvival.thermometer")
        );
        BuiltInRegistries.ITEM
                .getOptional(Identifier.fromNamespaceAndPath("sereneseasons", "calendar"))
                .filter(item -> item != Items.AIR)
                .ifPresent(calendar -> registration.addIngredientInfo(
                        calendar,
                        Component.translatable("jei.realismsurvival.calendar")
                ));
    }
}
