package com.example.realismsurvival.water;

import com.example.realismsurvival.api.water.WaterQuality;
import com.example.realismsurvival.registry.ModWaterQualities;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/** Block item whose display name follows {@link WaterQualityStates#QUALITY} on the stack. */
public final class GradedIceBlockItem extends BlockItem {

    public GradedIceBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        WaterQuality quality = GradedIceItems.quality(stack);
        Identifier key = ModWaterQualities.REGISTRY.getKey(quality);
        if (key != null) {
            return Component.translatable("item.realismsurvival.graded_ice." + key.getPath());
        }
        return super.getName(stack);
    }
}
