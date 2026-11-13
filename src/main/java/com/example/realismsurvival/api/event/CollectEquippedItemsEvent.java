package com.example.realismsurvival.api.event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingEvent;

/**
 * Fired when the core gathers worn/equipped item stacks for survival modifiers (insulation, …).
 * Core already adds armor and (when present) Curios. Addons may {@link #add(ItemStack)} stacks from
 * other accessory systems (Trinkets forks, custom inventories, …).
 *
 * <p>Empty stacks are ignored. Duplicate references are allowed; insulation sums per stack entry.
 */
public final class CollectEquippedItemsEvent extends LivingEvent {

    private final List<ItemStack> items = new ArrayList<>();

    public CollectEquippedItemsEvent(LivingEntity entity) {
        super(entity);
    }

    public void add(ItemStack stack) {
        if (stack != null && !stack.isEmpty()) {
            items.add(stack);
        }
    }

    public void addAll(Iterable<ItemStack> stacks) {
        for (ItemStack stack : stacks) {
            add(stack);
        }
    }

    /** Mutable live list (addons may also clear/reorder if needed). */
    public List<ItemStack> getItems() {
        return items;
    }

    public List<ItemStack> getItemsView() {
        return Collections.unmodifiableList(items);
    }
}
