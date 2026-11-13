package com.example.realismsurvival.api.event;

import com.example.realismsurvival.api.environment.EnvironmentContext;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Fired while building {@link EnvironmentContext}. Rain / pool / humidity addons adjust wetness.
 */
public final class WetnessCalculateEvent extends PlayerEvent {

    private final EnvironmentContext environment;

    public WetnessCalculateEvent(ServerPlayer player, EnvironmentContext environment) {
        super(player);
        this.environment = environment;
    }

    @Override
    public ServerPlayer getEntity() {
        return (ServerPlayer) super.getEntity();
    }

    public EnvironmentContext getEnvironment() {
        return environment;
    }

    public float getWetness() {
        return environment.wetness();
    }

    public void setWetness(float wetness) {
        environment.setWetness(wetness);
    }

    public boolean isWet() {
        return environment.wet();
    }

    public void setWet(boolean wet) {
        environment.setWet(wet);
        if (!wet) {
            environment.setWetness(0.0F);
        } else if (environment.wetness() <= 0.0F) {
            environment.setWetness(1.0F);
        }
    }
}
