package com.example.realismsurvival.client;

import com.example.realismsurvival.RealismSurvival;
import com.example.realismsurvival.water.ModEnchantments;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Liquid tint for graded water bottles: base quality color, shifted toward warm red / cool cyan
 * when the bottle carries warming / cooling (stand-in for colored enchantment glint).
 *
 * <p>Lives under {@code client} and is registered only from {@link com.example.realismsurvival.RealismSurvivalClient}
 * — do not use {@code @OnlyIn}; NeoForge no longer strips those members at runtime.
 */
public record GradedWaterTint(int baseColor) implements ItemTintSource {

    public static final Identifier ID = RealismSurvival.id("graded_water");
    public static final MapCodec<GradedWaterTint> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ExtraCodecs.RGB_COLOR_CODEC.fieldOf("base").forGetter(GradedWaterTint::baseColor)
    ).apply(instance, GradedWaterTint::new));

    private static final int WARM_SHIFT = 0xFF6A3A;
    private static final int COOL_SHIFT = 0x3AC8FF;
    private static final float ENCHANT_BLEND = 0.55F;

    @Override
    public int calculate(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner) {
        int color = baseColor;
        if (level != null) {
            var registries = level.registryAccess();
            if (ModEnchantments.hasWarming(stack, registries)) {
                color = blendRgb(color, WARM_SHIFT, ENCHANT_BLEND);
            } else if (ModEnchantments.hasCooling(stack, registries)) {
                color = blendRgb(color, COOL_SHIFT, ENCHANT_BLEND);
            }
        }
        return ARGB.opaque(color);
    }

    @Override
    public MapCodec<GradedWaterTint> type() {
        return MAP_CODEC;
    }

    private static int blendRgb(int from, int to, float amount) {
        int fr = (from >> 16) & 0xFF;
        int fg = (from >> 8) & 0xFF;
        int fb = from & 0xFF;
        int tr = (to >> 16) & 0xFF;
        int tg = (to >> 8) & 0xFF;
        int tb = to & 0xFF;
        int r = Math.round(fr + (tr - fr) * amount);
        int g = Math.round(fg + (tg - fg) * amount);
        int b = Math.round(fb + (tb - fb) * amount);
        return (r << 16) | (g << 8) | b;
    }
}
