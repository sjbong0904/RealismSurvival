package com.example.realismsurvival.mixin;

import com.example.realismsurvival.water.CoralSaltWater;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.CoralBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Live coral blocks survive next to salt water only. */
@Mixin(CoralBlock.class)
public abstract class CoralBlockMixin {

    @Inject(method = "scanForWater", at = @At("HEAD"), cancellable = true)
    private void realismsurvival$saltOnly(BlockGetter level, BlockPos blockPos, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(CoralSaltWater.canSustain(level.getBlockState(blockPos), level, blockPos));
    }
}
