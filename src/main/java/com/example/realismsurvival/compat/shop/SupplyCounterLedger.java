package com.example.realismsurvival.compat.shop;

import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

/**
 * Per-player daily sell counts for the supply counter, kept in player persistent data.
 * A "day" is an overworld day ({@code clock / 24000}), matching {@link SupplyEra}.
 */
final class SupplyCounterLedger {

    private static final String ROOT = "realismsurvival_supply_counter";
    private static final String DAY = "day";
    private static final String SOLD = "sold";

    private SupplyCounterLedger() {}

    static int soldToday(ServerPlayer player, String itemId) {
        CompoundTag root = player.getPersistentData().getCompoundOrEmpty(ROOT);
        if (root.getLongOr(DAY, Long.MIN_VALUE) != today(player)) {
            return 0;
        }
        return root.getCompoundOrEmpty(SOLD).getIntOr(itemId, 0);
    }

    static void record(ServerPlayer player, Map<String, Integer> sold) {
        CompoundTag data = player.getPersistentData();
        CompoundTag root = data.getCompoundOrEmpty(ROOT);
        long day = today(player);
        CompoundTag counts = root.getLongOr(DAY, Long.MIN_VALUE) == day
                ? root.getCompoundOrEmpty(SOLD)
                : new CompoundTag();
        for (Map.Entry<String, Integer> entry : sold.entrySet()) {
            counts.putInt(entry.getKey(), counts.getIntOr(entry.getKey(), 0) + entry.getValue());
        }
        root.putLong(DAY, day);
        root.put(SOLD, counts);
        data.put(ROOT, root);
    }

    private static long today(ServerPlayer player) {
        return Math.floorDiv(player.level().getOverworldClockTime(), 24_000L);
    }
}
