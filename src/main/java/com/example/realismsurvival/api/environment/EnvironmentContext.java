package com.example.realismsurvival.api.environment;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.material.Fluid;

/**
 * Snapshot of the player's surroundings for one survival evaluation window.
 * Built by the core, then adjusted through shelter / wetness / breath events before thirst & temperature run.
 */
public final class EnvironmentContext {

    private final ServerLevel level;
    private final Holder<Biome> biome;
    private final BlockPos pos;
    private final ResourceKey<Level> dimension;
    private final boolean raining;
    private final boolean thundering;
    private final int skyLight;
    private final int blockLight;
    private boolean sheltered;
    private float shelterFactor;
    private boolean wet;
    private float wetness;
    private boolean submerged;
    private Fluid submergedFluid;
    private boolean canBreathe;
    private float airSupplyRatio;

    public EnvironmentContext(
            ServerLevel level,
            Holder<Biome> biome,
            BlockPos pos,
            ResourceKey<Level> dimension,
            boolean raining,
            boolean thundering,
            int skyLight,
            int blockLight,
            boolean sheltered,
            float shelterFactor,
            boolean wet,
            float wetness,
            boolean submerged,
            Fluid submergedFluid,
            boolean canBreathe,
            float airSupplyRatio
    ) {
        this.level = level;
        this.biome = biome;
        this.pos = pos.immutable();
        this.dimension = dimension;
        this.raining = raining;
        this.thundering = thundering;
        this.skyLight = skyLight;
        this.blockLight = blockLight;
        this.sheltered = sheltered;
        this.shelterFactor = shelterFactor;
        this.wet = wet;
        this.wetness = wetness;
        this.submerged = submerged;
        this.submergedFluid = submergedFluid;
        this.canBreathe = canBreathe;
        this.airSupplyRatio = airSupplyRatio;
    }

    public ServerLevel level() {
        return level;
    }

    public Holder<Biome> biome() {
        return biome;
    }

    public BlockPos pos() {
        return pos;
    }

    public ResourceKey<Level> dimension() {
        return dimension;
    }

    public boolean raining() {
        return raining;
    }

    public boolean thundering() {
        return thundering;
    }

    public int skyLight() {
        return skyLight;
    }

    public int blockLight() {
        return blockLight;
    }

    public boolean sheltered() {
        return sheltered;
    }

    public void setSheltered(boolean sheltered) {
        this.sheltered = sheltered;
    }

    /** 0 = fully exposed, 1 = fully sheltered. */
    public float shelterFactor() {
        return shelterFactor;
    }

    public void setShelterFactor(float shelterFactor) {
        this.shelterFactor = Math.clamp(shelterFactor, 0.0F, 1.0F);
    }

    public boolean wet() {
        return wet;
    }

    public void setWet(boolean wet) {
        this.wet = wet;
    }

    /** 0 = dry, 1 = soaked. */
    public float wetness() {
        return wetness;
    }

    public void setWetness(float wetness) {
        this.wetness = Math.clamp(wetness, 0.0F, 1.0F);
        this.wet = this.wetness > 0.0F;
    }

    public boolean submerged() {
        return submerged;
    }

    public void setSubmerged(boolean submerged) {
        this.submerged = submerged;
    }

    public Fluid submergedFluid() {
        return submergedFluid;
    }

    public void setSubmergedFluid(Fluid submergedFluid) {
        this.submergedFluid = submergedFluid;
    }

    /** When false, breath/oxygen addons may apply penalties; core does not drown here. */
    public boolean canBreathe() {
        return canBreathe;
    }

    public void setCanBreathe(boolean canBreathe) {
        this.canBreathe = canBreathe;
    }

    /** Approximate remaining air 0–1 ({@code getAirSupply / max}). */
    public float airSupplyRatio() {
        return airSupplyRatio;
    }

    public void setAirSupplyRatio(float airSupplyRatio) {
        this.airSupplyRatio = Math.clamp(airSupplyRatio, 0.0F, 1.0F);
    }
}
