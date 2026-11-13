package com.example.realismsurvival.compat.shop;

import com.example.realismsurvival.compat.ModCompat;
import net.minecraft.resources.Identifier;

/**
 * Soft-presence helpers for the seasonal supply-shop stack.
 * Gameplay code must gate on these before touching Q Shop / SG Economy types.
 */
public final class ShopMods {

    public static final String QSHOP = "qshop";
    public static final String SG_ECONOMY = "sg_economy";
    public static final String EASY_NPC = "easy_npc";
    public static final String CELESTHYD = "celesthyd";

    /** Q Shop currency id kept in sync with the SG Economy inventory gold display. */
    public static final String LINKED_CURRENCY = "coins";

    /** Shop opened by interacting with supply-post NPCs. */
    public static final String SUPPLY_SHOP_ID = "supply_post";

    /** Easy NPC entity scoreboard/tag that opens the supply shop on interact. */
    public static final String NPC_SHOP_TAG = "rs_shop";

    /** Source id used when RS pushes SG balance into Q Shop (event must be ignored). */
    public static final Identifier SOURCE_SG_BRIDGE = Identifier.fromNamespaceAndPath("realismsurvival", "sg_bridge");

    /** Source id for sell-counter payouts credited straight to Q Shop (no SG Economy). */
    public static final Identifier SOURCE_SELL_COUNTER = Identifier.fromNamespaceAndPath("realismsurvival", "sell_counter");

    private ShopMods() {}

    public static boolean qshop() {
        return ModCompat.loaded(QSHOP);
    }

    public static boolean sgEconomy() {
        return ModCompat.loaded(SG_ECONOMY);
    }

    public static boolean easyNpc() {
        return ModCompat.loaded(EASY_NPC);
    }

    /** Bridge is useful only when both wallets exist. */
    public static boolean bridgeReady() {
        return qshop() && sgEconomy();
    }

    public static boolean shopOpenReady() {
        return qshop();
    }
}
