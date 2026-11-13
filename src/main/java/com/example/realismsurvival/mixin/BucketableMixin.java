package com.example.realismsurvival.mixin;

import com.example.realismsurvival.api.water.WaterApi;
import com.example.realismsurvival.water.BucketFill;
import net.minecraft.world.entity.animal.Bucketable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Fish / axolotl / tadpole capture accepts empty buckets and full graded water buckets,
 * not only {@code minecraft:water_bucket}.
 */
@Mixin(Bucketable.class)
public interface BucketableMixin {

    @Redirect(
            method = "bucketMobPickup",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;getItem()Lnet/minecraft/world/item/Item;"
            )
    )
    private static Item realismsurvival$captureBucketItem(ItemStack stack) {
        if (isCaptureBucket(stack)) {
            // Satisfy `itemStack.getItem() == Items.WATER_BUCKET` in the vanilla static method.
            return Items.WATER_BUCKET;
        }
        return stack.getItem();
    }

    private static boolean isCaptureBucket(ItemStack stack) {
        if (stack.is(Items.BUCKET) || stack.is(Items.WATER_BUCKET)) {
            return true;
        }
        return WaterApi.isGradedWaterBucket(stack.getItem()) && BucketFill.isFull(stack);
    }
}
