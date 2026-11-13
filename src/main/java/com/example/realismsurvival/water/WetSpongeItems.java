package com.example.realismsurvival.water;

import com.example.realismsurvival.api.water.WaterQuality;
import com.example.realismsurvival.registry.ModWaterQualities;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * QUALITY on vanilla {@link Items#WET_SPONGE} / {@link Blocks#WET_SPONGE}
 * via {@link DataComponents#BLOCK_STATE} (same pattern as graded ice).
 *
 * <p>Stacks without the component (elder guardian, creative pick of unset state) default to
 * <b>salt</b> — ocean monuments are the vanilla source.
 */
public final class WetSpongeItems {

    /**
     * Built-in salt registry id ({@link ModWaterQualities} order: distilled=0 … salt=3).
     * Hardcoded so block bootstrap can set a default before the quality registry is bound.
     */
    public static final int DEFAULT_QUALITY_ID = 3;

    private WetSpongeItems() {}

    public static boolean isWetSponge(ItemStack stack) {
        return stack.is(Items.WET_SPONGE);
    }

    public static List<ItemStack> allQualityStacks() {
        List<ItemStack> stacks = new ArrayList<>();
        for (WaterQuality quality : ModWaterQualities.REGISTRY) {
            stacks.add(create(quality));
        }
        return stacks;
    }

    public static ItemStack create(WaterQuality quality) {
        return withQuality(new ItemStack(Items.WET_SPONGE), quality);
    }

    public static ItemStack create(int qualityId) {
        return withQuality(new ItemStack(Items.WET_SPONGE), qualityId);
    }

    public static int qualityId(ItemStack stack) {
        if (!isWetSponge(stack)) {
            return DEFAULT_QUALITY_ID;
        }
        BlockItemStateProperties props = stack.get(DataComponents.BLOCK_STATE);
        if (props != null) {
            Integer quality = props.get(WaterQualityStates.QUALITY);
            if (quality != null) {
                return WaterQualityStates.clampId(quality);
            }
        }
        return DEFAULT_QUALITY_ID;
    }

    public static WaterQuality quality(ItemStack stack) {
        return WaterQualityStates.byId(qualityId(stack));
    }

    public static ItemStack withQuality(ItemStack sponge, int qualityId) {
        ItemStack copy = sponge.copy();
        copy.set(
                DataComponents.BLOCK_STATE,
                BlockItemStateProperties.EMPTY.with(WaterQualityStates.QUALITY, WaterQualityStates.clampId(qualityId))
        );
        return copy;
    }

    public static ItemStack withQuality(ItemStack sponge, WaterQuality quality) {
        return withQuality(sponge, WaterQualityStates.idOf(quality));
    }

    public static BlockState blockState(int qualityId) {
        return Blocks.WET_SPONGE.defaultBlockState()
                .setValue(WaterQualityStates.QUALITY, WaterQualityStates.clampId(qualityId));
    }

    public static BlockState blockState(WaterQuality quality) {
        return blockState(WaterQualityStates.idOf(quality));
    }

    public static int qualityId(BlockState state) {
        if (state.hasProperty(WaterQualityStates.QUALITY)) {
            return WaterQualityStates.clampId(state.getValue(WaterQualityStates.QUALITY));
        }
        return DEFAULT_QUALITY_ID;
    }

    public static Component displayName(ItemStack stack) {
        WaterQuality quality = quality(stack);
        Identifier key = ModWaterQualities.REGISTRY.getKey(quality);
        if (key != null) {
            return Component.translatable("item.minecraft.wet_sponge." + key.getPath());
        }
        return Component.translatable(Items.WET_SPONGE.getDescriptionId());
    }
}
