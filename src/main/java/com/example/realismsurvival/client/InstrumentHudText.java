package com.example.realismsurvival.client;

import com.example.realismsurvival.environment.ClimateTemperature;
import com.example.realismsurvival.environment.TemperatureEvaluator;
import java.util.Locale;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

/** Shared clock / ambient-temperature lines for the standalone HUD and FTB minimap info. */
public final class InstrumentHudText {

    private static final int CLOCK_RGB = 0xFFA040;

    private InstrumentHudText() {}

    public static Component clockLine(long dayTime) {
        int timeOfDay = (int) (dayTime % 24_000L);
        int totalMinutes = (timeOfDay * 60 * 24) / 24_000;
        int hours = (totalMinutes / 60 + 6) % 24;
        int minutes = totalMinutes % 60;
        String icon = (hours >= 6 && hours < 18) ? "\u2600" : "\u263D";

        return Component.translatable(
                        "hud.realismsurvival.clock_time",
                        icon,
                        String.format(Locale.ROOT, "%02d", hours),
                        String.format(Locale.ROOT, "%02d", minutes))
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(CLOCK_RGB)));
    }

    public static Component ambientTempLine(Player player, Level level) {
        Holder<Biome> biome = level.getBiome(player.blockPosition());
        float ambient = TemperatureEvaluator.computeAmbient(player, level, biome);
        float rounded = ClimateTemperature.roundOneDecimal(ambient);
        return Component.translatable("hud.realismsurvival.ambient_temp", formatTemp(rounded))
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(tempHudRgb(rounded))));
    }

    private static String formatTemp(float celsius) {
        return String.format(Locale.ROOT, "%.1f", celsius);
    }

    static int tempHudRgb(float celsius) {
        float t = Mth.clamp((celsius - (-5.0F)) / (40.0F - (-5.0F)), 0.0F, 1.0F);
        if (t < 0.35F) {
            return lerpRgb(0x5AB0FF, 0xA8D8FF, t / 0.35F);
        }
        if (t < 0.55F) {
            return lerpRgb(0xA8D8FF, 0xE8E8E8, (t - 0.35F) / 0.20F);
        }
        if (t < 0.75F) {
            return lerpRgb(0xE8E8E8, 0xFFB060, (t - 0.55F) / 0.20F);
        }
        return lerpRgb(0xFFB060, 0xFF5A3A, (t - 0.75F) / 0.25F);
    }

    private static int lerpRgb(int from, int to, float amount) {
        float a = Mth.clamp(amount, 0.0F, 1.0F);
        int fr = (from >> 16) & 0xFF;
        int fg = (from >> 8) & 0xFF;
        int fb = from & 0xFF;
        int tr = (to >> 16) & 0xFF;
        int tg = (to >> 8) & 0xFF;
        int tb = to & 0xFF;
        int r = Math.round(fr + (tr - fr) * a);
        int g = Math.round(fg + (tg - fg) * a);
        int b = Math.round(fb + (tb - fb) * a);
        return (r << 16) | (g << 8) | b;
    }
}
