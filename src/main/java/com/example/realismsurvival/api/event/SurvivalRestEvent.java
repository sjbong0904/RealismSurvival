package com.example.realismsurvival.api.event;

import com.example.realismsurvival.component.SurvivalData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Fired when a player finishes sleeping (wake-up). Rest / bed / tent / spa addons apply
 * recovery here via {@code SurvivalApi}.
 */
public final class SurvivalRestEvent extends PlayerEvent {

    private final ServerLevel level;
    private final SurvivalData data;
    private final boolean wakeImmediately;

    public SurvivalRestEvent(ServerPlayer player, ServerLevel level, SurvivalData data, boolean wakeImmediately) {
        super(player);
        this.level = level;
        this.data = data;
        this.wakeImmediately = wakeImmediately;
    }

    @Override
    public ServerPlayer getEntity() {
        return (ServerPlayer) super.getEntity();
    }

    public ServerLevel getLevel() {
        return level;
    }

    public SurvivalData getData() {
        return data;
    }

    /** Vanilla wake-up flag from {@code PlayerWakeUpEvent}. */
    public boolean wakeImmediately() {
        return wakeImmediately;
    }
}
