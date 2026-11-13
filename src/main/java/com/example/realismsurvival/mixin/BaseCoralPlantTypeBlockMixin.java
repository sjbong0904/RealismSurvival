package com.example.realismsurvival.mixin;

import com.example.realismsurvival.water.CoralSaltWater;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseCoralPlantTypeBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Coral plants/fans: salt only. Waterlogged cells use biome grade (vanilla fluid is always
 * {@code Fluids.WATER}).
 */
@Mixin(BaseCoralPlantTypeBlock.class)
public abstract class BaseCoralPlantTypeBlockMixin {

    @Inject(method = "scanForWater", at = @At("HEAD"), cancellable = true)
    private static void realismsurvival$saltOnly(
            BlockState state,
            BlockGetter level,
            BlockPos blockPos,
            CallbackInfoReturnable<Boolean> cir
    ) {
        cir.setReturnValue(CoralSaltWater.canSustain(state, level, blockPos));
    }
}
