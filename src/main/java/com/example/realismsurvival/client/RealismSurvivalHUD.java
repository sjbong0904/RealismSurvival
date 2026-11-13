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
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.GuiLayer;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * Thirst meter (right column) plus registration glue for the temporary HUD suite
 * (thirst / temperature / armor borders — spec §8.5–8.7).
 *
 * <p>Uses the mod's custom 9x9 thirst sprites from the GUI atlas.
 * Layers no-op when the matching server config toggle is off (synced SERVER config).
 */
public final class RealismSurvivalHUD implements GuiLayer {

    public static final Identifier LAYER_ID = RealismSurvival.id("thirst_level");

    static final Identifier SPRITE_FULL = RealismSurvival.id("hud/thirst_full");
    static final Identifier SPRITE_HALF = RealismSurvival.id("hud/thirst_half");
    static final Identifier SPRITE_EMPTY = RealismSurvival.id("hud/thirst_empty");

    private RealismSurvivalHUD() {}

    /**
     * Registers thirst, temperature, armor-row suppression, HP border overlay, left-anchored
     * status effects, and fallback instruments.
     * Call once from {@link com.example.realismsurvival.RealismSurvivalClient}.
     */
    public static void register(RegisterGuiLayersEvent event) {
        // 1) When temperature HUD is on, suppress vanilla armor icons (temp uses that row).
        //    When off, keep the vanilla armor layer.
        event.wrapLayer(VanillaGuiLayers.ARMOR_LEVEL, previous -> (graphics, delta) -> {
            if (RealismSurvivalConfig.TEMPERATURE_ENABLED.getAsBoolean()) {
                return;
            }
            previous.render(graphics, delta);
        });

        // 2) Material-tinted armor on hearts only while temperature HUD owns the armor slot.
        event.wrapLayer(VanillaGuiLayers.PLAYER_HEALTH, previous -> (graphics, delta) -> {
            previous.render(graphics, delta);
            if (RealismSurvivalConfig.TEMPERATURE_ENABLED.getAsBoolean()) {
                ArmorHeartOverlay.render(graphics);
            }
        });

        // 3) Temperature sits where armor used to (above HP once leftHeight has advanced).
        event.registerAbove(VanillaGuiLayers.ARMOR_LEVEL, TemperatureHudLayer.LAYER_ID, TemperatureHudLayer.create());

        // 4) Thirst above hunger (existing behaviour).
        event.registerAbove(VanillaGuiLayers.FOOD_LEVEL, LAYER_ID, new RealismSurvivalHUD());

        // 5) Status effects top-left so they don't sit on the FTB minimap (top-right).
        event.wrapLayer(VanillaGuiLayers.EFFECTS, previous -> (graphics, delta) -> LeftStatusEffectsHud.render(graphics));

        // 6) Instruments: clock + ambient. Skips itself when FTB minimap owns those lines.
        event.registerAboveAll(InstrumentsHudLayer.LAYER_ID, InstrumentsHudLayer.create());
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (!RealismSurvivalConfig.THIRST_ENABLED.getAsBoolean()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Player player = HudLayout.cameraPlayer(minecraft);
        if (player == null) {
            return;
        }

        SurvivalData data = SurvivalAttachments.get(player);
        int points = data.thirstPoints();

        Gui gui = minecraft.gui;
        int xRight = graphics.guiWidth() / 2 + HudLayout.STATUS_BAR_HALF_WIDTH;
        int y = graphics.guiHeight() - gui.rightHeight;

        for (int i = 0; i < HudLayout.ICON_COUNT; i++) {
            int x = xRight - i * HudLayout.ICON_STRIDE - HudLayout.ICON_SIZE;
            int threshold = i * 2 + 1;

            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SPRITE_EMPTY, x, y, HudLayout.ICON_SIZE, HudLayout.ICON_SIZE);
            if (threshold < points) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SPRITE_FULL, x, y, HudLayout.ICON_SIZE, HudLayout.ICON_SIZE);
            } else if (threshold == points) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SPRITE_HALF, x, y, HudLayout.ICON_SIZE, HudLayout.ICON_SIZE);
            }
        }

        gui.rightHeight += HudLayout.ROW_HEIGHT;
    }
}
