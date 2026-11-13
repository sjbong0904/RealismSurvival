package com.example.realismsurvival.compat.shop;

import net.minecraft.world.level.Level;

/**
 * Shop age. Earlier goods stay on the shelf; each step only adds a higher band.
 * Days are overworld days ({@code clock / 24000}), so the steps land inside the first Serene year.
 */
public enum SupplyEra {
    PLAIN(0, "기초"),
    IRON(12, "철"),
    GOLD(32, "금"),
    DIAMOND(64, "다이아"),
    NETHERITE(96, "네더라이트");

    private final int minDay;
    private final String label;

    SupplyEra(int minDay, String label) {
        this.minDay = minDay;
        this.label = label;
    }

    public String label() {
        return label;
    }

    public boolean atLeast(SupplyEra era) {
        return ordinal() >= era.ordinal();
    }

    public static SupplyEra fromLevel(Level level) {
        long day = Math.floorDiv(level.getOverworldClockTime(), 24_000L);
        if (day < 0L) {
            day = 0L;
        }
        SupplyEra era = PLAIN;
        for (SupplyEra candidate : values()) {
            if (day >= candidate.minDay) {
                era = candidate;
            }
        }
        return era;
    }
}
