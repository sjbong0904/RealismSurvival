package com.example.realismsurvival.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Single tooltip row of thirst droplets (same sprites as the thirst HUD).
 * Hunger / saturation are left to AppleSkin.
 */
public final class ThirstStatsClientTooltip implements ClientTooltipComponent {

    private static final Identifier THIRST_FULL = RealismSurvivalHUD.SPRITE_FULL;
    private static final Identifier THIRST_HALF = RealismSurvivalHUD.SPRITE_HALF;

    private static final int ICON = HudLayout.ICON_SIZE;
    private static final int STRIDE = HudLayout.ICON_STRIDE;
    private static final int ROW = HudLayout.ROW_HEIGHT;

    private final ThirstStatsTooltip stats;

    public ThirstStatsClientTooltip(ThirstStatsTooltip stats) {
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
        int points = thirstPoints();
        boolean harmful = stats.thirstRestore() < 0.0F;
        int color = harmful ? 0xFFFF_6A_6A : -1;
        int slots = Mth.positiveCeilDiv(points, 2);
        for (int i = 0; i < slots; i++) {
            int ix = x + i * STRIDE;
            int threshold = i * 2 + 1;
            if (threshold < points) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, THIRST_FULL, ix, y, ICON, ICON, color);
            } else if (threshold == points) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, THIRST_HALF, ix, y, ICON, ICON, color);
            }
        }
    }

    private int iconSlots() {
        return Mth.positiveCeilDiv(thirstPoints(), 2);
    }

    /** Map thirst restore (0–100 scale) onto the same 0–20 point icons as the HUD. */
    private int thirstPoints() {
        return Mth.clamp(Math.round(Math.abs(stats.thirstRestore()) / 5.0F), 0, 20);
    }
}
