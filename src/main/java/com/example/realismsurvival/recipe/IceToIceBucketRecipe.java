package com.example.realismsurvival.recipe;

import com.example.realismsurvival.api.water.WaterApi;
import com.example.realismsurvival.api.water.WaterQuality;
import com.example.realismsurvival.water.GradedIceItems;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/** graded ice (freshwater QUALITY) + empty bucket → matching ice bucket. */
public class IceToIceBucketRecipe extends CustomRecipe {

    /** Shared singleton — MapCodec + StreamCodec.unit must be the same instance for recipe sync. */
    public static final IceToIceBucketRecipe INSTANCE = new IceToIceBucketRecipe();
    public static final MapCodec<IceToIceBucketRecipe> MAP_CODEC = MapCodec.unit(INSTANCE);
    public static final StreamCodec<RegistryFriendlyByteBuf, IceToIceBucketRecipe> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);
    public static final RecipeSerializer<IceToIceBucketRecipe> SERIALIZER =
            new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return parse(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        WaterQuality quality = parse(input);
        if (quality == null) {
            return ItemStack.EMPTY;
        }
        ItemStack bucket = WaterApi.createIceBucket(quality);
        return bucket != null ? bucket : ItemStack.EMPTY;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        return NonNullList.withSize(input.size(), ItemStack.EMPTY);
    }

    private static @Nullable WaterQuality parse(CraftingInput input) {
        ItemStack ice = ItemStack.EMPTY;
        ItemStack bucket = ItemStack.EMPTY;

        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (GradedIceItems.isGradedIce(stack)) {
                if (!ice.isEmpty()) {
                    return null;
                }
                ice = stack;
            } else if (stack.is(Items.BUCKET)) {
                if (!bucket.isEmpty()) {
                    return null;
                }
                bucket = stack;
            } else {
                return null;
            }
        }

        if (ice.isEmpty() || bucket.isEmpty()) {
            return null;
        }
        return GradedIceItems.freshwaterQualityOrNull(ice);
    }

    @Override
    public RecipeSerializer<? extends CustomRecipe> getSerializer() {
        return SERIALIZER;
    }
}
