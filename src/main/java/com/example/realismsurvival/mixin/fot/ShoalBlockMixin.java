package com.example.realismsurvival.mixin.fot;

import com.example.realismsurvival.water.VanillaWaterConverter;
import com.example.realismsurvival.water.VanillaWaterItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** FoT shoal scoop / break must not leave vanilla water buckets or open {@code Blocks.WATER}. */
@Mixin(targets = "com.stevekung.fishofthieves.block.ShoalBlock")
public abstract class ShoalBlockMixin {

    @Inject(method = "pickupBlock", at = @At("RETURN"), cancellable = true)
    private void realismsurvival$gradedBucket(
            LivingEntity entity,
            LevelAccessor level,
            BlockPos pos,
            BlockState state,
            CallbackInfoReturnable<ItemStack> cir
    ) {
        cir.setReturnValue(VanillaWaterItems.gradedBucketAt(level, pos));
    }

    @Inject(method = "destroyShoal", at = @At("RETURN"))
    private void realismsurvival$convertWater(
            BlockState state, Level level, BlockPos pos, boolean dropTreasure, CallbackInfo ci
    ) {
        if (level instanceof ServerLevel server && level.getBlockState(pos).is(Blocks.WATER)) {
            VanillaWaterConverter.tryConvert(server, pos);
        }
    }
}
