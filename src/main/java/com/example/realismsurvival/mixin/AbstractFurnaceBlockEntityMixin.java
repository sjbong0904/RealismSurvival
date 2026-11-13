package com.example.realismsurvival.mixin;

import com.example.realismsurvival.api.ModItemTags;
import com.example.realismsurvival.water.BucketFill;
import com.example.realismsurvival.water.WetSpongeItems;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Wet sponge smelting empties into a graded water bucket matching sponge QUALITY.
 * Salt-water bucket evaporation returns the empty bucket (fuel slot if free, else input slot) instead of
 * destroying it, and partial buckets yield salt proportional to fill.
 */
@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class AbstractFurnaceBlockEntityMixin {

    @Inject(method = "burn", at = @At("HEAD"), cancellable = true)
    private static void realismsurvival$gradedSpongeBucket(
            NonNullList<ItemStack> items,
            ItemStack inputItemStack,
            ItemStack result,
            CallbackInfo ci
    ) {
        boolean saltBucket = inputItemStack.is(ModItemTags.STILL_SALT_SOURCE_BUCKETS);
        int resultCount = result.getCount();
        if (saltBucket && BucketFill.supportsFill(inputItemStack)) {
            int fill = Math.clamp(BucketFill.getFill(inputItemStack), 0, BucketFill.MAX);
            resultCount = Math.max(1, Math.round(resultCount * fill / (float) BucketFill.MAX));
        }

        ItemStack resultItemStack = items.get(2);
        if (resultItemStack.isEmpty()) {
            items.set(2, result.copyWithCount(resultCount));
        } else {
            resultItemStack.grow(resultCount);
        }

        if (inputItemStack.is(Items.WET_SPONGE)
                && !items.get(1).isEmpty()
                && items.get(1).is(Items.BUCKET)) {
            items.set(1, BucketFill.createBucket(WetSpongeItems.quality(inputItemStack), BucketFill.MAX));
        }

        if (saltBucket && inputItemStack.getCount() == 1) {
            // Fuel slot buckets are hopper-extractable from below; the input slot is not.
            if (items.get(1).isEmpty()) {
                items.set(1, new ItemStack(Items.BUCKET));
                items.set(0, ItemStack.EMPTY);
            } else {
                items.set(0, new ItemStack(Items.BUCKET));
            }
        } else {
            inputItemStack.shrink(1);
        }
        ci.cancel();
    }
}
