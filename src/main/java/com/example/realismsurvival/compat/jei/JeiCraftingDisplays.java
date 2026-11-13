package com.example.realismsurvival.compat.jei;

import com.example.realismsurvival.RealismSurvival;
import com.example.realismsurvival.api.water.WaterApi;
import com.example.realismsurvival.api.water.WaterQuality;
import com.example.realismsurvival.registry.ModBlocks;
import com.example.realismsurvival.registry.ModWaterQualities;
import com.example.realismsurvival.water.BucketFill;
import com.example.realismsurvival.water.GradedIceItems;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapelessRecipe;

/**
 * JEI-only shapeless displays for {@code CustomRecipe} water conversions.
 * These are not registered with the game recipe manager — crafting still uses the custom matchers.
 */
final class JeiCraftingDisplays {

    private JeiCraftingDisplays() {}

    static List<RecipeHolder<CraftingRecipe>> create() {
        List<RecipeHolder<CraftingRecipe>> recipes = new ArrayList<>();
        for (WaterQuality quality : ModWaterQualities.REGISTRY) {
            recipes.add(bucketToBottle(quality));
            for (int bottles = BucketFill.MIN; bottles <= BucketFill.MAX; bottles++) {
                recipes.add(bottleToBucket(quality, bottles));
            }
            if (GradedIceItems.isFreshwaterIceQuality(quality)) {
                RecipeHolder<CraftingRecipe> iceRecipe = iceToIceBucket(quality);
                if (iceRecipe != null) {
                    recipes.add(iceRecipe);
                }
            }
        }
        return recipes;
    }

    private static RecipeHolder<CraftingRecipe> bucketToBottle(WaterQuality quality) {
        ItemStack bucket = BucketFill.createBucket(quality, BucketFill.MAX);
        ItemStack bottle = quality.createBottle();
        return shapeless(
                "jei/bucket_to_bottle/" + idPath(quality),
                bottle,
                List.of(Ingredient.of(bucket.getItem()), Ingredient.of(Items.GLASS_BOTTLE))
        );
    }

    private static RecipeHolder<CraftingRecipe> bottleToBucket(WaterQuality quality, int bottleCount) {
        List<Ingredient> ingredients = new ArrayList<>(bottleCount + 1);
        ingredients.add(Ingredient.of(Items.BUCKET));
        for (int i = 0; i < bottleCount; i++) {
            ingredients.add(Ingredient.of(quality.bottleItem()));
        }
        ItemStack result = BucketFill.createBucket(quality, bottleCount);
        return shapeless(
                "jei/bottle_to_bucket/" + idPath(quality) + "_" + bottleCount,
                result,
                ingredients
        );
    }

    private static @Nullable RecipeHolder<CraftingRecipe> iceToIceBucket(WaterQuality quality) {
        ItemStack result = WaterApi.createIceBucket(quality);
        if (result == null || result.isEmpty()) {
            return null;
        }
        return shapeless(
                "jei/ice_to_ice_bucket/" + idPath(quality),
                result,
                List.of(Ingredient.of(ModBlocks.GRADED_ICE.asItem()), Ingredient.of(Items.BUCKET))
        );
    }

    private static RecipeHolder<CraftingRecipe> shapeless(String path, ItemStack result, List<Ingredient> ingredients) {
        ShapelessRecipe recipe = new ShapelessRecipe(
                new Recipe.CommonInfo(false),
                new CraftingRecipe.CraftingBookInfo(CraftingBookCategory.MISC, "realismsurvival_jei"),
                ItemStackTemplate.fromNonEmptyStack(result),
                List.copyOf(ingredients)
        );
        ResourceKey<Recipe<?>> id = ResourceKey.create(Registries.RECIPE, RealismSurvival.id(path));
        return new RecipeHolder<>(id, recipe);
    }

    private static String idPath(WaterQuality quality) {
        return ModWaterQualities.REGISTRY.getKey(quality).getPath();
    }
}
