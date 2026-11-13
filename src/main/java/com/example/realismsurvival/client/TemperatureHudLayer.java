package com.example.realismsurvival.client;

import com.example.realismsurvival.RealismSurvival;
import com.example.realismsurvival.RealismSurvivalConfig;
import com.example.realismsurvival.component.SurvivalAttachments;
import com.example.realismsurvival.component.SurvivalData;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.gui.GuiLayer;

/**
 * Body-temperature row above HP (spec §8.7): ten diamonds like hunger/thirst.
 *
 * <p>Left→right fill; each slot is tinted blue→red. Normal body temp (36.5 °C) lights
 * exactly the first five diamonds (10/20 points).
 */
public final class TemperatureHudLayer implements GuiLayer {

    public static final Identifier LAYER_ID = RealismSurvival.id("temperature_level");

    private static final Identifier SPRITE_EMPTY = RealismSurvival.id("hud/temp_empty");
    private static final Identifier SPRITE_FULL = RealismSurvival.id("hud/temp_full");
    private static final Identifier SPRITE_HALF = RealismSurvival.id("hud/temp_half");

    private TemperatureHudLayer() {}

    public static TemperatureHudLayer create() {
        return new TemperatureHudLayer();
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (!RealismSurvivalConfig.TEMPERATURE_ENABLED.getAsBoolean()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Player player = HudLayout.cameraPlayer(minecraft);
        if (player == null) {
            return;
        }

        int points = SurvivalAttachments.get(player).temperaturePoints();

        Gui gui = minecraft.gui;
        int xLeft = graphics.guiWidth() / 2 - HudLayout.STATUS_BAR_HALF_WIDTH;
        int y = graphics.guiHeight() - gui.leftHeight;

        for (int i = 0; i < HudLayout.ICON_COUNT; i++) {
            int x = xLeft + i * HudLayout.ICON_STRIDE;
            int threshold = i * 2 + 1;
            int color = diamondRgb(i);

            graphics.blitSprite(
                    RenderPipelines.GUI_TEXTURED,
                    SPRITE_EMPTY,
                    x,
                    y,
                    HudLayout.ICON_SIZE,
                    HudLayout.ICON_SIZE
            );
            if (threshold < points) {
                graphics.blitSprite(
                        RenderPipelines.GUI_TEXTURED,
                        SPRITE_FULL,
                        x,
                        y,
                        HudLayout.ICON_SIZE,
                        HudLayout.ICON_SIZE,
                        color
                );
            } else if (threshold == points) {
                graphics.blitSprite(
                        RenderPipelines.GUI_TEXTURED,
                        SPRITE_HALF,
                        x,
                        y,
                        HudLayout.ICON_SIZE,
                        HudLayout.ICON_SIZE,
                        color
                );
            }
        }

        gui.leftHeight += HudLayout.ROW_HEIGHT;
    }

    /** Slot 0 = cold blue, slot 9 = hot red (ARGB). */
    static int diamondRgb(int slotIndex) {
        float t = HudLayout.ICON_COUNT <= 1 ? 0.0F : slotIndex / (float) (HudLayout.ICON_COUNT - 1);
        int r;
        int g;
        int b;
        if (t < 0.5F) {
            float u = t / 0.5F;
            r = Math.round(Mth.lerp(u, 0x3A, 0xE8));
            g = Math.round(Mth.lerp(u, 0xB0, 0xE8));
            b = Math.round(Mth.lerp(u, 0xFF, 0xE8));
        } else {
            float u = (t - 0.5F) / 0.5F;
            r = Math.round(Mth.lerp(u, 0xE8, 0xFF));
            g = Math.round(Mth.lerp(u, 0xE8, 0x5A));
            b = Math.round(Mth.lerp(u, 0xE8, 0x3A));
        }
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
