package com.example.realismsurvival.water;

import com.example.realismsurvival.api.water.WaterApi;
import com.example.realismsurvival.api.water.WaterQuality;
import com.example.realismsurvival.registry.ModWaterQualities;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

/**
 * {@link WaterQualityStates#QUALITY} on {@link Blocks#WATER_CAULDRON}.
 *
 * <p>Default / worldgen = distilled. Rain and ungraded dripstone = fresh. Scoop returns the
 * cauldron's grade. Mixing grades is rejected until dilution exists.
 */
public final class CauldronWater {

    /** Distilled registry id — safe before the quality registry binds (same pattern as wet sponge). */
    public static final int DEFAULT_QUALITY_ID = 0;

    private CauldronWater() {}

    public static boolean isWaterCauldron(BlockState state) {
        return state.is(Blocks.WATER_CAULDRON);
    }

    public static int qualityId(BlockState state) {
        if (state.hasProperty(WaterQualityStates.QUALITY)) {
            return WaterQualityStates.clampId(state.getValue(WaterQualityStates.QUALITY));
        }
        return DEFAULT_QUALITY_ID;
    }

    public static WaterQuality quality(BlockState state) {
        return WaterQualityStates.byId(qualityId(state));
    }

    public static BlockState withQuality(BlockState state, WaterQuality quality) {
        return withQualityId(state, WaterQualityStates.idOf(quality));
    }

    public static BlockState withQualityId(BlockState state, int qualityId) {
        if (!state.hasProperty(WaterQualityStates.QUALITY)) {
            return state;
        }
        return state.setValue(WaterQualityStates.QUALITY, WaterQualityStates.clampId(qualityId));
    }

    public static BlockState waterCauldron(int level, WaterQuality quality) {
        int clamped = Math.max(LayeredCauldronBlock.MIN_FILL_LEVEL, Math.min(LayeredCauldronBlock.MAX_FILL_LEVEL, level));
        return withQuality(
                Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, clamped),
                quality
        );
    }

    public static BlockState full(WaterQuality quality) {
        return waterCauldron(LayeredCauldronBlock.MAX_FILL_LEVEL, quality);
    }

    public static BlockState levelOne(WaterQuality quality) {
        return waterCauldron(LayeredCauldronBlock.MIN_FILL_LEVEL, quality);
    }

    /** True when {@code incoming} may fill or raise this water cauldron. */
    public static boolean accepts(BlockState cauldron, WaterQuality incoming) {
        if (!isWaterCauldron(cauldron)) {
            return true;
        }
        return quality(cauldron) == incoming;
    }

    public static ItemStack createBucket(BlockState cauldron) {
        return quality(cauldron).createBucket();
    }

    public static ItemStack createBottle(BlockState cauldron) {
        return quality(cauldron).createBottle();
    }

    /**
     * Dripstone fill grade: graded source fluid's QUALITY; mud / vanilla / ungraded water → fresh.
     */
    public static WaterQuality dripQuality(Level level, BlockPos cauldronPos) {
        BlockPos tip = PointedDripstoneBlock.findStalactiteTipAboveCauldron(level, cauldronPos);
        if (tip == null) {
            return ModWaterQualities.FRESH.get();
        }
        BlockPos fluidPos = fluidSourceAboveStalactite(level, tip);
        BlockState aboveState = level.getBlockState(fluidPos);
        if (aboveState.is(Blocks.MUD)) {
            return ModWaterQualities.FRESH.get();
        }
        FluidState fluidState = level.getFluidState(fluidPos);
        WaterQuality graded = WaterApi.fromFluidState(fluidState);
        if (graded != null) {
            return graded;
        }
        return ModWaterQualities.FRESH.get();
    }

    /** Walk up the stalactite from tip to root, then one above (vanilla drip source cell). */
    private static BlockPos fluidSourceAboveStalactite(Level level, BlockPos tip) {
        BlockPos root = tip;
        for (int i = 0; i < 11; i++) {
            BlockPos above = root.above();
            if (!(level.getBlockState(above).getBlock() instanceof PointedDripstoneBlock)) {
                break;
            }
            root = above;
        }
        return root.above();
    }

    public static boolean isDrippableWater(Fluid fluid) {
        return fluid == Fluids.WATER || WaterApi.isGradedWaterFluid(fluid);
    }

    public static boolean isDrippableWater(FluidState state) {
        return !state.isEmpty() && (state.is(FluidTags.WATER) || WaterApi.isGradedWaterFluid(state.getType()));
    }
}
