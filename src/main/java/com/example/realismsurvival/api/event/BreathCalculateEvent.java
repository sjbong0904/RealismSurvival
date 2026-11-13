package com.example.realismsurvival.api.event;

import com.example.realismsurvival.api.environment.EnvironmentContext;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Fired while building {@link EnvironmentContext} for head-in-fluid / atmosphere addons
 * (gills, gas masks, space suits, toxic air). Core does not apply oxygen damage here —
 * addons listen and act (or cancel vanilla drowning elsewhere).
 */
public final class BreathCalculateEvent extends PlayerEvent {

    private final EnvironmentContext environment;

    public BreathCalculateEvent(ServerPlayer player, EnvironmentContext environment) {
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

    public boolean isSubmerged() {
        return environment.submerged();
    }

    public Fluid getSubmergedFluid() {
        return environment.submergedFluid();
    }

    public boolean canBreathe() {
        return environment.canBreathe();
    }

    public void setCanBreathe(boolean canBreathe) {
        environment.setCanBreathe(canBreathe);
    }

    public float getAirSupplyRatio() {
        return environment.airSupplyRatio();
    }

    public void setAirSupplyRatio(float ratio) {
        environment.setAirSupplyRatio(ratio);
    }
}
