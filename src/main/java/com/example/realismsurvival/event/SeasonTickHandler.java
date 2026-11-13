package com.example.realismsurvival.event;

import com.example.realismsurvival.RealismSurvival;
import com.example.realismsurvival.environment.SeasonCalendar;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** Keeps {@link SeasonCalendar} overworld cache fresh for precip mixins. */
@EventBusSubscriber(modid = RealismSurvival.MODID)
public final class SeasonTickHandler {

    private SeasonTickHandler() {}

    @SubscribeEvent
    static void onLevelTick(LevelTickEvent.Post event) {
        SeasonCalendar.onLevelTick(event.getLevel());
    }
}
