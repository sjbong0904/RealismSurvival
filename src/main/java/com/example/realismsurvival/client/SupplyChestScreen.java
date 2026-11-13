package com.example.realismsurvival.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Shared vanilla double-chest chrome for buy and sell. */
abstract class SupplyChestScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {

    private static final Identifier BACKGROUND = Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");
    static final int ROWS = 6;

    SupplyChestScreen(T menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 114 + ROWS * 18);
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, x, y, 0.0F, 0.0F, imageWidth, ROWS * 18 + 17, 256, 256);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED, BACKGROUND, x, y + ROWS * 18 + 17, 0.0F, 126.0F, imageWidth, 96, 256, 256);
    }
}
