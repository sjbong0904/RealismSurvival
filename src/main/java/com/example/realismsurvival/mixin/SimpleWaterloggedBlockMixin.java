package com.example.realismsurvival.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseCoralPlantTypeBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Vanilla waterlogging only accepts {@link Fluids#WATER}. Graded waters are in
 * {@code #minecraft:water}; allow them so buckets can fill waterloggable blocks.
 * Stored fluid remains vanilla water (same as {@link SimpleWaterloggedBlock#getFluidState} contract).
 *
 * <p>Coral plants are excluded: absorbing a flowing stream into waterlogging eats the current
 * (looks like a flow reset). They still waterlog via {@code getStateForPlacement} in full water.
 */
@Mixin(SimpleWaterloggedBlock.class)
public interface SimpleWaterloggedBlockMixin {

    @Inject(method = "canPlaceLiquid", at = @At("HEAD"), cancellable = true)
    private void realismsurvival$canPlaceGradedWater(
            @Nullable LivingEntity user,
            BlockGetter level,
            BlockPos pos,
            BlockState state,
            Fluid type,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!(state.getBlock() instanceof BaseCoralPlantTypeBlock) && type.is(FluidTags.WATER)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "placeLiquid", at = @At("HEAD"), cancellable = true)
    private void realismsurvival$placeGradedWater(
            LevelAccessor level,
            BlockPos pos,
            BlockState state,
            FluidState fluidState,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (state.getBlock() instanceof BaseCoralPlantTypeBlock) {
            cir.setReturnValue(false);
            return;
        }
        if (!state.getValue(BlockStateProperties.WATERLOGGED) && fluidState.is(FluidTags.WATER)) {
            if (!level.isClientSide()) {
                level.setBlock(pos, state.setValue(BlockStateProperties.WATERLOGGED, true), 3);
                // Always schedule vanilla water — waterlogged blocks report Fluids.WATER.
                level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
            }
            cir.setReturnValue(true);
        }
    }
}
