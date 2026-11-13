package com.example.realismsurvival.environment;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;

/**
 * Zone × season → ambient offset and precip policy for the own-calendar path.
 * When Serene Seasons is loaded, precip mixins no-op ({@link SeasonCalendar#sereneSeasonsActive()}).
 */
public final class SeasonClimate {

    private SeasonClimate() {}

    public enum PrecipPolicy {
        /** Do not override vanilla precip. */
        VANILLA,
        /** Prefer snow when the world is precipitating. */
        FORCE_SNOW,
        /** Prefer rain (suppress snow). */
        FORCE_RAIN
    }

    public record Profile(float tempOffsetC, PrecipPolicy precip, float cropMoistureBonus) {}

    public static Profile resolve(Holder<Biome> biome, Season season) {
        return resolve(ClimateZone.of(biome), season);
    }

    public static Profile resolve(ClimateZone zone, Season season) {
        return switch (zone) {
            case TEMPERATE -> temperate(season);
            case COLD -> cold(season);
            case ARID -> arid(season);
            case TROPICAL -> tropical(season);
            case OCEAN -> ocean(season);
        };
    }

    private static Profile temperate(Season season) {
        return switch (season) {
            case SPRING -> new Profile(0.0F, PrecipPolicy.VANILLA, 0.0F);
            case SUMMER -> new Profile(3.0F, PrecipPolicy.VANILLA, 0.15F);
            case AUTUMN -> new Profile(-1.0F, PrecipPolicy.VANILLA, 0.0F);
            case WINTER -> new Profile(-8.0F, PrecipPolicy.FORCE_SNOW, 0.0F);
        };
    }

    private static Profile cold(Season season) {
        return switch (season) {
            case SPRING -> new Profile(-2.0F, PrecipPolicy.VANILLA, 0.0F);
            case SUMMER -> new Profile(1.0F, PrecipPolicy.VANILLA, 0.1F);
            case AUTUMN -> new Profile(-4.0F, PrecipPolicy.FORCE_SNOW, 0.0F);
            case WINTER -> new Profile(-12.0F, PrecipPolicy.FORCE_SNOW, 0.0F);
        };
    }

    private static Profile arid(Season season) {
        return switch (season) {
            case SPRING -> new Profile(6.0F, PrecipPolicy.FORCE_RAIN, 0.0F);
            case SUMMER -> new Profile(10.0F, PrecipPolicy.FORCE_RAIN, 0.0F);
            case AUTUMN -> new Profile(5.0F, PrecipPolicy.FORCE_RAIN, 0.0F);
            case WINTER -> new Profile(2.0F, PrecipPolicy.FORCE_RAIN, 0.0F);
        };
    }

    private static Profile tropical(Season season) {
        return switch (season) {
            case SPRING -> new Profile(4.0F, PrecipPolicy.FORCE_RAIN, 0.05F);
            case SUMMER -> new Profile(5.0F, PrecipPolicy.FORCE_RAIN, 0.15F);
            case AUTUMN -> new Profile(3.0F, PrecipPolicy.FORCE_RAIN, 0.05F);
            case WINTER -> new Profile(2.0F, PrecipPolicy.FORCE_RAIN, 0.0F);
        };
    }

    private static Profile ocean(Season season) {
        return switch (season) {
            case SPRING -> new Profile(0.0F, PrecipPolicy.FORCE_RAIN, 0.0F);
            case SUMMER -> new Profile(2.0F, PrecipPolicy.FORCE_RAIN, 0.05F);
            case AUTUMN -> new Profile(-1.0F, PrecipPolicy.FORCE_RAIN, 0.0F);
            case WINTER -> new Profile(-3.0F, PrecipPolicy.FORCE_RAIN, 0.0F);
        };
    }
}
