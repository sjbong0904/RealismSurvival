package com.example.realismsurvival.network;

import com.example.realismsurvival.RealismSurvival;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

/**
 * Pushes datapack balancing tables to joining / reloading players (T-09 / spec §7.6).
 */
@EventBusSubscriber(modid = RealismSurvival.MODID)
public final class SurvivalDataSync {

    private SurvivalDataSync() {}

    @SubscribeEvent
    static void onDatapackSync(OnDatapackSyncEvent event) {
        SyncSurvivalDataPayload payload = SyncSurvivalDataPayload.fromServer();
        event.getRelevantPlayers().forEach(player -> {
            // Mock / incomplete connections (GameTest) never negotiate play payloads — skip them.
            if (!NetworkRegistry.hasChannel(player.connection, SyncSurvivalDataPayload.TYPE.id())) {
                return;
            }
            PacketDistributor.sendToPlayer(player, payload);
        });
    }
}
