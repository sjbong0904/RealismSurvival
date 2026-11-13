package com.example.realismsurvival.registry;

import com.example.realismsurvival.RealismSurvival;
import com.example.realismsurvival.api.water.WaterQuality;
import com.example.realismsurvival.event.ItemComponentModifiers;
import com.example.realismsurvival.item.SaltItem;
import com.example.realismsurvival.item.ThermometerItem;
import com.example.realismsurvival.water.BucketFill;
import com.example.realismsurvival.water.GradedIceBlockItem;
import com.example.realismsurvival.water.GradedIceBucketItem;
import com.example.realismsurvival.water.GradedWaterBottleItem;
import com.example.realismsurvival.water.GradedWaterBucketItem;
import com.example.realismsurvival.water.SnowBucketItem;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Consumables;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Water-quality buckets and drinkable bottles (spec §9.9).
 *
 * <p>Buckets place the matching graded fluid ({@link ModFluids}). Bottles are drinks that convert back to a glass bottle.
 * Item factories must not call {@code WaterQuality} holders during {@code ITEM} registration — that registry
 * may still be unbound (GameTest / load-order). Fluid + lazy quality supplier instead.
 */
public final class ModItems {

    private ModItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(RealismSurvival.MODID);

    public static final DeferredItem<Item> DISTILLED_WATER_BUCKET =
            registerWaterBucket("distilled_water_bucket", ModWaterQualities.DISTILLED);
    public static final DeferredItem<Item> FRESH_WATER_BUCKET =
            registerWaterBucket("fresh_water_bucket", ModWaterQualities.FRESH);
    public static final DeferredItem<Item> STAGNANT_WATER_BUCKET =
            registerWaterBucket("stagnant_water_bucket", ModWaterQualities.STAGNANT);
    public static final DeferredItem<Item> SALT_WATER_BUCKET =
            registerWaterBucket("salt_water_bucket", ModWaterQualities.SALT);

    /** Freshwater ice buckets (no salt). Craft: graded ice + empty bucket. */
    public static final DeferredItem<Item> DISTILLED_ICE_BUCKET =
            registerIceBucket("distilled_ice_bucket", ModWaterQualities.DISTILLED);
    public static final DeferredItem<Item> FRESH_ICE_BUCKET =
            registerIceBucket("fresh_ice_bucket", ModWaterQualities.FRESH);
    public static final DeferredItem<Item> STAGNANT_ICE_BUCKET =
            registerIceBucket("stagnant_ice_bucket", ModWaterQualities.STAGNANT);

    /** Scoopable powder-snow bucket (3-fill, drinkable, still input). */
    public static final DeferredItem<Item> SNOW_BUCKET = ITEMS.registerItem(
            "snow_bucket",
            SnowBucketItem::new,
            () -> new Item.Properties()
                    .stacksTo(1) // filled bucket — vanilla stacksTo(1)
                    .craftRemainder(Items.BUCKET)
                    .component(ModDataComponents.BUCKET_FILL, BucketFill.DEFAULT)
    );

    public static final DeferredItem<Item> DISTILLED_WATER_BOTTLE =
            registerWaterBottle("distilled_water_bottle", ModWaterQualities.DISTILLED);
    public static final DeferredItem<Item> FRESH_WATER_BOTTLE =
            registerWaterBottle("fresh_water_bottle", ModWaterQualities.FRESH);
    public static final DeferredItem<Item> STAGNANT_WATER_BOTTLE =
            registerWaterBottle("stagnant_water_bottle", ModWaterQualities.STAGNANT);
    public static final DeferredItem<Item> SALT_WATER_BOTTLE =
            registerWaterBottle("salt_water_bottle", ModWaterQualities.SALT);

    public static final DeferredItem<Item> SALT = ITEMS.registerItem(
            "salt",
            SaltItem::new,
            () -> new Item.Properties().stacksTo(64)
    );

    /** Bottles processed before a {@code filter_cartridge} breaks (item default, not config). */
    public static final int FILTER_CARTRIDGE_USES = 32;

    /** Required media for {@link ModBlocks#WATER_FILTER}. Damaged once per bottled output. */
    public static final DeferredItem<Item> FILTER_CARTRIDGE = ITEMS.registerItem(
            "filter_cartridge",
            Item::new,
            () -> new Item.Properties().stacksTo(1).durability(FILTER_CARTRIDGE_USES)
    );
    public static final DeferredItem<Item> THERMOMETER = ITEMS.registerItem(
            "thermometer",
            ThermometerItem::new,
            () -> new Item.Properties().stacksTo(1)
    );

    public static final DeferredItem<BlockItem> WATER_FILTER =
            ITEMS.registerSimpleBlockItem("water_filter", ModBlocks.WATER_FILTER);
    public static final DeferredItem<BlockItem> WATER_STILL =
            ITEMS.registerSimpleBlockItem("water_still", ModBlocks.WATER_STILL);
    public static final DeferredItem<BlockItem> SALT_BLOCK =
            ITEMS.registerSimpleBlockItem("salt_block", ModBlocks.SALT_BLOCK);
    public static final DeferredItem<BlockItem> SUPPLY_POST_MARKER =
            ITEMS.registerSimpleBlockItem("supply_post_marker", ModBlocks.SUPPLY_POST_MARKER);
    public static final DeferredItem<BlockItem> CLINIC_TABLE =
            ITEMS.registerSimpleBlockItem("clinic_table", ModBlocks.CLINIC_TABLE);
    public static final DeferredItem<BlockItem> GRADED_ICE = ITEMS.registerItem(
            "graded_ice",
            props -> new GradedIceBlockItem(ModBlocks.GRADED_ICE.get(), props),
            () -> new Item.Properties().useBlockDescriptionPrefix()
    );

    private static DeferredItem<Item> registerWaterBucket(String name, Supplier<WaterQuality> quality) {
        return ITEMS.registerItem(
                name,
                props -> new GradedWaterBucketItem(ModFluids.GRADED_WATER.get(), quality, props),
                () -> new Item.Properties()
                        .stacksTo(1) // filled bucket — same as minecraft:water_bucket
                        .craftRemainder(Items.BUCKET)
                        .component(ModDataComponents.BUCKET_FILL, BucketFill.DEFAULT)
        );
    }

    private static DeferredItem<Item> registerIceBucket(String name, Supplier<WaterQuality> quality) {
        // No CONSUMABLE component — drink via GradedIceBucketItem.finishUsingItem (same as water buckets).
        return ITEMS.registerItem(
                name,
                props -> new GradedIceBucketItem(quality, props),
                () -> new Item.Properties()
                        .stacksTo(1) // filled ice bucket — vanilla filled-bucket rule
                        .craftRemainder(Items.BUCKET)
                        .component(ModDataComponents.BUCKET_FILL, BucketFill.DEFAULT)
        );
    }

    private static DeferredItem<Item> registerWaterBottle(String name, Supplier<WaterQuality> quality) {
        return ITEMS.registerItem(
                name,
                props -> new GradedWaterBottleItem(quality, props),
                () -> new Item.Properties()
                        .stacksTo(ItemComponentModifiers.HYDRATION_CONTAINER_STACK)
                        .enchantable(15)
                        .component(DataComponents.CONSUMABLE, Consumables.DEFAULT_DRINK)
                        .usingConvertsTo(Items.GLASS_BOTTLE)
        );
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
