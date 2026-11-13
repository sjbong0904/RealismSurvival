package com.example.realismsurvival.registry;

import com.example.realismsurvival.RealismSurvival;
import com.example.realismsurvival.recipe.BottleToBucketRecipe;
import com.example.realismsurvival.recipe.BucketToBottleRecipe;
import com.example.realismsurvival.recipe.IceToIceBucketRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModRecipeSerializers {

    private ModRecipeSerializers() {}

    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, RealismSurvival.MODID);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<BucketToBottleRecipe>> BUCKET_TO_BOTTLE =
            SERIALIZERS.register("bucket_to_bottle", () -> BucketToBottleRecipe.SERIALIZER);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<BottleToBucketRecipe>> BOTTLE_TO_BUCKET =
            SERIALIZERS.register("bottle_to_bucket", () -> BottleToBucketRecipe.SERIALIZER);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<IceToIceBucketRecipe>> ICE_TO_ICE_BUCKET =
            SERIALIZERS.register("ice_to_ice_bucket", () -> IceToIceBucketRecipe.SERIALIZER);

    public static void register(IEventBus modEventBus) {
        SERIALIZERS.register(modEventBus);
    }
}
