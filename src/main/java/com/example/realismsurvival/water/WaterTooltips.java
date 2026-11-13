package com.example.realismsurvival.water;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;

/**
 * Hover helpers for drink / graded-water tooltips (side-agnostic text only).
 * Hunger/saturation icons are AppleSkin; thirst icons are {@code ThirstStatsClientTooltip}.
 */
public final class WaterTooltips {

    private WaterTooltips() {}

    public static void appendBottleUseHint(Consumer<Component> tooltip) {
        tooltip.accept(translatable("tooltip.realismsurvival.water.bottle_use").withStyle(ChatFormatting.DARK_GRAY));
    }

    /**
     * Two short lines: use on block / use in air. Block line depends on fill
     * (full = place; partial = discard or cannot place — matches item {@code use}).
     */
    public static void appendBucketUseHint(String keyPrefix, ItemStack stack, Consumer<Component> tooltip) {
        String fillKey = BucketFill.isFull(stack) ? "full" : "partial";
        tooltip.accept(translatable(keyPrefix + "." + fillKey + ".block").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.accept(translatable(keyPrefix + "." + fillKey + ".air").withStyle(ChatFormatting.DARK_GRAY));
    }

    public static Component temperatureComponent(float delta) {
        String formatted = delta > 0.0F ? "+" + trim(delta) : trim(delta);
        ChatFormatting color = delta > 0.0F ? ChatFormatting.GOLD : (delta < 0.0F ? ChatFormatting.BLUE : ChatFormatting.GRAY);
        return translatable("tooltip.realismsurvival.drink.temperature", formatted).withStyle(color);
    }

    /** Duration-scaled copies for tooltip preview (bucket sip = fill/3). Amplifier unchanged. */
    public static List<MobEffectInstance> scaleEffectDurations(List<MobEffectInstance> effects, float multiplier) {
        float m = Math.max(0.0F, multiplier);
        if (m == 1.0F) {
            return effects;
        }
        List<MobEffectInstance> scaled = new ArrayList<>(effects.size());
        for (MobEffectInstance effect : effects) {
            int duration = effect.getDuration();
            if (duration > 0) {
                duration = Math.max(1, Math.round(duration * m));
            }
            scaled.add(new MobEffectInstance(
                    effect.getEffect(),
                    duration,
                    effect.getAmplifier(),
                    effect.isAmbient(),
                    effect.isVisible(),
                    effect.showIcon()
            ));
        }
        return scaled;
    }

    public static void appendEffectTooltip(List<MobEffectInstance> effects, Consumer<Component> tooltip, float tickRate) {
        if (effects.isEmpty()) {
            return;
        }
        PotionContents.addPotionTooltip(effects, tooltip, 1.0F, tickRate);
    }

    private static MutableComponent translatable(String key, Object... args) {
        return Component.translatable(key, args);
    }

    private static String trim(float value) {
        if (value == (int) value) {
            return Integer.toString((int) value);
        }
        // One decimal when useful (saturation often fractional).
        String s = String.format(java.util.Locale.ROOT, "%.1f", value);
        if (s.endsWith(".0")) {
            return s.substring(0, s.length() - 2);
        }
        return s;
    }
}
