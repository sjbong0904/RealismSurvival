package com.example.realismsurvival.client;

import com.example.realismsurvival.api.water.WaterQuality;
import com.example.realismsurvival.water.WaterColors;
import com.example.realismsurvival.water.WaterQualityStates;
import java.util.Set;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

/**
 * Grade tint on shared ice texture — strongly biased toward {@link WaterColors}
 * so ice reads as the matching fluid grade (distilled / fresh / contaminated / salt).
 */
public final class GradedIceTint implements BlockTintSource {

    public static final GradedIceTint INSTANCE = new GradedIceTint();

    private GradedIceTint() {}

    @Override
    public int color(BlockState state) {
        return tintRgb(state);
    }

    @Override
    public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) {
        return tintRgb(state);
    }

    @Override
    public Set<Property<?>> relevantProperties() {
        return Set.of(WaterQualityStates.QUALITY);
    }

    private static int tintRgb(BlockState state) {
        int qualityId = state.hasProperty(WaterQualityStates.QUALITY)
                ? state.getValue(WaterQualityStates.QUALITY)
                : 1;
        WaterQuality quality = WaterQualityStates.byId(qualityId);
        return GradedIceItemTint.blend(WaterColors.of(quality));
    }
}
