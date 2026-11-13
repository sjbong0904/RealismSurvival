package com.example.realismsurvival.api.water;

import com.example.realismsurvival.registry.ModDataComponents;
import com.example.realismsurvival.registry.ModFluids;
import com.example.realismsurvival.water.BucketFill;
import com.example.realismsurvival.water.GradedWaterBottleItem;
import com.example.realismsurvival.water.GradedWaterBucketItem;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Consumables;

/**
 * Factories for addon graded-water containers (shared {@code graded_water} fluid + QUALITY).
 *
 * <p>Prefer these over touching {@code ModFluids} / {@code ModDataComponents} / {@code BucketFill}
 * directly — those packages are not part of the frozen public surface.
 */
public final class GradedWaterItems {

    /**
     * Filled buckets: vanilla rule ({@code stacksTo(1)}), same as {@code minecraft:water_bucket}.
     * Partial fill still uses {@code BUCKET_FILL}; stacks never merge across fill levels anyway.
     */
    public static final int BUCKET_STACK_SIZE = 1;

    /** Graded bottles: match honey bottle / core hydration bottles. */
    public static final int BOTTLE_STACK_SIZE = 16;

    /** @deprecated Use {@link #BOTTLE_STACK_SIZE} or {@link #BUCKET_STACK_SIZE}. */
    @Deprecated
    public static final int STACK_SIZE = BOTTLE_STACK_SIZE;

    private GradedWaterItems() {}

    public static Item.Properties bucketProperties() {
        return new Item.Properties()
                .stacksTo(BUCKET_STACK_SIZE)
                .craftRemainder(Items.BUCKET)
                .component(ModDataComponents.BUCKET_FILL, BucketFill.DEFAULT);
    }

    public static Item.Properties bottleProperties() {
        return new Item.Properties()
                .stacksTo(BOTTLE_STACK_SIZE)
                .enchantable(15)
                .component(DataComponents.CONSUMABLE, Consumables.DEFAULT_DRINK)
                .usingConvertsTo(Items.GLASS_BOTTLE);
    }

    public static Item createBucket(Supplier<WaterQuality> quality, Item.Properties properties) {
        return new GradedWaterBucketItem(ModFluids.GRADED_WATER.get(), quality, properties);
    }

    public static Item createBottle(Supplier<WaterQuality> quality, Item.Properties properties) {
        return new GradedWaterBottleItem(quality, properties);
    }
}
