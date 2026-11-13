package com.example.realismsurvival.mixin;

import com.example.realismsurvival.water.WaterHydration;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.FarmlandBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FarmlandBlock.class)
public abstract class FarmlandBlockMixin {

    @Inject(method = "isNearWater", at = @At("HEAD"), cancellable = true)
    private static void realismsurvival$gradedIrrigation(
            LevelReader level,
            BlockPos pos,
            CallbackInfoReturnable<Boolean> cir
    ) {
        cir.setReturnValue(WaterHydration.isNearHydratingWater(level, pos));
    }
}
