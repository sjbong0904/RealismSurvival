package com.example.realismsurvival.client;

import com.example.realismsurvival.world.inventory.MachineSlotIcons;
import com.example.realismsurvival.world.inventory.WaterFilterMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

/**
 * Filter GUI composed from vanilla furnace background + {@code container/slot} sprites.
 * Left column: input bucket → <b>filter cartridge</b> → glass bottle; dual outputs on the right.
 */
public class WaterFilterScreen extends AbstractContainerScreen<WaterFilterMenu> {

    private static final Identifier FURNACE_TEXTURE = Identifier.withDefaultNamespace("textures/gui/container/furnace.png");
    private static final Identifier SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot");
    private static final Identifier ARROW_SPRITE = Identifier.withDefaultNamespace("container/furnace/burn_progress");

    public WaterFilterScreen(WaterFilterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        graphics.blit(RenderPipelines.GUI_TEXTURED, FURNACE_TEXTURE, x, y, 0.0F, 0.0F, imageWidth, imageHeight, 256, 256);
        // Erase furnace flame graphic so the cartridge slot reads as a real inventory slot.
        patchPanel(graphics, x, y, 55, 35, 18, 18);
        blitSlot(graphics, x, y, 56, 35);
        blitSlot(graphics, x, y, 56, 53);
        // Cover single furnace result recess; draw two equal output slots side-by-side.
        patchPanel(graphics, x, y, 104, 30, 42, 26);
        blitSlot(graphics, x, y, MachineSlotIcons.OUT_LEFT_X, MachineSlotIcons.OUT_Y);
        blitSlot(graphics, x, y, MachineSlotIcons.OUT_RIGHT_X, MachineSlotIcons.OUT_Y);
        int progressWidth = Mth.ceil(menu.getProgress() * 24.0F);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ARROW_SPRITE, 24, 16, 0, 0, x + 79, y + 34, progressWidth, 16);
    }

    /** Copy a blank panel from the furnace GUI chrome (top inventory area). */
    private static void patchPanel(GuiGraphicsExtractor graphics, int guiX, int guiY, int slotX, int slotY, int w, int h) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, FURNACE_TEXTURE, guiX + slotX, guiY + slotY, 7, 16, w, h, 256, 256);
    }

    private static void blitSlot(GuiGraphicsExtractor graphics, int guiX, int guiY, int slotX, int slotY) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, guiX + slotX - 1, guiY + slotY - 1, 18, 18);
    }
}
