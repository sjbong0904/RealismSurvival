package com.example.realismsurvival.water;

import com.example.realismsurvival.api.water.WaterApi;
import com.example.realismsurvival.api.water.WaterQuality;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import org.jspecify.annotations.Nullable;

/**
 * Liquid block for the shared graded-water fluid. Stores {@link WaterQualityStates#QUALITY} on the
 * block state (vanilla {@link LiquidBlock} only caches LEVEL → fluid must be rebuilt here).
 */
public class GradedLiquidBlock extends LiquidBlock {

    private final int defaultQualityId;
    private final FlowingFluid gradedFluid;

    public GradedLiquidBlock(FlowingFluid fluid, Properties properties) {
        this(fluid, properties, 0);
    }

    public GradedLiquidBlock(FlowingFluid fluid, Properties properties, int defaultQualityId) {
        super(fluid, properties);
        this.gradedFluid = fluid;
        this.defaultQualityId = WaterQualityStates.clampId(defaultQualityId);
        registerDefaultState(defaultBlockState()
                .setValue(LEVEL, 0)
                .setValue(WaterQualityStates.QUALITY, this.defaultQualityId));
    }

    public GradedLiquidBlock(FlowingFluid fluid, Properties properties, WaterQuality defaultQuality) {
        this(fluid, properties, WaterQualityStates.idOf(defaultQuality));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(LEVEL, WaterQualityStates.QUALITY);
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        // LiquidBlock.LEVEL is 0–15; fluid LEVEL is only 1–8. Vanilla clamps via stateCache —
        // without that, LEVEL≥9 calls getFlowing(negative) and crashes registry bake.
        int level = Math.min(state.getValue(LEVEL), 8);
        int qualityId = state.getValue(WaterQualityStates.QUALITY);
        FluidState fluidState;
        if (level == 0) {
            fluidState = gradedFluid.getSource(false);
        } else if (level == 8) {
            fluidState = gradedFluid.getFlowing(8, true);
        } else {
            fluidState = gradedFluid.getFlowing(8 - level, false);
        }
        return WaterQualityStates.withQualityId(fluidState, qualityId);
    }

    @Override
    public ItemStack pickupBlock(@Nullable LivingEntity user, LevelAccessor level, BlockPos pos, BlockState state) {
        if (state.getValue(LEVEL) != 0) {
            return ItemStack.EMPTY;
        }
        WaterQuality quality = WaterApi.fromFluidState(state.getFluidState());
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 11);
        if (quality != null) {
            return quality.createBucket();
        }
        return super.pickupBlock(user, level, pos, state);
    }

    @Override
    public boolean shouldHideAdjacentFluidFace(BlockState state, Direction selfFace, FluidState adjacentFluid) {
        return WaterSurfaceCompat.connects(state.getFluidState(), adjacentFluid)
                || super.shouldHideAdjacentFluidFace(state, selfFace, adjacentFluid);
    }
}
