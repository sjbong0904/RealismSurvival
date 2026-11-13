package com.example.realismsurvival.mixin;

import com.example.realismsurvival.api.water.WaterApi;
import com.example.realismsurvival.water.GradedIceBlock;
import com.example.realismsurvival.water.VanillaWaterConverter;
import com.example.realismsurvival.water.WaterQualityStates;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.IceBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Vanilla ice / frosted ice melt into {@link Blocks#WATER}. Rewrite to graded water in-place
 * so open vanilla water never remains (spec: no vanilla open water).
 */
@Mixin(IceBlock.class)
public abstract class IceBlockMeltMixin {

    @Inject(method = "melt", at = @At("HEAD"), cancellable = true)
    private void realismsurvival$meltToGraded(BlockState state, Level level, BlockPos pos, CallbackInfo ci) {
        if ((Object) this instanceof GradedIceBlock) {
            return;
        }
        ci.cancel();
        if (level.environmentAttributes().getValue(EnvironmentAttributes.WATER_EVAPORATES, pos)) {
            level.removeBlock(pos, false);
            return;
        }
        BlockState water = gradedWaterAt(level, pos);
        level.setBlockAndUpdate(pos, water);
        level.neighborChanged(pos, water.getBlock(), null);
    }

    @WrapOperation(
            method = "playerDestroy",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"
            )
    )
    private boolean realismsurvival$destroyMeltsToGraded(
            Level level,
            BlockPos pos,
            BlockState state,
            Operation<Boolean> original
    ) {
        if (state.is(Blocks.WATER)) {
            state = gradedWaterAt(level, pos);
        }
        return original.call(level, pos, state);
    }

    private static BlockState gradedWaterAt(Level level, BlockPos pos) {
        Holder<Biome> biome = level.getBiome(pos);
        var quality = VanillaWaterConverter.classifyNaturalWater(biome, WaterApi.resolveFromBiome(biome));
        return com.example.realismsurvival.registry.ModFluids.graded()
                .sourceOfId(WaterQualityStates.idOf(quality), false)
                .createLegacyBlock();
    }
}
