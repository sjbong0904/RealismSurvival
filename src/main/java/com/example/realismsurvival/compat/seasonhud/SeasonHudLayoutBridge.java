package com.example.realismsurvival.compat.seasonhud;

import com.example.realismsurvival.RealismSurvival;
import com.example.realismsurvival.compat.ModCompat;
import java.lang.reflect.Method;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

/**
 * Reflection soft-compat for SeasonHUD layout. SeasonHUD is {@code localRuntime} only —
 * no compile dependency.
 *
 * <p>RS clock/thermometer attach to SeasonHUD's corner/scale so the instrument stack moves with
 * the season line when the FTB minimap is off. Minimap-on season text uses {@link #seasonHudText}
 * (calendar-gated, independent of {@code drawDefaultHud}).
 */
public final class SeasonHudLayoutBridge {

    private static final int MARGIN = 2;
    /** Pre-scale line step (SeasonHUD uses font height 9, then {@code pose.scale}). */
    public static final int STEP = 9;
    private static final double FALLBACK_SCALE = 1.0;

    private static Boolean available;
    private static Method getEnableMod;
    private static Method getHudLocation;
    private static Method getHudX;
    private static Method getHudY;
    private static Method getHudScale;
    private static Method drawDefaultHud;
    private static Method vanillaShouldDrawHud;
    private static Method hideHudInCurrentDimension;
    private static Method validNeedCalendar;
    private static Method currentSeasonGetInstance;
    private static Method currentSeasonGetHudText;
    private static Method fertilityGetInstance;
    private static Method fertilityShouldDrawNewLine;

    private SeasonHudLayoutBridge() {}

    public static boolean present() {
        return ModCompat.loaded(ModCompat.SEASON_HUD) && resolve();
    }

    /**
     * Season/date line for the FTB minimap. Ignores {@code drawDefaultHud} so the text still
     * exists when SeasonHUD hides its overlay because minimap integration is on.
     */
    public static @Nullable Component seasonHudText(Minecraft minecraft) {
        if (!present() || minecraft == null || minecraft.player == null) {
            return null;
        }
        try {
            if (!calendarHudVisible(minecraft)) {
                return null;
            }
            Object season = currentSeasonGetInstance.invoke(null, minecraft);
            return (Component) currentSeasonGetHudText.invoke(season);
        } catch (ReflectiveOperationException | RuntimeException e) {
            RealismSurvival.LOGGER.debug("SeasonHUD minimap text failed", e);
            return null;
        }
    }

    /** SeasonHUD corner/scale plus how many season lines are currently drawn. */
    public static Bundle resolve(Minecraft minecraft) {
        if (!present() || minecraft.player == null) {
            return Bundle.fallback();
        }
        try {
            if (!(boolean) getEnableMod.invoke(null)) {
                return Bundle.fallback();
            }

            double scale = ((Number) getHudScale.invoke(null)).doubleValue();
            if (scale <= 0.0) {
                scale = FALLBACK_SCALE;
            }

            Object location = getHudLocation.invoke(null);
            String loc = location instanceof Enum<?> e ? e.name() : String.valueOf(location);
            boolean bottom = loc.startsWith("BOTTOM");

            int screenW = minecraft.getWindow().getGuiScaledWidth();
            int screenH = minecraft.getWindow().getGuiScaledHeight();
            int cfgX = ((Number) getHudX.invoke(null)).intValue();
            int cfgY = ((Number) getHudY.invoke(null)).intValue();

            boolean seasonDrawn = isSeasonDrawn(minecraft);
            int seasonLines = seasonDrawn ? 1 + fertilityExtraLines(minecraft) : 0;
            int seasonTextW = seasonDrawn ? seasonTextWidth(minecraft, scale) : 0;

            int scaledLineH = (int) (STEP * scale);
            int[] seasonXY =
                    anchorXY(loc, screenW, screenH, seasonTextW, scaledLineH, scale, cfgX, cfgY);

            return new Bundle(loc, scale, bottom, seasonDrawn, seasonLines, seasonXY[0], seasonXY[1]);
        } catch (ReflectiveOperationException | RuntimeException e) {
            RealismSurvival.LOGGER.debug("SeasonHUD layout bridge failed", e);
            return Bundle.fallback();
        }
    }

    /** X for a line of the given width (right/center anchors follow SeasonHUD). */
    public static int lineX(Bundle bundle, Minecraft minecraft, int textWidth) {
        if (bundle == null || minecraft == null) {
            return MARGIN;
        }
        try {
            int screenW = minecraft.getWindow().getGuiScaledWidth();
            int screenH = minecraft.getWindow().getGuiScaledHeight();
            int cfgX = present() && getHudX != null ? ((Number) getHudX.invoke(null)).intValue() : MARGIN;
            int cfgY = present() && getHudY != null ? ((Number) getHudY.invoke(null)).intValue() : MARGIN;
            int scaledLineH = (int) (STEP * bundle.scale());
            int scaledTextW = (int) (textWidth * bundle.scale());
            return anchorXY(
                    bundle.location(),
                    screenW,
                    screenH,
                    scaledTextW,
                    scaledLineH,
                    bundle.scale(),
                    cfgX,
                    cfgY)[0];
        } catch (ReflectiveOperationException | RuntimeException e) {
            return bundle.seasonX();
        }
    }

    /**
     * Y of the first instrument line in pre-scale space.
     * Top anchors grow downward under the season line; bottom anchors keep season on the lowest row.
     */
    public static int stackStartY(Bundle bundle, int instrumentLines) {
        int n = Math.max(0, instrumentLines);
        if (n == 0) {
            return bundle.seasonY();
        }
        if (bundle.bottom()) {
            return bundle.seasonY() - (bundle.seasonLines() + n - 1) * STEP;
        }
        return bundle.seasonY() + bundle.seasonLines() * STEP;
    }

    private static boolean isSeasonDrawn(Minecraft minecraft) throws ReflectiveOperationException {
        if (!(boolean) drawDefaultHud.invoke(null, minecraft)) {
            return false;
        }
        return calendarHudVisible(minecraft);
    }

    private static boolean calendarHudVisible(Minecraft minecraft) throws ReflectiveOperationException {
        Player player = minecraft.player;
        if (player == null) {
            return false;
        }
        if (!(boolean) getEnableMod.invoke(null)) {
            return false;
        }
        if (!(boolean) vanillaShouldDrawHud.invoke(null, minecraft)) {
            return false;
        }
        if ((boolean) hideHudInCurrentDimension.invoke(null, minecraft)) {
            return false;
        }
        return (boolean) validNeedCalendar.invoke(null, player);
    }

    private static int fertilityExtraLines(Minecraft minecraft) {
        if (fertilityGetInstance == null || fertilityShouldDrawNewLine == null) {
            return 0;
        }
        try {
            Object fert = fertilityGetInstance.invoke(null, minecraft);
            return (boolean) fertilityShouldDrawNewLine.invoke(fert) ? 1 : 0;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return 0;
        }
    }

    private static int seasonTextWidth(Minecraft minecraft, double scale) {
        try {
            Object season = currentSeasonGetInstance.invoke(null, minecraft);
            Component text = (Component) currentSeasonGetHudText.invoke(season);
            return (int) (minecraft.font.width(text) * scale);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return 40;
        }
    }

    private static int[] anchorXY(
            String loc,
            int screenW,
            int screenH,
            int scaledTextW,
            int scaledLineH,
            double scale,
            int cfgX,
            int cfgY) {
        int x;
        int y;
        switch (loc) {
            case "TOP_CENTER" -> {
                x = (int) ((screenW / 2.0 - scaledTextW / 2.0) / scale);
                y = MARGIN;
            }
            case "TOP_RIGHT" -> {
                x = (int) ((screenW - scaledTextW - MARGIN) / scale);
                y = MARGIN;
            }
            case "BOTTOM_LEFT" -> {
                x = MARGIN;
                y = (int) ((screenH - scaledLineH - MARGIN) / scale);
            }
            case "BOTTOM_RIGHT" -> {
                x = (int) ((screenW - scaledTextW - MARGIN) / scale);
                y = (int) ((screenH - scaledLineH - MARGIN) / scale);
            }
            case "CUSTOM" -> {
                x = cfgX;
                y = cfgY;
            }
            default -> {
                x = MARGIN;
                y = MARGIN;
            }
        }
        return new int[] {x, y};
    }

    private static boolean resolve() {
        if (available != null) {
            return available;
        }
        try {
            Class<?> client = Class.forName("club.iananderson.seasonhud.config.SeasonHudClient");
            getEnableMod = client.getMethod("getEnableMod");
            getHudLocation = client.getMethod("getHudLocation");
            getHudX = client.getMethod("getHudX");
            getHudY = client.getMethod("getHudY");
            getHudScale = client.getMethod("getHudScale");

            Class<?> common = Class.forName("club.iananderson.seasonhud.Common");
            drawDefaultHud = common.getMethod("drawDefaultHud", Minecraft.class);
            vanillaShouldDrawHud = common.getMethod("vanillaShouldDrawHud", Minecraft.class);
            hideHudInCurrentDimension = common.getMethod("hideHudInCurrentDimension", Minecraft.class);

            Class<?> calendar = Class.forName("club.iananderson.seasonhud.impl.accessory.mods.Calendar");
            validNeedCalendar = calendar.getMethod("validNeedCalendar", Player.class);

            Class<?> currentSeason = Class.forName("club.iananderson.seasonhud.impl.season.CurrentSeason");
            currentSeasonGetInstance = currentSeason.getMethod("getInstance", Minecraft.class);
            currentSeasonGetHudText = currentSeason.getMethod("getHudText");

            try {
                Class<?> fertility = Class.forName("club.iananderson.seasonhud.impl.season.CurrentFertility");
                fertilityGetInstance = fertility.getMethod("getInstance", Minecraft.class);
                fertilityShouldDrawNewLine = fertility.getMethod("shouldDrawNewLine");
            } catch (ReflectiveOperationException ignored) {
                fertilityGetInstance = null;
                fertilityShouldDrawNewLine = null;
            }

            available = true;
        } catch (ReflectiveOperationException e) {
            RealismSurvival.LOGGER.warn("SeasonHUD detected but layout bridge failed: {}", e.toString());
            available = false;
        }
        return available;
    }

    public record Bundle(
            String location,
            double scale,
            boolean bottom,
            boolean seasonDrawn,
            int seasonLines,
            int seasonX,
            int seasonY) {
        static Bundle fallback() {
            return new Bundle("TOP_LEFT", FALLBACK_SCALE, false, false, 0, MARGIN, MARGIN);
        }
    }
}
