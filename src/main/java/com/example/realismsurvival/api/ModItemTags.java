package com.example.realismsurvival.api;

import com.example.realismsurvival.RealismSurvival;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * Public item tags for machine inputs and addon integration (spec §15.3).
 * Addons add their buckets/bottles here so core machines recognize them.
 */
public final class ModItemTags {

    private ModItemTags() {}

    /** Buckets accepted by the water filter (default: stagnant). Output is always core fresh bottles. */
    public static final TagKey<Item> FILTER_INPUT_BUCKETS =
            TagKey.create(Registries.ITEM, RealismSurvival.id("filter_input_buckets"));

    /** Consumable filter media required for the water filter to run (durability damaged per bottle). */
    public static final TagKey<Item> FILTER_CARTRIDGES =
            TagKey.create(Registries.ITEM, RealismSurvival.id("filter_cartridges"));

    /** Buckets accepted by the water still (default: fresh + salt). Output is always core distilled. */
    public static final TagKey<Item> STILL_INPUT_BUCKETS =
            TagKey.create(Registries.ITEM, RealismSurvival.id("still_input_buckets"));

    /** Still inputs that also produce salt byproduct (default: salt water buckets). */
    public static final TagKey<Item> STILL_SALT_SOURCE_BUCKETS =
            TagKey.create(Registries.ITEM, RealismSurvival.id("still_salt_source_buckets"));

    /**
     * Non-distilled water buckets (smelting / recipes). Extend with addon buckets as needed.
     */
    public static final TagKey<Item> UNDISTILLED_WATER_BUCKETS =
            TagKey.create(Registries.ITEM, RealismSurvival.id("undistilled_water_buckets"));
}
