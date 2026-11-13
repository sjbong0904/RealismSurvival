package com.example.realismsurvival.api.event;

import com.example.realismsurvival.api.environment.EnvironmentContext;
import com.example.realismsurvival.component.SurvivalData;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Fired on the logical server after ambient °C is computed and before the °C/hour body-temp step.
 * Addons may adjust {@link #ambient} and {@link #insulation} (0 = none, 1 = full isolation).
 *
 * <p>Breakdown fields (altitude / day-night / wet / heat / cold / season) are informational defaults
 * from the core evaluator — useful for climate addons that want to replace one term without
 * recomputing everything.
 *
 * <p>Not cancellable — disable systems via config instead.
 */
public final class TemperatureCalculateEvent extends PlayerEvent {

    private final ServerLevel level;
    private final Holder<Biome> biome;
    private final SurvivalData currentData;
    private final EnvironmentContext environment;
    private final float altitudeOffset;
    private final float dayNightOffset;
    private final float wetOffset;
    private final float heatBonus;
    private final float coldPenalty;
    private final float seasonOffset;
    private float ambient;
    private float insulation;

    public TemperatureCalculateEvent(
            ServerPlayer player,
            ServerLevel level,
            Holder<Biome> biome,
            SurvivalData currentData,
            EnvironmentContext environment,
            float ambient,
            float insulation,
            float altitudeOffset,
            float dayNightOffset,
            float wetOffset,
            float heatBonus,
            float coldPenalty,
            float seasonOffset
    ) {
        super(player);
        this.level = level;
        this.biome = biome;
        this.currentData = currentData;
        this.environment = environment;
        this.ambient = ambient;
        this.insulation = insulation;
        this.altitudeOffset = altitudeOffset;
        this.dayNightOffset = dayNightOffset;
        this.wetOffset = wetOffset;
        this.heatBonus = heatBonus;
        this.coldPenalty = coldPenalty;
        this.seasonOffset = seasonOffset;
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

    public EnvironmentContext getEnvironment() {
        return environment;
    }

    public float getAmbient() {
        return ambient;
    }

    public void setAmbient(float ambient) {
        this.ambient = ambient;
    }

    /** Add to ambient (°C). Climate / weather addons use this for season or storm offsets. */
    public void addAmbient(float deltaCelsius) {
        this.ambient += deltaCelsius;
    }

    public float getInsulation() {
        return insulation;
    }

    /** Clamped to {@code [0, 1]} when applied by the evaluator. */
    public void setInsulation(float insulation) {
        this.insulation = insulation;
    }

    public void addInsulation(float delta) {
        this.insulation += delta;
    }

    public float getAltitudeOffset() {
        return altitudeOffset;
    }

    public float getDayNightOffset() {
        return dayNightOffset;
    }

    public float getWetOffset() {
        return wetOffset;
    }

    public float getHeatBonus() {
        return heatBonus;
    }

    /** Nearby cold term from the core evaluator (≤ 0). */
    public float getColdPenalty() {
        return coldPenalty;
    }

    /** Climate-zone × season °C term from the core evaluator. */
    public float getSeasonOffset() {
        return seasonOffset;
    }
}
