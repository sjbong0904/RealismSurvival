package com.example.realismsurvival.mixin;

import com.example.realismsurvival.api.water.WaterApi;
import com.example.realismsurvival.water.GradedIceBlock;
import com.example.realismsurvival.water.VanillaWaterConverter;
import com.example.realismsurvival.water.WaterQualityStates;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Runtime precipitation freeze → graded ice with the fluid's QUALITY. */
@Mixin(ServerLevel.class)
public abstract class ServerLevelFreezeMixin {

    @WrapOperation(
            method = "tickPrecipitation",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerLevel;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"
            )
    )
    private boolean realismsurvival$placeGradedIce(
            ServerLevel level,
            BlockPos pos,
            BlockState state,
            Operation<Boolean> original
    ) {
        if (!state.is(Blocks.ICE)) {
            return original.call(level, pos, state);
        }
        FluidState fluid = level.getFluidState(pos);
        BlockState ice;
        if (WaterApi.isGradedWaterFluid(fluid.getType())) {
            ice = GradedIceBlock.fromFluidState(fluid);
        } else {
            var biome = level.getBiome(pos);
            var quality = VanillaWaterConverter.classifyNaturalWater(biome, WaterApi.resolveFromBiome(biome));
            ice = GradedIceBlock.withQualityId(WaterQualityStates.idOf(quality));
        }
        return original.call(level, pos, ice);
    }
}
