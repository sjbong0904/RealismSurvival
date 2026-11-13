package com.example.realismsurvival.mixin;

import com.example.realismsurvival.water.SpongeAbsorb;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SpongeBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Replace sponge absorb so wet sponge keeps absorbed water {@code QUALITY}. */
@Mixin(SpongeBlock.class)
public abstract class SpongeBlockMixin {

    @Inject(method = "tryAbsorbWater", at = @At("HEAD"), cancellable = true)
    private void realismsurvival$absorbWithQuality(Level level, BlockPos pos, CallbackInfo ci) {
        SpongeAbsorb.tryAbsorb(level, pos);
        ci.cancel();
    }
}
