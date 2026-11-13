package com.example.realismsurvival.mixin;

import com.example.realismsurvival.water.WaterSurfaceCompat;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.common.extensions.IBlockExtension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * {@code shouldHideAdjacentFluidFace} lives on NeoForge's {@link IBlockExtension} (default method),
 * not on {@code BlockState}. Sodium calls this via {@code shouldOccludeFluid}.
 */
@Mixin(IBlockExtension.class)
public interface BlockFluidFaceMixin {

    @Inject(method = "shouldHideAdjacentFluidFace", at = @At("HEAD"), cancellable = true)
    private void realismsurvival$hideWaterGradeFaces(
            BlockState state,
            Direction selfFace,
            FluidState adjacentFluid,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (WaterSurfaceCompat.connects(state.getFluidState(), adjacentFluid)) {
            cir.setReturnValue(true);
        }
    }
}
