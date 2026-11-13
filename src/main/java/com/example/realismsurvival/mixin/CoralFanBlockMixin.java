package com.example.realismsurvival.mixin;

import com.example.realismsurvival.water.CoralDeath;
import com.example.realismsurvival.water.CoralSaltWater;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CoralFanBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CoralFanBlock.class)
public abstract class CoralFanBlockMixin {

    @Shadow
    @Final
    private Block deadBlock;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void realismsurvival$dieKeepWater(
            BlockState state,
            ServerLevel level,
            BlockPos pos,
            RandomSource random,
            CallbackInfo ci
    ) {
        if (!CoralSaltWater.canSustain(state, level, pos)) {
            CoralDeath.replaceWithDead(level, pos, state, this.deadBlock);
        }
        ci.cancel();
    }
}
