package com.example.realismsurvival.mixin;

import com.example.realismsurvival.water.WaterSurfaceCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.WaterFluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Vanilla flowing water only yields downward to non-{@code #minecraft:water} fluids.
 * Graded water is water-like but not the same fluid — allow a <em>source</em> of another
 * water-like fluid to displace flowing vanilla water (not flowing↔flowing fights).
 */
@Mixin(WaterFluid.class)
public abstract class WaterFluidMixin {

    @Inject(method = "canBeReplacedWith", at = @At("HEAD"), cancellable = true)
    private void realismsurvival$displaceFlowingForGradedWater(
            FluidState state,
            BlockGetter level,
            BlockPos pos,
            Fluid fluid,
            Direction direction,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (WaterSurfaceCompat.canDisplaceFlowingWaterLike(state, fluid)) {
            cir.setReturnValue(true);
        }
    }
}
