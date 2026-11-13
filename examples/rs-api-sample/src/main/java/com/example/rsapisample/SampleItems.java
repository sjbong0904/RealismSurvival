package com.example.rsapisample;

import com.example.realismsurvival.api.water.GradedWaterItems;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class SampleItems {

    private SampleItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(RsApiSample.MODID);

    public static final DeferredItem<Item> MINERAL_WATER_BUCKET = ITEMS.registerItem(
            "mineral_water_bucket",
            props -> GradedWaterItems.createBucket(SampleWaterQualities.MINERAL, props),
            GradedWaterItems::bucketProperties
    );

    public static final DeferredItem<Item> MINERAL_WATER_BOTTLE = ITEMS.registerItem(
            "mineral_water_bottle",
            props -> GradedWaterItems.createBottle(SampleWaterQualities.MINERAL, props),
            GradedWaterItems::bottleProperties
    );

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
