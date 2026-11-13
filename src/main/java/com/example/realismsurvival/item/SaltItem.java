package com.example.realismsurvival.item;

import net.minecraft.world.item.Item;

/**
 * Byproduct from distillation and furnace evaporation (spec §15.5.1 W-02 / W-03).
 * Sinks: filter cartridges (salt recipe yields 2), salt block storage, salted fish, hide curing
 * ({@code salt + rotten_flesh → leather}), sponge drying, gunpowder.
 */
public final class SaltItem extends Item {

    public SaltItem(Properties properties) {
        super(properties);
    }
}
