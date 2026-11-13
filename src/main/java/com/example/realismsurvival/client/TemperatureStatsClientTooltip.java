package com.example.realismsurvival.client;

import com.example.realismsurvival.RealismSurvival;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Tooltip row of temperature diamonds (same sprites as the temperature HUD).
 * Warm deltas tint red-orange; cool deltas tint cyan.
 */
public final class TemperatureStatsClientTooltip implements ClientTooltipComponent {

    private static final Identifier TEMP_FULL = RealismSurvival.id("hud/temp_full");
    /** Right-filled half — same dark-left style as hunger/thirst tooltips. */
    private static final Identifier TEMP_HALF = RealismSurvival.id("hud/temp_half_tooltip");

    private static final int ICON = HudLayout.ICON_SIZE;
    private static final int STRIDE = HudLayout.ICON_STRIDE;
    private static final int ROW = HudLayout.ROW_HEIGHT;

    private static final int WARM_COLOR = 0xFFFF_6A_3A;
    private static final int COOL_COLOR = 0xFF3A_C8_FF;

    private final TemperatureStatsTooltip stats;

    public TemperatureStatsClientTooltip(TemperatureStatsTooltip stats) {
        this.stats = stats;
    }

    @Override
    public int getHeight(Font font) {
        return ROW;
    }

    @Override
    public int getWidth(Font font) {
        int slots = iconSlots();
        if (slots <= 0) {
            return ICON;
        }
        return (slots - 1) * STRIDE + ICON;
    }

    @Override
    public void extractImage(Font font, int x, int y, int w, int h, GuiGraphicsExtractor graphics) {
        int points = tempPoints();
        int color = stats.temperatureDelta() < 0.0F ? COOL_COLOR : WARM_COLOR;
        int slots = Mth.positiveCeilDiv(points, 2);
        for (int i = 0; i < slots; i++) {
            int ix = x + i * STRIDE;
            int threshold = i * 2 + 1;
            if (threshold < points) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, TEMP_FULL, ix, y, ICON, ICON, color);
            } else if (threshold == points) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, TEMP_HALF, ix, y, ICON, ICON, color);
            }
        }
    }

    private int iconSlots() {
        return Mth.positiveCeilDiv(tempPoints(), 2);
    }

    /** Map |Δ°C| onto 0–20 icon points (~0.25 °C per point). */
    private int tempPoints() {
        return Mth.clamp(Math.round(Math.abs(stats.temperatureDelta()) * 4.0F), 0, 20);
    }
}
