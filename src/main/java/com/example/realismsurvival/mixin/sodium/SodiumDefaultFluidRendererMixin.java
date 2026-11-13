package com.example.realismsurvival.mixin.sodium;

import com.example.realismsurvival.water.WaterSurfaceCompat;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Sodium replaces vanilla {@code FluidRenderer}; without this, grade boundaries keep drawing
 * opaque side faces. Also forces smooth vertex tinting for water-like fluids so grade mid-bands
 * show even when video biome-blend is 0.
 */
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.DefaultFluidRenderer")
public abstract class SodiumDefaultFluidRendererMixin {

    @WrapOperation(
            method = {
                    "isFullBlockFluidSideVisible",
                    "sampleFluidHeight(Lnet/minecraft/client/renderer/block/BlockAndTintGetter;Lnet/minecraft/world/level/material/Fluid;Lnet/minecraft/core/BlockPos;)F"
            },
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/material/Fluid;isSame(Lnet/minecraft/world/level/material/Fluid;)Z"
            )
    )
    private boolean realismsurvival$connectWaterGrades(Fluid instance, Fluid other, Operation<Boolean> original) {
        if (WaterSurfaceCompat.connectsFluids(instance, other)) {
            return true;
        }
        return original.call(instance, other);
    }

    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/caffeinemc/mods/sodium/client/world/LevelSlice;hasBiomeBlend()Z"
            ),
            remap = false
    )
    private boolean realismsurvival$smoothWaterGradeTint(
            LevelSlice slice,
            Operation<Boolean> original,
            @Local(argsOnly = true) FluidState fluidState
    ) {
        if (WaterSurfaceCompat.isWaterSurface(fluidState)) {
            return true;
        }
        return original.call(slice);
    }
}
