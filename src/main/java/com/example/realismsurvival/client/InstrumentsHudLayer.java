package com.example.realismsurvival.client;

import com.example.realismsurvival.RealismSurvival;
import com.example.realismsurvival.compat.ftbchunks.FtbChunksInstruments;
import com.example.realismsurvival.compat.seasonhud.SeasonHudLayoutBridge;
import com.example.realismsurvival.compat.seasonhud.SeasonHudLayoutBridge.Bundle;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.gui.GuiLayer;
import org.joml.Matrix3x2fStack;

/**
 * Fallback instrument HUD: clock + ambient °C, stacked with SeasonHUD when the FTB minimap is off.
 *
 * <p>When FTB Chunks minimap is enabled, season/clock/temp are {@code MinimapInfoComponent}s under the
 * biome line instead. Season/date is still drawn by SeasonHUD in this fallback path (calendar-gated).
 */
public final class InstrumentsHudLayer implements GuiLayer {

    public static final Identifier LAYER_ID = RealismSurvival.id("instruments");

    private static final int DRAW_WHITE = 0xFFFFFFFF;

    private InstrumentsHudLayer() {}

    public static InstrumentsHudLayer create() {
        return new InstrumentsHudLayer();
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (FtbChunksInstruments.minimapOwnsInstruments()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Player player = HudLayout.cameraPlayer(minecraft);
        if (player == null) {
            return;
        }
        Level level = player.level();

        boolean showTime = InstrumentAccess.hasClock(player);
        boolean showTemp = InstrumentAccess.hasThermometer(player);
        if (!showTime && !showTemp) {
            return;
        }

        List<Component> lines = new ArrayList<>(2);
        if (showTime) {
            lines.add(InstrumentHudText.clockLine(level.getOverworldClockTime()));
        }
        if (showTemp) {
            lines.add(InstrumentHudText.ambientTempLine(player, level));
        }

        Font font = minecraft.font;
        Bundle bundle = SeasonHudLayoutBridge.resolve(minecraft);
        int y = SeasonHudLayoutBridge.stackStartY(bundle, lines.size());
        float scale = (float) bundle.scale();

        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        pose.scale(scale, scale);
        try {
            for (Component line : lines) {
                int x = SeasonHudLayoutBridge.lineX(bundle, minecraft, font.width(line));
                graphics.text(font, line, x, y, DRAW_WHITE, true);
                y += SeasonHudLayoutBridge.STEP;
            }
        } finally {
            pose.popMatrix();
        }
    }
}
