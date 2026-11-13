package com.example.realismsurvival.compat.shop;

import com.example.realismsurvival.environment.ClimateZone;
import com.example.realismsurvival.world.inventory.SupplyCounterMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

/**
 * Supply sell counter: the player lays goods on temporary slots, the server quotes them, and one confirm
 * removes exactly the accepted items and pays once. Unaccepted items stay on the counter and return to the
 * player when the menu closes.
 */
public final class SupplyCounterTrade {

    private SupplyCounterTrade() {}

    public static boolean available() {
        return SeasonalSupplyShop.counterEnabled() && SgQShopCurrencyBridge.walletAvailable();
    }

    public static boolean open(ServerPlayer player, Entity merchant) {
        if (!available() || merchant == null) {
            return false;
        }
        ClimateZone climate = ClimateZone.of(player.level().getBiome(merchant.blockPosition()));
        SupplyCategory category = SupplyCategory.fromEntity(merchant);
        Component title = Component.translatable(
                "container.realismsurvival.shop.sell",
                SeasonalSupplyShop.climateName(climate),
                category.label());
        player.openMenu(new SimpleMenuProvider(
                (id, inventory, p) -> new SupplyCounterMenu(id, inventory, player, merchant, climate, category),
                title));
        return true;
    }

    public static SupplyCounterQuote quote(
            ServerPlayer player, Container counter, ClimateZone climate, SupplyCategory category) {
        return SupplyCounterQuote.compute(
                player, counter, SeasonalSupplyShop.buybackRates(player.level(), climate, category), category);
    }

    /** Re-quotes on the server, removes accepted items, pays; restores the counter if payment fails. */
    public static void confirm(ServerPlayer player, Container counter, ClimateZone climate, SupplyCategory category) {
        if (!SgQShopCurrencyBridge.walletAvailable()) {
            player.sendSystemMessage(Component.translatable("message.realismsurvival.supply_counter.no_wallet"), true);
            return;
        }
        SupplyCounterQuote quote = quote(player, counter, climate, category);
        if (quote.totalCoins() <= 0L) {
            player.sendSystemMessage(Component.translatable("message.realismsurvival.supply_counter.nothing"), true);
            return;
        }

        ItemStack[] before = new ItemStack[counter.getContainerSize()];
        int soldItems = 0;
        for (int i = 0; i < before.length; i++) {
            before[i] = counter.getItem(i).copy();
            int accepted = quote.slots().get(i).accepted();
            if (accepted > 0) {
                counter.removeItem(i, accepted);
                soldItems += accepted;
            }
        }

        if (!SgQShopCurrencyBridge.deposit(player, quote.totalCoins())) {
            for (int i = 0; i < before.length; i++) {
                counter.setItem(i, before[i]);
            }
            player.sendSystemMessage(Component.translatable("message.realismsurvival.supply_counter.failed"), true);
            return;
        }
        SupplyCounterLedger.record(player, quote.soldByItem());
        counter.setChanged();
        player.sendSystemMessage(Component.translatable(
                "message.realismsurvival.supply_counter.sold", soldItems, quote.totalCoins()));
    }
}
