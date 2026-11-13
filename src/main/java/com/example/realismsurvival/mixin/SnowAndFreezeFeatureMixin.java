package com.example.realismsurvival.mixin;

import com.example.realismsurvival.api.water.WaterApi;
import com.example.realismsurvival.water.GradedIceBlock;
import com.example.realismsurvival.water.VanillaWaterConverter;
import com.example.realismsurvival.water.WaterQualityStates;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.SnowAndFreezeFeature;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Worldgen freeze_top_layer → graded ice (biome / fluid QUALITY). */
@Mixin(SnowAndFreezeFeature.class)
public abstract class SnowAndFreezeFeatureMixin {

    @WrapOperation(
            method = "place",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/WorldGenLevel;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"
            )
    )
    private boolean realismsurvival$placeGradedIce(
            WorldGenLevel level,
            BlockPos pos,
            BlockState state,
            int flags,
            Operation<Boolean> original
    ) {
        if (!state.is(Blocks.ICE)) {
            return original.call(level, pos, state, flags);
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
        return original.call(level, pos, ice, flags);
    }
}