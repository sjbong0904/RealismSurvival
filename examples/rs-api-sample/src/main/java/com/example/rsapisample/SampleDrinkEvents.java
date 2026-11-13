package com.example.rsapisample;

import com.example.realismsurvival.api.event.PlayerDrinkEvent;
import com.example.realismsurvival.api.water.WaterApi;
import net.neoforged.bus.api.SubscribeEvent;

/** Demonstrates cancellable drink mutation on the public event bus. */
public final class SampleDrinkEvents {

    private SampleDrinkEvents() {}

    @SubscribeEvent
    public static void onDrink(PlayerDrinkEvent event) {
        if (event.getQuality() == null) {
            return;
        }
        if (event.getQuality() != SampleWaterQualities.MINERAL.get()) {
            return;
        }
        // Small bonus on top of datapack thirst_restore — proves event mutation.
        event.setThirstRestore(event.getThirstRestore() + 2.0F);
        if (WaterApi.fromBottleItem(event.getConsumed().getItem()) == SampleWaterQualities.MINERAL.get()) {
            RsApiSample.LOGGER.debug("[rs-api-sample] Mineral bottle drink +2 thirst bonus");
        }
    }
}
