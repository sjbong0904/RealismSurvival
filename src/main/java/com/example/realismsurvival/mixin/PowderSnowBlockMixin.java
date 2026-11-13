package com.example.realismsurvival.mixin;

import com.example.realismsurvival.water.BucketFill;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.PowderSnowBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Scooping powder snow yields the mod snow bucket (3-fill, drinkable). */
@Mixin(PowderSnowBlock.class)
public class PowderSnowBlockMixin {

    @Inject(method = "pickupBlock", at = @At("RETURN"), cancellable = true)
    private void realismsurvival$modSnowBucket(
            @Nullable LivingEntity user,
            LevelAccessor level,
            BlockPos pos,
            BlockState state,
            CallbackInfoReturnable<ItemStack> cir
    ) {
        ItemStack result = cir.getReturnValue();
        if (result != null && result.is(Items.POWDER_SNOW_BUCKET)) {
            cir.setReturnValue(BucketFill.createSnowBucket(BucketFill.MAX));
        }
    }
}
