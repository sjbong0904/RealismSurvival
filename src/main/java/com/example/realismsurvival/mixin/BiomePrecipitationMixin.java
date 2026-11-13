package com.example.realismsurvival.mixin;

import com.example.realismsurvival.environment.ClimateZone;
import com.example.realismsurvival.environment.Season;
import com.example.realismsurvival.environment.SeasonCalendar;
import com.example.realismsurvival.environment.SeasonClimate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Own-calendar precip policy. Skipped entirely when Serene Seasons is active.
 *
 * <p>{@link Biome#warmEnoughToRain} has no Level/Holder, so zone is inferred from base temperature
 * (tags still drive ambient {@code seasonTerm} where Holders are available).
 */
@Mixin(Biome.class)
public abstract class BiomePrecipitationMixin {

    @Inject(method = "warmEnoughToRain(Lnet/minecraft/core/BlockPos;I)Z", at = @At("HEAD"), cancellable = true)
    private void realismsurvival$seasonPrecip(BlockPos pos, int seaLevel, CallbackInfoReturnable<Boolean> cir) {
        if (SeasonCalendar.sereneSeasonsActive()) {
            return;
        }
        Season season = SeasonCalendar.cachedOverworldSeason();
        ClimateZone zone = zoneFromTemperature(((Biome) (Object) this).getBaseTemperature());
        SeasonClimate.PrecipPolicy policy = SeasonClimate.resolve(zone, season).precip();
        switch (policy) {
            case FORCE_SNOW -> cir.setReturnValue(false);
            case FORCE_RAIN -> cir.setReturnValue(true);
            case VANILLA -> {
                // no override
            }
        }
    }

    private static ClimateZone zoneFromTemperature(float base) {
        if (base < 0.2F) {
            return ClimateZone.COLD;
        }
        if (base >= 1.5F) {
            return ClimateZone.ARID;
        }
        if (base >= 0.95F) {
            return ClimateZone.TROPICAL;
        }
        return ClimateZone.TEMPERATE;
    }
}
