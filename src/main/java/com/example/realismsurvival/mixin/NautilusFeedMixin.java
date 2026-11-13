package com.example.realismsurvival.mixin;

import com.example.realismsurvival.water.VanillaWaterItems;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Fish-bucket feed on nautilus leaves a graded water bucket instead of vanilla. */
@Mixin(AbstractNautilus.class)
public abstract class NautilusFeedMixin {

    @Inject(method = "usePlayerItem", at = @At("RETURN"))
    private void realismsurvival$gradedLeftover(Player player, InteractionHand hand, ItemStack stack, CallbackInfo ci) {
        if (player.level().isClientSide()) {
            return;
        }
        ItemStack held = player.getItemInHand(hand);
        if (held.is(Items.WATER_BUCKET)) {
            AbstractNautilus self = (AbstractNautilus) (Object) this;
            player.setItemInHand(hand, VanillaWaterItems.gradedBucketAt(player.level(), self.blockPosition()));
        }
    }
}
