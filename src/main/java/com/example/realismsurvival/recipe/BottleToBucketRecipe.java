package com.example.realismsurvival.recipe;

import com.example.realismsurvival.api.water.WaterApi;
import com.example.realismsurvival.api.water.WaterQuality;
import com.example.realismsurvival.water.BucketFill;
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

public class BottleToBucketRecipe extends CustomRecipe {

    /** Shared singleton — MapCodec + StreamCodec.unit must be the same instance for recipe sync. */
    public static final BottleToBucketRecipe INSTANCE = new BottleToBucketRecipe();
    public static final MapCodec<BottleToBucketRecipe> MAP_CODEC = MapCodec.unit(INSTANCE);
    public static final StreamCodec<RegistryFriendlyByteBuf, BottleToBucketRecipe> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);
    public static final RecipeSerializer<BottleToBucketRecipe> SERIALIZER =
            new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return parse(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        Parsed parsed = parse(input);
        if (parsed == null) {
            return ItemStack.EMPTY;
        }
        return BucketFill.createBucket(parsed.quality(), parsed.resultFill());
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> remainders = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        Parsed parsed = parse(input);
        if (parsed == null) {
            return remainders;
        }
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            // Bottles → glass; vessel bucket is consumed into the result (do not leave a partial).
            if (WaterApi.isGradedWaterBottle(stack.getItem())) {
                remainders.set(slot, new ItemStack(Items.GLASS_BOTTLE));
            }
        }
        return remainders;
    }

    private static @org.jspecify.annotations.Nullable Parsed parse(CraftingInput input) {
        ItemStack bucket = ItemStack.EMPTY;
        WaterQuality bucketQuality = null;
        int bucketFill = 0;
        WaterQuality bottleQuality = null;
        int bottleCount = 0;

        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.is(Items.BUCKET)) {
                if (!bucket.isEmpty()) {
                    return null;
                }
                bucket = stack;
                bucketFill = 0;
            } else if (WaterApi.isGradedWaterBucket(stack.getItem())) {
                if (!bucket.isEmpty()) {
                    return null;
                }
                bucket = stack;
                bucketQuality = WaterQuality.fromBucketItem(stack.getItem());
                bucketFill = BucketFill.getFill(stack);
            } else if (WaterApi.isGradedWaterBottle(stack.getItem())) {
                WaterQuality quality = WaterQuality.fromBottleItem(stack.getItem());
                if (quality == null || (bottleQuality != null && bottleQuality != quality)) {
                    return null;
                }
                bottleQuality = quality;
                // One bottle per occupied slot (crafting consumes 1/slot); ignore stack size.
                bottleCount++;
            } else {
                return null;
            }
        }

        if (bottleQuality == null || bottleCount < 1 || bottleCount > BucketFill.MAX) {
            return null;
        }
        if (bucket.isEmpty()) {
            return null;
        }
        if (bucket.is(Items.BUCKET)) {
            if (bucketFill != 0) {
                return null;
            }
            if (bottleCount > BucketFill.MAX) {
                return null;
            }
            return new Parsed(bottleQuality, bottleCount, bottleCount);
        }
        if (bucketQuality == null || bucketQuality != bottleQuality) {
            return null;
        }
        int total = bucketFill + bottleCount;
        if (total > BucketFill.MAX) {
            return null;
        }
        return new Parsed(bottleQuality, bottleCount, total);
    }

    @Override
    public RecipeSerializer<? extends CustomRecipe> getSerializer() {
        return SERIALIZER;
    }

    private record Parsed(WaterQuality quality, int bottleCount, int resultFill) {}
}
