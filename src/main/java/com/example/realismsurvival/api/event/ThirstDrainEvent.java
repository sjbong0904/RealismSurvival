package com.example.realismsurvival.api.event;

import com.example.realismsurvival.component.SurvivalData;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Fired on the logical server after the core computes thirst drain for one evaluation window,
 * before thirst is subtracted. Cancel to skip drain for this window (dehydration damage is also skipped).
 *
 * <p>Mutate {@link #drain} (points to remove this window; already scaled by {@link #windowSeconds}).
 */
public final class ThirstDrainEvent extends PlayerEvent implements ICancellableEvent {

    private final ServerLevel level;
    private final Holder<Biome> biome;
    private final SurvivalData currentData;
    private final float windowSeconds;
    private float drain;

    public ThirstDrainEvent(
            ServerPlayer player,
            ServerLevel level,
            Holder<Biome> biome,
            SurvivalData currentData,
            float windowSeconds,
            float drain
    ) {
        super(player);
        this.level = level;
        this.biome = biome;
        this.currentData = currentData;
        this.windowSeconds = windowSeconds;
        this.drain = drain;
    }

    @Override
    public ServerPlayer getEntity() {
        return (ServerPlayer) super.getEntity();
    }

    public ServerLevel getLevel() {
        return level;
    }

    public Holder<Biome> getBiome() {
        return biome;
    }

    public SurvivalData getCurrentData() {
        return currentData;
    }

    /** Seconds covered by this evaluation tick window. */
    public float getWindowSeconds() {
        return windowSeconds;
    }

    /** Thirst points to remove this window (non-negative after clamp when applied). */
    public float getDrain() {
        return drain;
    }

    public void setDrain(float drain) {
        this.drain = drain;
    }

    /** Convenience: {@code drain *= factor}. */
    public void multiplyDrain(float factor) {
        this.drain *= factor;
    }
}
