package com.example.realismsurvival.api.event;

import com.example.realismsurvival.api.water.WaterQuality;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/**
 * Fired on the logical server (including worldgen) immediately before vanilla liquid water
 * is rewritten to a graded fluid.
 *
 * <p>Cancel to leave {@code minecraft:water} in place. {@link #setQuality} changes the replacement grade.
 */
public final class ConvertVanillaWaterEvent extends Event implements ICancellableEvent {

    private final BlockPos pos;
    private final Holder<Biome> biome;
    private final FluidState vanillaFluid;
    private WaterQuality quality;

    public ConvertVanillaWaterEvent(
            BlockPos pos,
            Holder<Biome> biome,
            FluidState vanillaFluid,
            WaterQuality quality
    ) {
        this.pos = pos.immutable();
        this.biome = biome;
        this.vanillaFluid = vanillaFluid;
        this.quality = quality;
    }

    public BlockPos getPos() {
        return pos;
    }

    public Holder<Biome> getBiome() {
        return biome;
    }

    public FluidState getVanillaFluid() {
        return vanillaFluid;
    }

    public WaterQuality getQuality() {
        return quality;
    }

    public void setQuality(WaterQuality quality) {
        this.quality = Objects.requireNonNull(quality, "quality");
    }
}
