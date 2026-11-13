package com.example.realismsurvival.mixin;

import com.example.realismsurvival.registry.ModWaterQualities;
import com.example.realismsurvival.water.CauldronWater;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.CauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Empty cauldron rain / drip → graded water cauldron (rain & ungraded drip = fresh). */
@Mixin(CauldronBlock.class)
public abstract class CauldronBlockMixin {

    @Inject(method = "handlePrecipitation", at = @At("HEAD"), cancellable = true)
    private void realismsurvival$rainFresh(
            BlockState state, Level level, BlockPos pos, Biome.Precipitation precipitation, CallbackInfo ci
    ) {
        if (precipitation != Biome.Precipitation.RAIN) {
            return;
        }
        // Mirror CauldronBlock.shouldHandlePrecipitation rain branch (protected).
        if (level.getRandom().nextFloat() >= 0.05F) {
            ci.cancel();
            return;
        }
        BlockState filled = CauldronWater.levelOne(ModWaterQualities.FRESH.get());
        level.setBlockAndUpdate(pos, filled);
        level.gameEvent(null, GameEvent.BLOCK_CHANGE, pos);
        ci.cancel();
    }

    @Inject(method = "receiveStalactiteDrip", at = @At("HEAD"), cancellable = true)
    private void realismsurvival$dripFill(BlockState state, Level level, BlockPos pos, Fluid fluid, CallbackInfo ci) {
        if (!CauldronWater.isDrippableWater(fluid)) {
            return;
        }
        if (fluid == Fluids.LAVA) {
            return;
        }
        var quality = CauldronWater.dripQuality(level, pos);
        BlockState filled = CauldronWater.levelOne(quality);
        level.setBlockAndUpdate(pos, filled);
        level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(filled));
        level.levelEvent(1047, pos, 0);
        ci.cancel();
    }
}
