package com.example.realismsurvival.water;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/** Copies bottle enchantments when filling or crafting graded water bottles. */
public final class BottleEnchantmentTransfer {

    private BottleEnchantmentTransfer() {}

    public static void copyEnchantments(ItemStack from, ItemStack to) {
        ItemEnchantments enchantments = EnchantmentHelper.getEnchantmentsForCrafting(from);
        if (!enchantments.isEmpty()) {
            to.set(DataComponents.ENCHANTMENTS, enchantments);
        }
    }
}
