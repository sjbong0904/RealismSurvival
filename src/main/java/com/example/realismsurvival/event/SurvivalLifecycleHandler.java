package com.example.realismsurvival.event;

import com.example.realismsurvival.RealismSurvival;
import com.example.realismsurvival.RealismSurvivalConfig;
import com.example.realismsurvival.component.SurvivalAttachments;
import com.example.realismsurvival.component.SurvivalData;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Death / End-return respawn policy (T-02).
 *
 * <p>{@code copyOnDeath} still copies the attachment onto the new entity; this handler then optionally
 * overwrites thirst/temperature for a normal death respawn. End conquest never resets.
 */
@EventBusSubscriber(modid = RealismSurvival.MODID)
public final class SurvivalLifecycleHandler {

    private SurvivalLifecycleHandler() {}

    @SubscribeEvent
    static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (event.isEndConquered()) {
            return;
        }
        if (!RealismSurvivalConfig.RESPAWN_RESET_ON_DEATH.getAsBoolean()) {
            return;
        }

        float thirst = (float) RealismSurvivalConfig.RESPAWN_THIRST.getAsDouble();
        float temperature = (float) RealismSurvivalConfig.RESPAWN_TEMPERATURE.getAsDouble();
        SurvivalAttachments.set(player, new SurvivalData(thirst, temperature));
    }
}
