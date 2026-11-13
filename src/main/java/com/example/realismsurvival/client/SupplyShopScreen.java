package com.example.realismsurvival.client;

import com.example.realismsurvival.world.inventory.SupplyShopMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Hub and buy catalog: Skript-style chest icons on vanilla double-chest chrome. */
public class SupplyShopScreen extends SupplyChestScreen<SupplyShopMenu> {

    public SupplyShopScreen(SupplyShopMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }
}
