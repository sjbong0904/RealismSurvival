package com.example.realismsurvival.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.DriedGhastBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * {@link DriedGhastBlock} hard-codes {@link Fluids#WATER} for waterlogging / placement in water.
 * Accept any {@code #minecraft:water} (graded fluids) so pouring a custom water bucket hydrates it.
 */
@Mixin(DriedGhastBlock.class)
public abstract class DriedGhastBlockMixin {

    @Inject(method = "placeLiquid", at = @At("HEAD"), cancellable = true)
    private void realismsurvival$placeGradedWater(
            LevelAccessor level,
            BlockPos pos,
            BlockState state,
            FluidState fluidState,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!state.getValue(BlockStateProperties.WATERLOGGED) && fluidState.is(FluidTags.WATER)) {
            if (!level.isClientSide()) {
                level.setBlock(pos, state.setValue(BlockStateProperties.WATERLOGGED, true), 3);
                level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
                level.playSound(null, pos, SoundEvents.DRIED_GHAST_PLACE_IN_WATER, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "getStateForPlacement", at = @At("RETURN"), cancellable = true)
    private void realismsurvival$placeInGradedWater(BlockPlaceContext context, CallbackInfoReturnable<BlockState> cir) {
        BlockState placed = cir.getReturnValue();
        if (placed == null || placed.getValue(BlockStateProperties.WATERLOGGED)) {
            return;
        }
        FluidState at = context.getLevel().getFluidState(context.getClickedPos());
        if (at.is(FluidTags.WATER)) {
            cir.setReturnValue(placed.setValue(BlockStateProperties.WATERLOGGED, true));
        }
    }
}
