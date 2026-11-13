package com.example.realismsurvival.water;

import com.example.realismsurvival.api.water.WaterApi;
import com.example.realismsurvival.api.water.WaterQuality;
import com.example.realismsurvival.registry.ModDataComponents;
import com.example.realismsurvival.registry.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Partial fill (1..3) for graded water / ice / snow buckets (spec §15.5.1 W-05). */
public final class BucketFill {

    public static final int MIN = 1;
    public static final int MAX = 3;
    public static final int DEFAULT = MAX;

    private BucketFill() {}

    public static boolean supportsFill(Item item) {
        return WaterApi.isGradedWaterBucket(item)
                || WaterApi.isGradedIceBucket(item)
                || item == ModItems.SNOW_BUCKET.get()
                || item == Items.POWDER_SNOW_BUCKET;
    }

    public static boolean supportsFill(ItemStack stack) {
        return supportsFill(stack.getItem());
    }

    /** @deprecated use {@link #supportsFill(ItemStack)} */
    @Deprecated
    public static boolean isGradedBucket(ItemStack stack) {
        return WaterApi.isGradedWaterBucket(stack.getItem());
    }

    public static int getFill(ItemStack stack) {
        if (!supportsFill(stack)) {
            return 0;
        }
        if (stack.has(ModDataComponents.BUCKET_FILL.get())) {
            return stack.getOrDefault(ModDataComponents.BUCKET_FILL.get(), DEFAULT);
        }
        // Vanilla powder-snow bucket / legacy stacks without the component → treat as full.
        return DEFAULT;
    }

    public static void setFill(ItemStack stack, int fill) {
        if (supportsFill(stack)) {
            stack.set(ModDataComponents.BUCKET_FILL.get(), clamp(fill));
        }
    }

    public static void decrement(ItemStack stack) {
        setFill(stack, getFill(stack) - 1);
    }

    public static boolean isFull(ItemStack stack) {
        return getFill(stack) >= MAX;
    }

    public static ItemStack createBucket(WaterQuality quality, int fill) {
        ItemStack stack = new ItemStack(quality.bucketItem());
        setFill(stack, fill);
        return stack;
    }

    public static ItemStack createIceBucket(WaterQuality quality, int fill) {
        ItemStack stack = WaterApi.createIceBucket(quality);
        if (stack == null) {
            return ItemStack.EMPTY;
        }
        setFill(stack, fill);
        return stack;
    }

    public static ItemStack createSnowBucket(int fill) {
        ItemStack stack = new ItemStack(ModItems.SNOW_BUCKET.get());
        setFill(stack, fill);
        return stack;
    }

    /**
     * Result after pouring/sipping one unit. Always a fresh stack — never mutates {@code bucket},
     * so a multi-count stack of full buckets cannot all drop to fill−1 together.
     */
    public static ItemStack afterPour(ItemStack bucket) {
        int fill = getFill(bucket);
        if (fill <= MIN) {
            return new ItemStack(Items.BUCKET);
        }
        Item item = bucket.getItem();
        if (WaterApi.isGradedIceBucket(item)) {
            WaterQuality quality = WaterApi.fromIceBucketItem(item);
            return quality != null ? createIceBucket(quality, fill - 1) : ItemStack.EMPTY;
        }
        if (item == ModItems.SNOW_BUCKET.get() || item == Items.POWDER_SNOW_BUCKET) {
            return createSnowBucket(fill - 1);
        }
        WaterQuality quality = WaterApi.fromBucketItem(item);
        if (quality == null) {
            return ItemStack.EMPTY;
        }
        return createBucket(quality, fill - 1);
    }

    /**
     * Sip strength vs a full bottle. A bucket is three bottle-units ({@link #MAX} fill), so each
     * sip applies the full bottle thirst/temp/effects. Three sips ≈ three bottles.
     */
    public static float sipMultiplier(ItemStack stack) {
        return 1.0F;
    }

    private static int clamp(int fill) {
        return Math.clamp(fill, MIN, MAX);
    }
}
