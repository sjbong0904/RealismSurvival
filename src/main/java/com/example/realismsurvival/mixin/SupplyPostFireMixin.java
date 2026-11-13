package com.example.realismsurvival.mixin;

import com.example.realismsurvival.compat.shop.SupplyPostSite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.FireBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Fire does not catch the wooden stall. */
@Mixin(FireBlock.class)
public abstract class SupplyPostFireMixin {

    @Inject(method = "canCatchFire", at = @At("HEAD"), cancellable = true)
    private void realismsurvival$spareSupplyPost(
            BlockGetter level,
            BlockPos pos,
            Direction face,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (SupplyPostSite.contains(level, pos)) {
            cir.setReturnValue(false);
        }
    }
}
