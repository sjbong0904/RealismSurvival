package com.example.realismsurvival.compat.shop;

import com.example.realismsurvival.environment.ClimateZone;
import com.example.realismsurvival.world.inventory.SupplyShopMenu;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Chest-style supply shop: hub → buy catalog or sell counter. Purchases are server-authoritative.
 */
public final class SupplyShopTrade {

    private SupplyShopTrade() {}

    public static boolean available() {
        return SgQShopCurrencyBridge.walletAvailable();
    }

    public static boolean openHub(ServerPlayer player, Entity merchant) {
        if (!available() || merchant == null) {
            if (player != null) {
                player.sendSystemMessage(Component.translatable("message.realismsurvival.supply_counter.no_wallet"), true);
            }
            return false;
        }
        ClimateZone climate = ClimateZone.of(player.level().getBiome(merchant.blockPosition()));
        SupplyCategory category = SupplyCategory.fromEntity(merchant);
        player.openMenu(new SimpleMenuProvider(
                (id, inventory, p) -> SupplyShopMenu.hub(id, inventory, player, merchant, climate, category),
                title(climate, category, false)));
        return true;
    }

    public static boolean openBuy(ServerPlayer player, Entity merchant) {
        if (!available() || merchant == null) {
            return false;
        }
        ClimateZone climate = ClimateZone.of(player.level().getBiome(merchant.blockPosition()));
        SupplyCategory category = SupplyCategory.fromEntity(merchant);
        player.openMenu(new SimpleMenuProvider(
                (id, inventory, p) -> SupplyShopMenu.buy(id, inventory, player, merchant, climate, category, 0),
                title(climate, category, true)));
        return true;
    }

    public static void openBuyPage(ServerPlayer player, Entity merchant, ClimateZone climate, SupplyCategory category, int page) {
        player.openMenu(new SimpleMenuProvider(
                (id, inventory, p) -> SupplyShopMenu.buy(id, inventory, player, merchant, climate, category, page),
                title(climate, category, true)));
    }

    public static Component title(ClimateZone climate, SupplyCategory category, boolean buy) {
        String key = buy ? "container.realismsurvival.shop.buy" : "container.realismsurvival.shop";
        return Component.translatable(key, SeasonalSupplyShop.climateName(climate), category.label());
    }

    public static boolean purchase(ServerPlayer player, SeasonalSupplyShop.BuyOffer offer, int times) {
        if (times <= 0 || offer == null) {
            return false;
        }
        Item item = BuiltInRegistries.ITEM.getValue(offer.itemId());
        if (item == null || item == Items.AIR) {
            return false;
        }
        int remaining = offer.dailyUnits() - SupplyBuyLedger.boughtToday(player, offer.itemId().toString());
        int canBuy = Math.min(times, remaining / offer.count());
        if (canBuy <= 0) {
            player.sendSystemMessage(Component.translatable("message.realismsurvival.shop.limit"), true);
            return false;
        }
        double cost = offer.price() * (double) canBuy;
        if (!SgQShopCurrencyBridge.hasBalance(player, cost)) {
            player.sendSystemMessage(Component.translatable("message.realismsurvival.shop.cannot_afford", offer.price()), true);
            return false;
        }
        if (!SgQShopCurrencyBridge.withdraw(player, cost)) {
            player.sendSystemMessage(Component.translatable("message.realismsurvival.shop.cannot_afford", offer.price()), true);
            return false;
        }
        int items = offer.count() * canBuy;
        ItemStack stack = new ItemStack(item, items);
        player.getInventory().add(stack);
        if (!stack.isEmpty()) {
            player.drop(stack, false);
        }
        SupplyBuyLedger.record(player, offer.itemId().toString(), items);
        player.sendSystemMessage(Component.translatable("message.realismsurvival.shop.bought", items, (int) Math.round(cost)));
        return true;
    }
}
