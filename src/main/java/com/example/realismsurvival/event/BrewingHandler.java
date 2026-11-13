package com.example.realismsurvival.event;

import com.example.realismsurvival.RealismSurvival;
import com.example.realismsurvival.water.DistilledWaterBrewingRecipe;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;

/** Distilled water bottle ↔ brewing stand (treated as vanilla water potion base). */
@EventBusSubscriber(modid = RealismSurvival.MODID)
public final class BrewingHandler {

    private BrewingHandler() {}

    @SubscribeEvent
    static void register(RegisterBrewingRecipesEvent event) {
        event.getBuilder().addRecipe(DistilledWaterBrewingRecipe.INSTANCE);
    }
}
