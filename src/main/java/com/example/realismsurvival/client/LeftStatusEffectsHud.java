package com.example.realismsurvival.client;

import com.google.common.collect.Ordering;
import java.util.Collection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.neoforge.client.extensions.common.IClientMobEffectExtensions;

/**
 * Vanilla status-effect icons, left-anchored so the FTB Chunks minimap keeps the top-right.
 *
 * <p>Same sprites, blink, and beneficial/harmful rows as {@code Gui.extractEffects}; only the X origin
 * moves from {@code guiWidth() - 25 * n} to {@code 1 + 25 * (n - 1)}.
 */
public final class LeftStatusEffectsHud {

    private static final Identifier BACKGROUND_AMBIENT =
            Identifier.withDefaultNamespace("hud/effect_background_ambient");
    private static final Identifier BACKGROUND = Identifier.withDefaultNamespace("hud/effect_background");

    private static final int ICON_STRIDE = 25;
    private static final int ORIGIN_X = 1;
    private static final int ORIGIN_Y = 1;
    private static final int HARMFUL_ROW_OFFSET = 26;
    private static final int DEMO_OFFSET = 15;

    private LeftStatusEffectsHud() {}

    public static void render(GuiGraphicsExtractor graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        Collection<MobEffectInstance> activeEffects = minecraft.player.getActiveEffects();
        if (activeEffects.isEmpty() || (minecraft.screen != null && minecraft.screen.showsActiveEffects())) {
            return;
        }

        Gui gui = minecraft.gui;
        int beneficialCount = 0;
        int harmfulCount = 0;

        for (MobEffectInstance instance : Ordering.natural().reverse().sortedCopy(activeEffects)) {
            var renderer = IClientMobEffectExtensions.of(instance);
            if (!renderer.isVisibleInGui(instance) || !instance.showIcon()) {
                continue;
            }

            Holder<MobEffect> effect = instance.getEffect();
            int y = ORIGIN_Y;
            if (minecraft.isDemo()) {
                y += DEMO_OFFSET;
            }

            int x;
            if (effect.value().isBeneficial()) {
                beneficialCount++;
                x = ORIGIN_X + ICON_STRIDE * (beneficialCount - 1);
            } else {
                harmfulCount++;
                x = ORIGIN_X + ICON_STRIDE * (harmfulCount - 1);
                y += HARMFUL_ROW_OFFSET;
            }

            float alpha = 1.0F;
            if (instance.isAmbient()) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND_AMBIENT, x, y, 24, 24);
            } else {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND, x, y, 24, 24);
                if (instance.endsWithin(200)) {
                    int remainingDuration = instance.getDuration();
                    int usedSeconds = 10 - remainingDuration / 20;
                    alpha = Mth.clamp(remainingDuration / 10.0F / 5.0F * 0.5F, 0.0F, 0.5F)
                            + Mth.cos(remainingDuration * (float) Math.PI / 5.0F)
                                    * Mth.clamp(usedSeconds / 10.0F * 0.25F, 0.0F, 0.25F);
                    alpha = Mth.clamp(alpha, 0.0F, 1.0F);
                }
            }

            @SuppressWarnings("removal")
            boolean customIcon = renderer.renderGuiIcon(instance, gui, graphics, x, y, 0, alpha);
            if (customIcon) {
                continue;
            }
            graphics.blitSprite(
                    RenderPipelines.GUI_TEXTURED,
                    Gui.getMobEffectSprite(effect),
                    x + 3,
                    y + 3,
                    18,
                    18,
                    ARGB.white(alpha));
        }
    }
}
