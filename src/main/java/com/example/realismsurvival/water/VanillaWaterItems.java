package com.example.realismsurvival.water;

import com.example.realismsurvival.api.water.WaterApi;
import com.example.realismsurvival.api.water.WaterQuality;
import com.example.realismsurvival.registry.ModWaterQualities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.material.FluidState;

/**
 * Rewrites leftover vanilla water stacks (bucket / water potion) into graded items.
 */
public final class VanillaWaterItems {

    private VanillaWaterItems() {}

    public static boolean isVanillaWaterBucket(ItemStack stack) {
        return stack.is(Items.WATER_BUCKET);
    }

    public static boolean isVanillaWaterPotion(ItemStack stack) {
        if (!stack.is(Items.POTION)) {
            return false;
        }
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        return contents != null && contents.is(Potions.WATER);
    }

    /** Biome / fluid grade at {@code pos}; used for mob-bucket leftovers and shoal scoop. */
    public static WaterQuality qualityAt(LevelReader level, BlockPos pos) {
        FluidState fluid = level.getFluidState(pos);
        WaterQuality graded = WaterApi.fromFluidState(fluid);
        if (graded != null) {
            return graded;
        }
        return WaterApi.resolveFromBiome(level.getBiome(pos));
    }

    public static ItemStack gradedBucketAt(LevelReader level, BlockPos pos) {
        return qualityAt(level, pos).createBucket();
    }

    public static ItemStack replaceWaterBucket(ItemStack stack, LevelReader level, BlockPos pos) {
        if (!isVanillaWaterBucket(stack)) {
            return stack;
        }
        ItemStack graded = gradedBucketAt(level, pos);
        graded.setCount(stack.getCount());
        return graded;
    }

    /** Worldgen / loot consumables → distilled. */
    public static ItemStack distilledBucket() {
        return ModWaterQualities.DISTILLED.get().createBucket();
    }

    public static ItemStack distilledBottle() {
        return ModWaterQualities.DISTILLED.get().createBottle();
    }

    public static ItemStack replaceWaterPotionWithDistilled(ItemStack stack) {
        if (!isVanillaWaterPotion(stack)) {
            return stack;
        }
        ItemStack graded = distilledBottle();
        graded.setCount(stack.getCount());
        return graded;
    }
}
