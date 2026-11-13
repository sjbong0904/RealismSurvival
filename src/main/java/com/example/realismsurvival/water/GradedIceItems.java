package com.example.realismsurvival.water;

import com.example.realismsurvival.api.water.WaterQuality;
import com.example.realismsurvival.registry.ModBlocks;
import com.example.realismsurvival.registry.ModWaterQualities;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import org.jspecify.annotations.Nullable;

/** QUALITY on graded-ice item stacks ({@link DataComponents#BLOCK_STATE}). */
public final class GradedIceItems {

    private GradedIceItems() {}

    public static boolean isGradedIce(ItemStack stack) {
        return stack.is(ModBlocks.GRADED_ICE.asItem());
    }

    /** One stack per registered quality (creative / JEI listing). */
    public static List<ItemStack> allQualityStacks() {
        List<ItemStack> stacks = new ArrayList<>();
        for (WaterQuality quality : ModWaterQualities.REGISTRY) {
            stacks.add(create(quality));
        }
        return stacks;
    }

    public static ItemStack create(WaterQuality quality) {
        return withQuality(new ItemStack(ModBlocks.GRADED_ICE.asItem()), quality);
    }

    /** Quality id on the stack, or fresh default when unset. */
    public static int qualityId(ItemStack stack) {
        if (!isGradedIce(stack)) {
            return WaterQualityStates.idOf(ModWaterQualities.FRESH.get());
        }
        BlockItemStateProperties props = stack.get(DataComponents.BLOCK_STATE);
        if (props != null) {
            Integer quality = props.get(WaterQualityStates.QUALITY);
            if (quality != null) {
                return WaterQualityStates.clampId(quality);
            }
        }
        return WaterQualityStates.idOf(ModWaterQualities.FRESH.get());
    }

    public static WaterQuality quality(ItemStack stack) {
        return WaterQualityStates.byId(qualityId(stack));
    }

    public static ItemStack withQuality(ItemStack ice, int qualityId) {
        ItemStack copy = ice.copy();
        copy.set(
                DataComponents.BLOCK_STATE,
                BlockItemStateProperties.EMPTY.with(WaterQualityStates.QUALITY, WaterQualityStates.clampId(qualityId))
        );
        return copy;
    }

    public static ItemStack withQuality(ItemStack ice, WaterQuality quality) {
        return withQuality(ice, WaterQualityStates.idOf(quality));
    }

    /** Distilled / fresh / stagnant — not salt. */
    public static boolean isFreshwaterIceQuality(WaterQuality quality) {
        return quality != null && quality != ModWaterQualities.SALT.get();
    }

    public static @Nullable WaterQuality freshwaterQualityOrNull(ItemStack ice) {
        if (!isGradedIce(ice)) {
            return null;
        }
        WaterQuality quality = quality(ice);
        return isFreshwaterIceQuality(quality) ? quality : null;
    }
}
