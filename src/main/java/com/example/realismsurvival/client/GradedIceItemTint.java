package com.example.realismsurvival.client;

import com.example.realismsurvival.RealismSurvival;
import com.example.realismsurvival.api.water.WaterQuality;
import com.example.realismsurvival.water.GradedIceItems;
import com.example.realismsurvival.water.WaterColors;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Inventory / hand tint for graded ice — same grade blend as {@link GradedIceTint} (block).
 * Quality is read from {@code BLOCK_STATE} on the stack.
 */
public final class GradedIceItemTint implements ItemTintSource {

    public static final Identifier ID = RealismSurvival.id("graded_ice");
    public static final GradedIceItemTint INSTANCE = new GradedIceItemTint();
    public static final MapCodec<GradedIceItemTint> MAP_CODEC = MapCodec.unit(INSTANCE);

    private static final float GRADE_BLEND = 0.82F;
    private static final int ICE_BASE = 0xE8F4FF;

    private GradedIceItemTint() {}

    @Override
    public int calculate(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner) {
        WaterQuality quality = GradedIceItems.quality(stack);
        return ARGB.opaque(blend(WaterColors.of(quality)));
    }

    @Override
    public MapCodec<GradedIceItemTint> type() {
        return MAP_CODEC;
    }

    /** Shared with {@link GradedIceTint}. */
    static int blend(int gradeRgb) {
        return ARGB.color(
                Mth.lerpInt(GRADE_BLEND, ARGB.red(ICE_BASE), ARGB.red(gradeRgb)),
                Mth.lerpInt(GRADE_BLEND, ARGB.green(ICE_BASE), ARGB.green(gradeRgb)),
                Mth.lerpInt(GRADE_BLEND, ARGB.blue(ICE_BASE), ARGB.blue(gradeRgb))
        );
    }
}
