package com.example.realismsurvival.mixin.fot;

import com.example.realismsurvival.compat.ModCompat;
import com.example.realismsurvival.water.VanillaWaterItems;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * After FoT swaps a thieves-fish bucket for {@code water_bucket} on axolotl use, rewrite to graded.
 */
@Mixin(value = Axolotl.class, priority = 2000)
public abstract class AxolotlBucketMixin {

    @Inject(method = "mobInteract", at = @At("RETURN"))
    private void realismsurvival$replaceWaterBucket(
            Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir
    ) {
        if (!ModCompat.loaded(ModCompat.FISH_OF_THIEVES) || player.level().isClientSide()) {
            return;
        }
        InteractionResult result = cir.getReturnValue();
        if (result == null || !result.consumesAction()) {
            return;
        }
        ItemStack held = player.getItemInHand(hand);
        if (held.is(Items.WATER_BUCKET)) {
            Axolotl self = (Axolotl) (Object) this;
            player.setItemInHand(hand, VanillaWaterItems.gradedBucketAt(player.level(), self.blockPosition()));
        }
    }
}
