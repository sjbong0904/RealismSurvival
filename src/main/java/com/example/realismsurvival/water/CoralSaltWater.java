package com.example.realismsurvival.water;

import com.example.realismsurvival.api.water.WaterApi;
import com.example.realismsurvival.api.water.WaterQuality;
import com.example.realismsurvival.registry.ModWaterQualities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;

/** Coral survival: salt water only (spec §9.9). */
public final class CoralSaltWater {

    private CoralSaltWater() {}

    public static boolean isSalt(BlockGetter level, BlockPos fluidPos, FluidState fluid) {
        if (fluid.isEmpty()) {
            return false;
        }

        WaterQuality graded = WaterApi.fromFluidState(fluid);
        if (graded != null) {
            return graded == ModWaterQualities.SALT.get();
        }

        if (!fluid.is(FluidTags.WATER)) {
            return false;
        }

        // Vanilla / waterlogged fluid: grade by biome at the fluid cell.
        if (level instanceof LevelReader reader) {
            return WaterApi.resolveFromBiome(reader.getBiome(fluidPos)) == ModWaterQualities.SALT.get();
        }

        return false;
    }

    /**
     * Salt-only replacement for {@code BaseCoralPlantTypeBlock#scanForWater} /
     * {@code CoralBlock#scanForWater}.
     */
    public static boolean canSustain(BlockState state, BlockGetter level, BlockPos blockPos) {
        if (state.hasProperty(BlockStateProperties.WATERLOGGED) && state.getValue(BlockStateProperties.WATERLOGGED)) {
            if (isSalt(level, blockPos, level.getFluidState(blockPos))) {
                return true;
            }
        }

        for (Direction direction : Direction.values()) {
            BlockPos fluidPos = blockPos.relative(direction);
            if (isSalt(level, fluidPos, level.getFluidState(fluidPos))) {
                return true;
            }
        }
        return false;
    }
}
