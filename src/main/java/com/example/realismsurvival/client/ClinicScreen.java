package com.example.realismsurvival.client;

import com.example.realismsurvival.clinic.ClinicTreatment;
import com.example.realismsurvival.world.inventory.ClinicMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class ClinicScreen extends AbstractContainerScreen<ClinicMenu> {

    public ClinicScreen(ClinicMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 140);
    }

    @Override
    protected void init() {
        super.init();
        ClinicTreatment[] values = ClinicTreatment.values();
        int y = topPos + 24;
        for (int i = 0; i < values.length; i++) {
            final int ordinal = i;
            ClinicTreatment treatment = values[i];
            addRenderableWidget(Button.builder(
                            Component.translatable(
                                    "clinic.realismsurvival.button",
                                    Component.translatable("clinic.realismsurvival." + treatment.id()),
                                    treatment.cost()),
                            button -> {
                                if (minecraft != null && minecraft.gameMode != null) {
                                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, ordinal);
                                }
                            })
                    .bounds(leftPos + 12, y, 152, 20)
                    .build());
            y += 24;
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int x = leftPos;
        int y = topPos;
        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF2A2A2A);
        graphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFF3E3E3E);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, title, 8, 8, 0xFFFFFF, false);
        ClinicTreatment[] values = ClinicTreatment.values();
        for (int i = 0; i < values.length; i++) {
            int left = menu.usesLeft(i);
            graphics.text(
                    font,
                    Component.translatable("clinic.realismsurvival.uses_left", left),
                    120,
                    28 + i * 24,
                    left > 0 ? 0xFFA0FFA0 : 0xFFFF8080,
                    false);
        }
    }
}
