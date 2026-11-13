package com.example.rsmedicine.client;

import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;

/** Compact row of mob-effect sprites for cures that all have icons. */
public final class MedicineCureIconsClientTooltip implements ClientTooltipComponent {

    private static final int ICON = 12;
    private static final int STRIDE = 14;
    private static final int ROW = 14;

    private final MedicineCureIconsTooltip stats;

    public MedicineCureIconsClientTooltip(MedicineCureIconsTooltip stats) {
        this.stats = stats;
    }

    @Override
    public int getHeight(Font font) {
        return ROW;
    }

    @Override
    public int getWidth(Font font) {
        int n = stats.effects().size();
        if (n <= 0) {
            return ICON;
        }
        return (n - 1) * STRIDE + ICON;
    }

    @Override
    public void extractImage(Font font, int x, int y, int w, int h, GuiGraphicsExtractor graphics) {
        List<Holder<MobEffect>> effects = stats.effects();
        for (int i = 0; i < effects.size(); i++) {
            Identifier sprite = Gui.getMobEffectSprite(effects.get(i));
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x + i * STRIDE, y, ICON, ICON);
        }
    }
}
