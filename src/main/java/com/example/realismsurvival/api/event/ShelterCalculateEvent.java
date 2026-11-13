package com.example.realismsurvival.api.event;

import com.example.realismsurvival.api.environment.EnvironmentContext;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Fired while building {@link EnvironmentContext}. Structure / tent / vehicle addons may force
 * shelter or change {@link #shelterFactor}.
 */
public final class ShelterCalculateEvent extends PlayerEvent {

    private final EnvironmentContext environment;

    public ShelterCalculateEvent(ServerPlayer player, EnvironmentContext environment) {
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

    public boolean isSheltered() {
        return environment.sheltered();
    }

    public void setSheltered(boolean sheltered) {
        environment.setSheltered(sheltered);
        if (sheltered && environment.shelterFactor() <= 0.0F) {
            environment.setShelterFactor(1.0F);
        }
    }

    public float getShelterFactor() {
        return environment.shelterFactor();
    }

    public void setShelterFactor(float factor) {
        environment.setShelterFactor(factor);
        environment.setSheltered(factor > 0.0F);
    }
}
