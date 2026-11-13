package com.example.realismsurvival.client;

import com.example.realismsurvival.compat.shop.SupplyPricePolicy.Reason;
import com.example.realismsurvival.world.inventory.SupplyCounterMenu;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Sell counter on the same chest chrome as buy. The 3×3 inner slots are real; back / gold / confirm
 * are chest icons. Slot tint and tooltip carry the server quote.
 */
public class SupplyCounterScreen extends SupplyChestScreen<SupplyCounterMenu> {

    public SupplyCounterScreen(SupplyCounterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        super.extractLabels(graphics, xm, ym);
        for (int i = 0; i < SupplyCounterMenu.COUNTER_SLOTS; i++) {
            Slot slot = menu.counterSlot(i);
            Reason reason = menu.reason(i);
            int tint = switch (reason) {
                case OK -> 0x5533CC33;
                case PARTIAL -> 0x66E0C030;
                case EMPTY -> 0;
                default -> 0x66CC3333;
            };
            if (tint != 0 && slot.hasItem()) {
                graphics.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, tint);
            }
        }
    }

    @Override
    protected List<Component> getTooltipFromContainerItem(ItemStack itemStack) {
        List<Component> lines = new ArrayList<>(super.getTooltipFromContainerItem(itemStack));
        Slot slot = hoveredSlot;
        if (slot == null) {
            return lines;
        }
        int index = menu.sellIndex(slot.index);
        if (index < 0) {
            return lines;
        }
        Reason reason = menu.reason(index);
        int cents = menu.unitCents(index);
        if (cents > 0) {
            lines.add(Component.translatable(
                            "gui.realismsurvival.supply_counter.unit",
                            String.format(Locale.ROOT, "%d.%02d", cents / 100, cents % 100))
                    .withStyle(ChatFormatting.GRAY));
        }
        if (menu.offSpecialty(index) && (reason == Reason.OK || reason == Reason.PARTIAL || reason == Reason.LIMIT)) {
            lines.add(Component.translatable("gui.realismsurvival.supply_counter.off_specialty")
                    .withStyle(ChatFormatting.GOLD));
        }
        ChatFormatting color = reason == Reason.OK
                ? ChatFormatting.GREEN
                : reason == Reason.PARTIAL ? ChatFormatting.YELLOW : ChatFormatting.RED;
        lines.add(Component.translatable(
                        "gui.realismsurvival.supply_counter.reason." + reason.name().toLowerCase(Locale.ROOT),
                        menu.accepted(index))
                .withStyle(color));
        return lines;
    }
}
