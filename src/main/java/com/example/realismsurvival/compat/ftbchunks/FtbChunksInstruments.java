package com.example.realismsurvival.compat.ftbchunks;

import com.example.realismsurvival.RealismSurvival;
import com.example.realismsurvival.client.InstrumentAccess;
import com.example.realismsurvival.client.InstrumentHudText;
import com.example.realismsurvival.compat.ModCompat;
import com.example.realismsurvival.compat.seasonhud.SeasonHudLayoutBridge;
import com.example.realismsurvival.environment.SeasonCalendar;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.api.client.FTBChunksClientAPI;
import dev.ftb.mods.ftbchunks.api.client.minimap.MinimapComponentContext;
import dev.ftb.mods.ftbchunks.api.client.minimap.MinimapInfoComponent;
import dev.ftb.mods.ftbchunks.config.FTBChunksClientConfig;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * Season / clock / ambient °C as FTB Chunks minimap info rows (under biome).
 *
 * <p>SeasonHUD's own FTB component is a stub in 26.1.2-2.0.10, so RS registers these instead.
 * Compile-only FTB API — call {@link #registerIfPresent()} only after {@code ftbchunks} is loaded.
 */
public final class FtbChunksInstruments {

    public static final Identifier SEASON_ID = RealismSurvival.id("season");
    public static final Identifier CLOCK_ID = RealismSurvival.id("clock");
    public static final Identifier AMBIENT_TEMP_ID = RealismSurvival.id("ambient_temp");

    private static final String BIOME_ID = "ftbchunks:biome";
    private static final String PLAYER_POS_ID = "ftbchunks:player_pos";

    private FtbChunksInstruments() {}

    public static void registerIfPresent() {
        if (!ModCompat.loaded(ModCompat.FTB_CHUNKS)) {
            return;
        }
        try {
            register();
        } catch (RuntimeException e) {
            RealismSurvival.LOGGER.warn("FTB Chunks minimap instrument registration failed: {}", e.toString());
        }
    }

    /**
     * True when the FTB minimap is on — standalone {@code InstrumentsHudLayer} should stay hidden
     * so clock/temp are not drawn twice (SeasonHUD default overlay is already suppressed).
     */
    public static boolean minimapOwnsInstruments() {
        if (!ModCompat.loaded(ModCompat.FTB_CHUNKS)) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(FTBChunksClientConfig.MINIMAP_ENABLED.get());
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static void register() {
        FTBChunksClientAPI api = FTBChunksAPI.clientApi();
        MinimapInfoComponent season = new SeasonLine();
        MinimapInfoComponent clock = new ClockLine();
        MinimapInfoComponent temp = new AmbientTempLine();
        api.registerMinimapComponent(season);
        api.registerMinimapComponent(clock);
        api.registerMinimapComponent(temp);
        insertAfterBiome(List.of(
                SEASON_ID.toString(), CLOCK_ID.toString(), AMBIENT_TEMP_ID.toString()));
        RealismSurvival.LOGGER.info("[RealismSurvival] FTB Chunks minimap instruments registered");
    }

    private static void insertAfterBiome(List<String> ours) {
        List<String> current = FTBChunksClientConfig.MINIMAP_INFO_ORDER.get();
        List<String> order = new ArrayList<>(current);
        order.removeAll(ours);
        int insertAt;
        int biomeIdx = order.indexOf(BIOME_ID);
        if (biomeIdx >= 0) {
            insertAt = biomeIdx + 1;
        } else {
            int posIdx = order.indexOf(PLAYER_POS_ID);
            insertAt = posIdx >= 0 ? posIdx + 1 : 0;
        }
        order.addAll(insertAt, ours);
        if (order.equals(current)) {
            return;
        }
        FTBChunksClientConfig.MINIMAP_INFO_ORDER.set(order);
        FTBChunksClientConfig.saveNeeded();
    }

    private static final class SeasonLine implements MinimapInfoComponent {
        @Override
        public Identifier id() {
            return SEASON_ID;
        }

        @Override
        public boolean shouldRender(MinimapComponentContext context) {
            return line(context) != null;
        }

        @Override
        public void render(MinimapComponentContext context, GuiGraphicsExtractor graphics, Font font) {
            Component line = line(context);
            if (line != null) {
                drawCenteredText(font, graphics, line, 0);
            }
        }

        private static @Nullable Component line(MinimapComponentContext context) {
            Minecraft minecraft = context.minecraft();
            Component seasonHud = SeasonHudLayoutBridge.seasonHudText(minecraft);
            if (seasonHud != null) {
                return seasonHud;
            }
            Player player = context.clientPlayer();
            Level level = context.clientLevel();
            if (player == null || level == null || !InstrumentAccess.hasCalendar(player)) {
                return null;
            }
            return SeasonCalendar.from(level).hudLine();
        }
    }

    private static final class ClockLine implements MinimapInfoComponent {
        @Override
        public Identifier id() {
            return CLOCK_ID;
        }

        @Override
        public boolean shouldRender(MinimapComponentContext context) {
            Player player = context.clientPlayer();
            return player != null && InstrumentAccess.hasClock(player);
        }

        @Override
        public void render(MinimapComponentContext context, GuiGraphicsExtractor graphics, Font font) {
            Level level = context.clientLevel();
            if (level == null) {
                return;
            }
            drawCenteredText(font, graphics, InstrumentHudText.clockLine(level.getOverworldClockTime()), 0);
        }
    }

    private static final class AmbientTempLine implements MinimapInfoComponent {
        @Override
        public Identifier id() {
            return AMBIENT_TEMP_ID;
        }

        @Override
        public boolean shouldRender(MinimapComponentContext context) {
            Player player = context.clientPlayer();
            return player != null && InstrumentAccess.hasThermometer(player);
        }

        @Override
        public void render(MinimapComponentContext context, GuiGraphicsExtractor graphics, Font font) {
            Player player = context.clientPlayer();
            Level level = context.clientLevel();
            if (player == null || level == null) {
                return;
            }
            drawCenteredText(font, graphics, InstrumentHudText.ambientTempLine(player, level), 0);
        }
    }
}
