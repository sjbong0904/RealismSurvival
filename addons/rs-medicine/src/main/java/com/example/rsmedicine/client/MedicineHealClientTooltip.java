package com.example.rsmedicine.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Vanilla HP heart row for medicine heal amounts. */
public final class MedicineHealClientTooltip implements ClientTooltipComponent {

    private static final Identifier HEART_FULL = Identifier.withDefaultNamespace("hud/heart/full");
    private static final Identifier HEART_HALF = Identifier.withDefaultNamespace("hud/heart/half");

    private static final int ICON = 9;
    private static final int STRIDE = 8;
    private static final int ROW = 10;

    private final MedicineHealTooltip stats;

    public MedicineHealClientTooltip(MedicineHealTooltip stats) {
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
        int points = healPoints();
        int slots = Mth.positiveCeilDiv(points, 2);
        for (int i = 0; i < slots; i++) {
            int ix = x + i * STRIDE;
            int threshold = i * 2 + 1;
            if (threshold < points) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HEART_FULL, ix, y, ICON, ICON);
            } else if (threshold == points) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HEART_HALF, ix, y, ICON, ICON);
            }
        }
    }

    private int iconSlots() {
        return Mth.positiveCeilDiv(healPoints(), 2);
    }

    /** Half-heart points (4 HP → 2 full hearts). */
    private int healPoints() {
        return Mth.clamp(Math.round(stats.healAmount()), 0, 40);
    }
}
