package com.example.realismsurvival.compat.shop;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

/** Per-player daily buy counts for the supply chest shop. */
public final class SupplyBuyLedger {

    private static final String ROOT = "realismsurvival_supply_buy";
    private static final String DAY = "day";
    private static final String BOUGHT = "bought";

    private SupplyBuyLedger() {}

    public static int boughtToday(ServerPlayer player, String itemId) {
        CompoundTag root = player.getPersistentData().getCompoundOrEmpty(ROOT);
        if (root.getLongOr(DAY, Long.MIN_VALUE) != today(player)) {
            return 0;
        }
        return root.getCompoundOrEmpty(BOUGHT).getIntOr(itemId, 0);
    }

    static void record(ServerPlayer player, String itemId, int count) {
        if (count <= 0) {
            return;
        }
        CompoundTag data = player.getPersistentData();
        CompoundTag root = data.getCompoundOrEmpty(ROOT);
        long day = today(player);
        CompoundTag counts = root.getLongOr(DAY, Long.MIN_VALUE) == day
                ? root.getCompoundOrEmpty(BOUGHT)
                : new CompoundTag();
        counts.putInt(itemId, counts.getIntOr(itemId, 0) + count);
        root.putLong(DAY, day);
        root.put(BOUGHT, counts);
        data.put(ROOT, root);
    }

    private static long today(ServerPlayer player) {
        return Math.floorDiv(player.level().getOverworldClockTime(), 24_000L);
    }
}
