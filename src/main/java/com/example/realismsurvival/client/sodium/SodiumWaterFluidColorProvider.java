package com.example.realismsurvival.client.sodium;

import com.example.realismsurvival.client.WaterFluidTint;
import com.example.realismsurvival.water.WaterSurfaceCompat;
import net.caffeinemc.mods.sodium.client.model.quad.blender.BlendedColorProvider;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.material.FluidState;

/**
 * Sodium's default Forge fluid adapter paints one flat colour per cell. Extending
 * {@link BlendedColorProvider} interpolates vertex colours from neighbouring samples
 * (same path as biome grass), so grade boundaries read as a soft band when biome blend &gt; 0.
 */
public final class SodiumWaterFluidColorProvider extends BlendedColorProvider<FluidState> {

    public static final SodiumWaterFluidColorProvider INSTANCE = new SodiumWaterFluidColorProvider();

    private SodiumWaterFluidColorProvider() {}

    @Override
    protected int getColor(LevelSlice slice, FluidState rendered, BlockPos pos) {
        FluidState here = slice.getFluidState(pos);
        if (!WaterSurfaceCompat.isWaterSurface(here)) {
            // Corner sample off the water surface — keep the rendered cell's tint at this pos.
            here = rendered;
        }
        return WaterFluidTint.INSTANCE.colorInWorld(here, here.createLegacyBlock(), slice, pos);
    }
}
