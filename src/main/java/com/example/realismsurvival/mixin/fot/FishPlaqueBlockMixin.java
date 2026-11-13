package com.example.realismsurvival.mixin.fot;

import com.example.realismsurvival.water.VanillaWaterItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** FoT fish plaque returns a graded water bucket instead of {@code minecraft:water_bucket}. */
@Mixin(targets = "com.stevekung.fishofthieves.block.FishPlaqueBlock")
public abstract class FishPlaqueBlockMixin {

    @Inject(method = "useItemOn", at = @At("RETURN"))
    private void realismsurvival$replaceWaterBucket(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit,
            CallbackInfoReturnable<InteractionResult> cir
    ) {
        if (level.isClientSide()) {
            return;
        }
        ItemStack held = player.getItemInHand(hand);
        if (held.is(Items.WATER_BUCKET)) {
            player.setItemInHand(hand, VanillaWaterItems.gradedBucketAt(level, pos));
        }
    }
}
