package com.example.realismsurvival.mixin;

import com.example.realismsurvival.water.WaterSurfaceCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.FlowingFluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Graded water and vanilla water / waterlogged cells must act as one surface for flow physics.
 * Otherwise waterlogged coral (always {@code Fluids.WATER}) looks like a solid wall to graded
 * flow — place/die next to a stream and the current appears to reset.
 */
@Mixin(FlowingFluid.class)
public abstract class FlowingFluidMixin {

    @Inject(method = "affectsFlow", at = @At("HEAD"), cancellable = true)
    private void realismsurvival$affectsFlow(FluidState neighbourFluid, CallbackInfoReturnable<Boolean> cir) {
        Fluid self = (Fluid) (Object) this;
        cir.setReturnValue(
                neighbourFluid.isEmpty()
                        || neighbourFluid.getType().isSame(self)
                        || WaterSurfaceCompat.connectsFluids(self, neighbourFluid.getType())
        );
    }

    @Inject(method = "isSolidFace", at = @At("HEAD"), cancellable = true)
    private void realismsurvival$waterSurfaceNotSolid(
            BlockGetter level,
            BlockPos pos,
            Direction direction,
            CallbackInfoReturnable<Boolean> cir
    ) {
        FluidState fluidState = level.getFluidState(pos);
        Fluid self = (Fluid) (Object) this;
        if (!fluidState.isEmpty() && WaterSurfaceCompat.connectsFluids(self, fluidState.getType())) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "hasSameAbove", at = @At("HEAD"), cancellable = true)
    private static void realismsurvival$hasSameAbove(
            FluidState fluidState,
            BlockGetter level,
            BlockPos pos,
            CallbackInfoReturnable<Boolean> cir
    ) {
        FluidState above = level.getFluidState(pos.above());
        if (WaterSurfaceCompat.connects(fluidState, above)) {
            cir.setReturnValue(true);
        }
    }

    /** Vanilla {@code WaterFluid#getNewLiquid} counts graded neighbours as same water. */
    @Redirect(
            method = "getNewLiquid",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/material/Fluid;isSame(Lnet/minecraft/world/level/material/Fluid;)Z"
            )
    )
    private boolean realismsurvival$getNewLiquidSameSurface(Fluid self, Fluid other) {
        return self.isSame(other) || WaterSurfaceCompat.connectsFluids(self, other);
    }
}
