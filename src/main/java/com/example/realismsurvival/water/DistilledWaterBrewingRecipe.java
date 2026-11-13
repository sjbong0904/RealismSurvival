package com.example.realismsurvival.water;

import com.example.realismsurvival.registry.ModItems;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.neoforged.neoforge.common.brewing.IBrewingRecipe;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jspecify.annotations.Nullable;

/**
 * Treats {@code distilled_water_bottle} as {@link Potions#WATER} for brewing-stand mixes.
 *
 * <p>Delegates output to vanilla {@link PotionBrewing#mix} on a synthetic water potion so FoT and
 * other WATER-base mixes stay in sync. Recursion-safe: the synthetic water bottle is not this input.
 */
public final class DistilledWaterBrewingRecipe implements IBrewingRecipe {

    public static final DistilledWaterBrewingRecipe INSTANCE = new DistilledWaterBrewingRecipe();

    private DistilledWaterBrewingRecipe() {}

    @Override
    public boolean isInput(ItemStack input) {
        return input.is(ModItems.DISTILLED_WATER_BOTTLE.get());
    }

    @Override
    public boolean isIngredient(ItemStack ingredient) {
        if (ingredient.isEmpty()) {
            return false;
        }
        PotionBrewing brewing = resolveBrewing();
        if (brewing == null) {
            return false;
        }
        return brewing.hasMix(waterBottle(), ingredient);
    }

    @Override
    public ItemStack getOutput(ItemStack input, ItemStack ingredient) {
        if (!isInput(input) || ingredient.isEmpty()) {
            return ItemStack.EMPTY;
        }
        PotionBrewing brewing = resolveBrewing();
        if (brewing == null) {
            return ItemStack.EMPTY;
        }
        // mix(ingredient, source) — custom recipes are tried first; water bottle skips this recipe.
        return brewing.mix(ingredient, waterBottle());
    }

    private static ItemStack waterBottle() {
        return PotionContents.createItemStack(Items.POTION, Potions.WATER);
    }

    private static @Nullable PotionBrewing resolveBrewing() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            return server.potionBrewing();
        }
        return ClientBrewingBridge.get();
    }
}
