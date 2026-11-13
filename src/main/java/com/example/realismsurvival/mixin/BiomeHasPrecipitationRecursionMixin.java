package com.example.realismsurvival.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Breaks Serene Seasons' client {@code MixinBiomeClient.onHasPrecipitation} recursion:
 * {@code Biome.hasPrecipitation} → {@code SeasonHooks.hasPrecipitationSeasonal} →
 * {@code Biome.hasPrecipitation} (non-tropical fallback).
 *
 * <p>On re-entry, return climate settings directly so SS can read the vanilla precip flag
 * without re-running its HEAD inject.
 */
@Mixin(value = Biome.class, priority = 2000)
public abstract class BiomeHasPrecipitationRecursionMixin {

    @Unique
    private static final ThreadLocal<Boolean> RS$IN_HAS_PRECIPITATION =
            ThreadLocal.withInitial(() -> false);

    @WrapMethod(method = "hasPrecipitation")
    private boolean realismsurvival$guardHasPrecipitation(Operation<Boolean> original) {
        if (Boolean.TRUE.equals(RS$IN_HAS_PRECIPITATION.get())) {
            return ((Biome) (Object) this).getModifiedClimateSettings().hasPrecipitation();
        }
        RS$IN_HAS_PRECIPITATION.set(true);
        try {
            return original.call();
        } finally {
            RS$IN_HAS_PRECIPITATION.set(false);
        }
    }
}
