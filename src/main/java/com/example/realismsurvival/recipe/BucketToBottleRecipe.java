package com.example.realismsurvival.recipe;

import com.example.realismsurvival.api.water.WaterApi;
import com.example.realismsurvival.api.water.WaterQuality;
import com.example.realismsurvival.water.BottleEnchantmentTransfer;
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

public class BucketToBottleRecipe extends CustomRecipe {

    /** Shared singleton — MapCodec + StreamCodec.unit must be the same instance for recipe sync. */
    public static final BucketToBottleRecipe INSTANCE = new BucketToBottleRecipe();
    public static final MapCodec<BucketToBottleRecipe> MAP_CODEC = MapCodec.unit(INSTANCE);
    public static final StreamCodec<RegistryFriendlyByteBuf, BucketToBottleRecipe> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);
    public static final RecipeSerializer<BucketToBottleRecipe> SERIALIZER =
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
        ItemStack bottle = parsed.quality().createBottle();
        BottleEnchantmentTransfer.copyEnchantments(parsed.glassBottle(), bottle);
        return bottle;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> remainders = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        Parsed parsed = parse(input);
        if (parsed == null) {
            return remainders;
        }
        ItemStack bucketRemainder = BucketFill.afterPour(parsed.bucket());
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (ItemStack.isSameItemSameComponents(stack, parsed.bucket())) {
                remainders.set(slot, bucketRemainder);
                break;
            }
        }
        return remainders;
    }

    private static @org.jspecify.annotations.Nullable Parsed parse(CraftingInput input) {
        ItemStack bucket = ItemStack.EMPTY;
        ItemStack glass = ItemStack.EMPTY;
        int count = 0;
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            count++;
            if (WaterApi.isGradedWaterBucket(stack.getItem())) {
                if (!bucket.isEmpty()) {
                    return null;
                }
                bucket = stack;
            } else if (stack.is(Items.GLASS_BOTTLE)) {
                if (!glass.isEmpty()) {
                    return null;
                }
                glass = stack;
            } else {
                return null;
            }
        }
        if (count != 2 || bucket.isEmpty() || glass.isEmpty()) {
            return null;
        }
        WaterQuality quality = WaterQuality.fromBucketItem(bucket.getItem());
        if (quality == null || BucketFill.getFill(bucket) <= 0) {
            return null;
        }
        return new Parsed(bucket, glass, quality);
    }

    @Override
    public RecipeSerializer<? extends CustomRecipe> getSerializer() {
        return SERIALIZER;
    }

    private record Parsed(ItemStack bucket, ItemStack glassBottle, WaterQuality quality) {}
}
