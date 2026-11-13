package com.example.realismsurvival.world.inventory;

import it.unimi.dsi.fastutil.objects.ReferenceSortedSets;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.TooltipDisplay;

/** Chest-shop icon slot: visible, not takeable, not fillable. */
public final class DisplaySlot extends Slot {

    public DisplaySlot(Container container, int slot, int x, int y) {
        super(container, slot, x, y);
    }

    static ItemStack pane() {
        ItemStack stack = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
        stack.set(DataComponents.ITEM_NAME, Component.literal(" "));
        stack.set(DataComponents.TOOLTIP_DISPLAY, new TooltipDisplay(true, ReferenceSortedSets.emptySet()));
        return stack;
    }

    static ItemStack named(ItemStack stack, Component name, List<Component> lore) {
        stack.set(DataComponents.ITEM_NAME, name);
        if (!lore.isEmpty()) {
            stack.set(DataComponents.LORE, new ItemLore(lore, lore));
        }
        return stack;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return false;
    }

    @Override
    public boolean mayPickup(Player player) {
        return false;
    }

    @Override
    public boolean isFake() {
        return true;
    }
}
