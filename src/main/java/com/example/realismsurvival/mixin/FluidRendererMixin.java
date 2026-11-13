package com.example.realismsurvival.mixin;

import com.example.realismsurvival.water.WaterSurfaceCompat;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.FluidRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Graded water types are distinct Fluids (scoop identity), so vanilla treats them as separate
 * liquids and draws vertical seams + height faults at grade boundaries. For rendering only,
 * treat all {@code #minecraft:water} as one continuous surface.
 */
@Mixin(FluidRenderer.class)
public abstract class FluidRendererMixin {

    @Inject(method = "isNeighborSameFluid", at = @At("HEAD"), cancellable = true)
    private static void realismsurvival$connectWaterGrades(
            FluidState fluidState,
            FluidState neighborFluidState,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (WaterSurfaceCompat.connects(fluidState, neighborFluidState)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(
            method = "shouldRenderFace(Lnet/minecraft/world/level/material/FluidState;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;Lnet/minecraft/world/level/block/state/BlockState;)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void realismsurvival$hideWaterGradeSeams(
            FluidState fluidState,
            BlockState selfState,
            Direction direction,
            BlockState otherState,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (WaterSurfaceCompat.connects(fluidState, otherState.getFluidState())) {
            cir.setReturnValue(false);
        }
    }

    @Inject(
            method = "getHeight(Lnet/minecraft/client/renderer/block/BlockAndTintGetter;Lnet/minecraft/world/level/material/Fluid;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/material/FluidState;)F",
            at = @At("HEAD"),
            cancellable = true
    )
    private void realismsurvival$blendWaterHeights(
            BlockAndTintGetter level,
            Fluid fluidType,
            BlockPos pos,
            BlockState state,
            FluidState fluidState,
            CallbackInfoReturnable<Float> cir
    ) {
        if (!WaterSurfaceCompat.connects(fluidType, fluidState)) {
            return;
        }
        FluidState above = level.getBlockState(pos.above()).getFluidState();
        cir.setReturnValue(WaterSurfaceCompat.isWaterSurface(above) ? 1.0F : fluidState.getOwnHeight());
    }
}
