package com.example.realismsurvival.client;

import com.example.realismsurvival.world.inventory.MachineSlotIcons;
import com.example.realismsurvival.world.inventory.WaterStillMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

/**
 * Still GUI composed from vanilla furnace background + {@code container/slot} sprites
 * for empty-container and dual horizontal outputs.
 */
public class WaterStillScreen extends AbstractContainerScreen<WaterStillMenu> {

    private static final Identifier FURNACE_TEXTURE = Identifier.withDefaultNamespace("textures/gui/container/furnace.png");
    private static final Identifier SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot");
    private static final Identifier LIT_SPRITE = Identifier.withDefaultNamespace("container/furnace/lit_progress");
    private static final Identifier ARROW_SPRITE = Identifier.withDefaultNamespace("container/furnace/burn_progress");

    public WaterStillScreen(WaterStillMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        graphics.blit(RenderPipelines.GUI_TEXTURED, FURNACE_TEXTURE, x, y, 0.0F, 0.0F, imageWidth, imageHeight, 256, 256);
        blitSlot(graphics, x, y, 26, 35);
        // Cover single furnace result recess; draw two equal output slots side-by-side.
        graphics.blit(RenderPipelines.GUI_TEXTURED, FURNACE_TEXTURE, x + 104, y + 30, 7, 14, 42, 26, 256, 256);
        blitSlot(graphics, x, y, MachineSlotIcons.OUT_LEFT_X, MachineSlotIcons.OUT_Y);
        blitSlot(graphics, x, y, MachineSlotIcons.OUT_RIGHT_X, MachineSlotIcons.OUT_Y);
        if (menu.isLit()) {
            int litHeight = Mth.ceil(menu.getLitProgress() * 13.0F) + 1;
            graphics.blitSprite(
                    RenderPipelines.GUI_TEXTURED,
                    LIT_SPRITE,
                    14,
                    14,
                    0,
                    14 - litHeight,
                    x + 56,
                    y + 36 + 14 - litHeight,
                    14,
                    litHeight
            );
        }
        int progressWidth = Mth.ceil(menu.getBurnProgress() * 24.0F);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ARROW_SPRITE, 24, 16, 0, 0, x + 79, y + 34, progressWidth, 16);
    }

    private static void blitSlot(GuiGraphicsExtractor graphics, int guiX, int guiY, int slotX, int slotY) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, guiX + slotX - 1, guiY + slotY - 1, 18, 18);
    }
}
