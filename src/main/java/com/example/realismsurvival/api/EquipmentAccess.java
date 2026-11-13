package com.example.realismsurvival.api;

import com.example.realismsurvival.api.compat.CuriosAccess;
import com.example.realismsurvival.api.event.CollectEquippedItemsEvent;
import java.util.List;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Collects item stacks that count as “worn” for survival modifiers.
 * Armor + optional Curios + {@link CollectEquippedItemsEvent} for other accessory mods.
 */
public final class EquipmentAccess {

    private EquipmentAccess() {}

    public static List<ItemStack> collectEquipped(LivingEntity entity) {
        CollectEquippedItemsEvent event = new CollectEquippedItemsEvent(entity);
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR) {
                continue;
            }
            event.add(entity.getItemBySlot(slot));
        }
        CuriosAccess.forEachEquipped(entity, event::add);
        NeoForge.EVENT_BUS.post(event);
        return event.getItems();
    }
}
