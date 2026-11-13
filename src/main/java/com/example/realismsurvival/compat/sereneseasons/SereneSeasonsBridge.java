package com.example.realismsurvival.compat.sereneseasons;

import com.example.realismsurvival.RealismSurvival;
import com.example.realismsurvival.compat.ModCompat;
import com.example.realismsurvival.environment.Season;
import java.lang.reflect.Method;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

/**
 * Reflection soft-compat for Serene Seasons. No compile dependency —
 * SS (+ GlitchCore) is a {@code localRuntime} optional for {@code runClient}.
 *
 * <p>When loaded: season authority is SS; precip/foliage stay with SS; we only read season/day.
 */
public final class SereneSeasonsBridge {

    private static final String HELPER = "sereneseasons.api.season.SeasonHelper";

    private static Boolean available;
    private static Method getSeasonState;
    private static Method getSeason;
    private static Method getDay;
    private static Method getSeasonCycleTicks;
    private static Method getCycleDuration;
    private static Method usesTropicalSeasons;
    private static volatile boolean lookupFailureLogged;

    private SereneSeasonsBridge() {}

    private static void logLookupFailureOnce(String what, Throwable error) {
        if (!lookupFailureLogged) {
            lookupFailureLogged = true;
            RealismSurvival.LOGGER.warn(
                    "Serene Seasons {} lookup failed; falling back to the Realism Survival calendar and crop season rules: {}",
                    what,
                    error.toString()
            );
        }
    }

    public static boolean present() {
        return ModCompat.loaded(ModCompat.SERENE_SEASONS) && resolve();
    }

    /** Current SS season mapped to our {@link Season}, or empty if SS is absent/unusable. */
    public static Optional<Season> season(Level level) {
        if (!present()) {
            return Optional.empty();
        }
        try {
            Object state = getSeasonState.invoke(null, level);
            Object ssSeason = getSeason.invoke(state);
            return Optional.of(Season.byName(((Enum<?>) ssSeason).name()));
        } catch (ReflectiveOperationException | RuntimeException e) {
            logLookupFailureOnce("season", e);
            return Optional.empty();
        }
    }

    /** Day index from SS cycle, or empty. */
    public static Optional<Integer> dayInCycle(Level level) {
        if (!present()) {
            return Optional.empty();
        }
        try {
            Object state = getSeasonState.invoke(null, level);
            int day = ((Number) getDay.invoke(state)).intValue();
            return Optional.of(Math.max(0, day));
        } catch (ReflectiveOperationException | RuntimeException e) {
            logLookupFailureOnce("day", e);
            return Optional.empty();
        }
    }

    public static Optional<Integer> year(Level level) {
        if (!present()) {
            return Optional.empty();
        }
        try {
            Object state = getSeasonState.invoke(null, level);
            int cycleDuration = ((Number) getCycleDuration.invoke(state)).intValue();
            if (cycleDuration <= 0) {
                return Optional.of(1);
            }
            long absDays = Math.floorDiv(level.getOverworldClockTime(), 24_000L);
            int daysPerYear = Math.max(1, cycleDuration / 24_000);
            return Optional.of((int) (absDays / daysPerYear) + 1);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return Optional.of(1);
        }
    }

    public static boolean usesTropicalSeasons(Holder<Biome> biome) {
        if (!present() || usesTropicalSeasons == null) {
            return false;
        }
        try {
            return (boolean) usesTropicalSeasons.invoke(null, biome);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
        }
    }

    private static boolean resolve() {
        if (available != null) {
            return available;
        }
        try {
            Class<?> helper = Class.forName(HELPER);
            getSeasonState = helper.getMethod("getSeasonState", Level.class);
            usesTropicalSeasons = helper.getMethod("usesTropicalSeasons", Holder.class);
            Class<?> stateType = getSeasonState.getReturnType();
            getSeason = stateType.getMethod("getSeason");
            getDay = stateType.getMethod("getDay");
            getSeasonCycleTicks = stateType.getMethod("getSeasonCycleTicks");
            getCycleDuration = stateType.getMethod("getCycleDuration");
            available = true;
        } catch (ReflectiveOperationException e) {
            RealismSurvival.LOGGER.warn("Serene Seasons detected but API bridge failed: {}", e.toString());
            available = false;
        }
        return available;
    }
}
