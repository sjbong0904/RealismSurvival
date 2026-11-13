package com.example.realismsurvival.environment;

import com.example.realismsurvival.compat.sereneseasons.SereneSeasonsBridge;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.Level;

/**
 * Season facade: Serene Seasons when present, otherwise a 365-day / 4-season own calendar.
 *
 * <p>Clock HUD uses {@code dayTime % 24000} (time-of-day). Calendar uses day index / SS day.
 * Those axes are orthogonal — they share the dayTime source but never the same field.
 */
public final class SeasonCalendar {

    public static final int DAYS_PER_YEAR = 365;
    /** Spring 91 · Summer 92 · Autumn 91 · Winter 91 */
    public static final int[] DAYS_IN_SEASON = {91, 92, 91, 91};
    /** Non-leap Gregorian month lengths (sum 365). */
    public static final int[] DAYS_IN_MONTH = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};

    /** Last overworld season seen on tick — used by precip mixin (no Level arg). */
    private static volatile Season cachedOverworldSeason = Season.SPRING;
    private static volatile boolean sereneSeasonsActive;

    private SeasonCalendar() {}

    public static void onLevelTick(Level level) {
        if (level.dimension() != Level.OVERWORLD) {
            return;
        }
        CalendarDate date = from(level);
        cachedOverworldSeason = date.season();
        sereneSeasonsActive = date.fromSereneSeasons();
    }

    public static Season cachedOverworldSeason() {
        return cachedOverworldSeason;
    }

    /** True when the last overworld season lookup came from SS — precip mixins must not override SS. */
    public static boolean sereneSeasonsActive() {
        return sereneSeasonsActive;
    }

    public static CalendarDate from(Level level) {
        SsSnapshot ss = trySerene(level);
        if (ss != null) {
            return ss.toDate();
        }
        return fromGameTime(level.getOverworldClockTime());
    }

    public static CalendarDate fromGameTime(long dayTime) {
        long day = Math.floorDiv(dayTime, 24_000L);
        if (day < 0L) {
            day = 0L;
        }
        int dayOfYear = (int) (day % DAYS_PER_YEAR);
        int year = (int) (day / DAYS_PER_YEAR) + 1;
        int[] monthDay = monthAndDay(dayOfYear);
        int cursor = dayOfYear;
        for (int i = 0; i < Season.values().length; i++) {
            int len = DAYS_IN_SEASON[i];
            if (cursor < len) {
                return new CalendarDate(
                        year, monthDay[0], monthDay[1], dayOfYear, Season.values()[i], cursor + 1, false);
            }
            cursor -= len;
        }
        return new CalendarDate(year, monthDay[0], monthDay[1], dayOfYear, Season.WINTER, DAYS_IN_SEASON[3], false);
    }

    /** 0-based day-of-year → 1-based month and day-of-month. */
    public static int[] monthAndDay(int dayOfYear) {
        int rem = Math.floorMod(dayOfYear, DAYS_PER_YEAR);
        for (int m = 0; m < DAYS_IN_MONTH.length; m++) {
            if (rem < DAYS_IN_MONTH[m]) {
                return new int[] {m + 1, rem + 1};
            }
            rem -= DAYS_IN_MONTH[m];
        }
        return new int[] {12, 31};
    }

    private static SsSnapshot trySerene(Level level) {
        var season = SereneSeasonsBridge.season(level);
        if (season.isEmpty()) {
            return null;
        }
        int day = SereneSeasonsBridge.dayInCycle(level).orElse(0);
        int year = SereneSeasonsBridge.year(level).orElse(1);
        Season s = season.get();
        int dayInSeason = (day % DAYS_IN_SEASON[s.ordinal()]) + 1;
        int dayOfYear = Math.floorMod(day, DAYS_PER_YEAR);
        int[] monthDay = monthAndDay(dayOfYear);
        return new SsSnapshot(year, monthDay[0], monthDay[1], dayOfYear, s, dayInSeason);
    }

    public record CalendarDate(
            int year,
            int month,
            int dayOfMonth,
            int dayOfYear,
            Season season,
            int dayInSeason,
            boolean fromSereneSeasons
    ) {
        /** {@code 🌸 1년 3월 15일 (봄)} — whole line tinted with season color. */
        public Component hudLine() {
            MutableComponent line = Component.translatable(
                    "hud.realismsurvival.calendar_line",
                    season.emoji(),
                    year,
                    month,
                    dayOfMonth,
                    season.displayName()
            );
            return line.withStyle(season.hudStyle());
        }
    }

    private record SsSnapshot(
            int year, int month, int dayOfMonth, int dayOfYear, Season season, int dayInSeason
    ) {
        CalendarDate toDate() {
            return new CalendarDate(year, month, dayOfMonth, dayOfYear, season, dayInSeason, true);
        }
    }
}
