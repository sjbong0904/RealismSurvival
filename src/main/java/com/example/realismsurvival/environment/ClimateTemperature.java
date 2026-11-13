package com.example.realismsurvival.environment;

import com.example.realismsurvival.compat.sereneseasons.SereneSeasonsBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;

/**
 * Maps vanilla biome temperature (snow/rain scale) to display / ambient °C (spec §9.3).
 *
 * <pre>
 * t = biomeBase − max(0, y − seaLevel) / 600   // vanilla altitude lapse
 * °C = −5 + t × 25
 * </pre>
 */
public final class ClimateTemperature {

    /** Vanilla: temperature drops by 1/600 per block above sea level. */
    public static final float VANILLA_ALTITUDE_LAPSE = 1.0F / 600.0F;

    /** Match biome noise quart resolution — enough detail, far cheaper than every block. */
    private static final int BLEND_SAMPLE_STEP = 4;

    private static final float CELSIUS_OFFSET = -5.0F;
    private static final float CELSIUS_SCALE = 25.0F;

    private ClimateTemperature() {}

    /**
     * Distance-weighted blend of nearby biome climates (horizontal Chebyshev disk).
     *
     * @param radius 0 → foot biome only; samples every {@value #BLEND_SAMPLE_STEP} blocks
     */
    public static BiomeClimateBlend blendNearby(
            LevelReader level, BlockPos origin, int radius, Season season
    ) {
        Holder<Biome> foot = level.getBiome(origin);
        if (radius <= 0) {
            return sampleOne(foot, season);
        }

        float sumVanilla = 0.0F;
        float sumSeason = 0.0F;
        float sumWeight = 0.0F;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int y = origin.getY();

        for (int dx = -radius; dx <= radius; dx += BLEND_SAMPLE_STEP) {
            for (int dz = -radius; dz <= radius; dz += BLEND_SAMPLE_STEP) {
                cursor.set(origin.getX() + dx, y, origin.getZ() + dz);
                int cheb = Math.max(Math.abs(dx), Math.abs(dz));
                float weight = 1.0F / (1.0F + cheb);
                Holder<Biome> biome = level.getBiome(cursor);
                sumVanilla += biome.value().getBaseTemperature() * weight;
                sumSeason += seasonOffsetC(biome, season) * weight;
                sumWeight += weight;
            }
        }

        if (sumWeight <= 0.0F) {
            return sampleOne(foot, season);
        }
        return new BiomeClimateBlend(sumVanilla / sumWeight, sumSeason / sumWeight);
    }

    private static BiomeClimateBlend sampleOne(Holder<Biome> biome, Season season) {
        return new BiomeClimateBlend(biome.value().getBaseTemperature(), seasonOffsetC(biome, season));
    }

    private static float seasonOffsetC(Holder<Biome> biome, Season season) {
        float offset = SeasonClimate.resolve(biome, season).tempOffsetC();
        if (SereneSeasonsBridge.usesTropicalSeasons(biome) && season == Season.WINTER) {
            return Math.max(offset, 2.0F);
        }
        return offset;
    }

    /**
     * @param vanillaBase blended or single-biome {@link Biome#getBaseTemperature()}
     * @param seasonOffsetC blended zone×season °C term
     */
    public record BiomeClimateBlend(float vanillaBase, float seasonOffsetC) {}

    /** Vanilla effective temperature at {@code y} (sea level and below: no lapse). */
    public static float vanillaEffectiveTemp(float biomeBaseTemp, int y, int seaLevel) {
        float lapse = Math.max(0, y - seaLevel) * VANILLA_ALTITUDE_LAPSE;
        return biomeBaseTemp - lapse;
    }

    public static float vanillaEffectiveTemp(Biome biome, int y, int seaLevel) {
        return vanillaEffectiveTemp(biome.getBaseTemperature(), y, seaLevel);
    }

    /** Single conversion: vanilla temperature scale → °C. */
    public static float toCelsius(float vanillaTemp) {
        return CELSIUS_OFFSET + vanillaTemp * CELSIUS_SCALE;
    }

    /**
     * Climate ambient °C from biome + altitude (no day/night, wetness, or heat).
     */
    public static float climateCelsius(float biomeBaseTemp, int y, int seaLevel) {
        return toCelsius(vanillaEffectiveTemp(biomeBaseTemp, y, seaLevel));
    }

    /** °C contribution of altitude alone vs sea-level climate for the same biome base. */
    public static float altitudeCelsiusOffset(float biomeBaseTemp, int y, int seaLevel) {
        float atSea = toCelsius(biomeBaseTemp);
        float atY = climateCelsius(biomeBaseTemp, y, seaLevel);
        return atY - atSea;
    }

    public static float roundOneDecimal(float celsius) {
        return Mth.floor(celsius * 10.0F + 0.5F) / 10.0F;
    }
}
