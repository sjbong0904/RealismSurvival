package com.example.realismsurvival.mixin;

import com.example.realismsurvival.api.water.WaterApi;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Vanilla {@link Biome#shouldFreeze} only matches {@link Fluids#WATER}. Graded water is a
 * different fluid type, so cold biomes would never freeze after worldgen conversion.
 *
 * <p>{@link FluidState#is(Object)} is {@link net.minecraft.core.TypedInstance#is(Object)} —
 * the bytecode descriptor is {@code (Ljava/lang/Object;)Z}, not {@code (LFluid;)Z}.
 */
@Mixin(Biome.class)
public abstract class BiomeFreezeMixin {

    @WrapOperation(
            method = "shouldFreeze(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;Z)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/material/FluidState;is(Ljava/lang/Object;)Z"
            )
    )
    private boolean realismsurvival$freezeGradedWater(
            FluidState state,
            Object fluid,
            Operation<Boolean> original
    ) {
        if (original.call(state, fluid)) {
            return true;
        }
        return fluid == Fluids.WATER && WaterApi.isGradedWaterFluid(state.getType());
    }
}
