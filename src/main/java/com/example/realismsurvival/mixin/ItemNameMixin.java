package com.example.realismsurvival.mixin;

import com.example.realismsurvival.water.WetSpongeItems;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Wet sponge display name follows stored water QUALITY. */
@Mixin(Item.class)
public abstract class ItemNameMixin {

    @Inject(method = "getName", at = @At("HEAD"), cancellable = true)
    private void realismsurvival$wetSpongeName(ItemStack stack, CallbackInfoReturnable<Component> cir) {
        if (stack.is(Items.WET_SPONGE)) {
            cir.setReturnValue(WetSpongeItems.displayName(stack));
        }
    }
}
