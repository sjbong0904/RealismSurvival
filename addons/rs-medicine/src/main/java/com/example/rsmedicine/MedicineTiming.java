package com.example.rsmedicine;

import net.minecraft.util.RandomSource;

/** Shared 1–8s uniform duration for afflictions and bonus buffs. */
public final class MedicineTiming {

    public static final int MIN_TICKS = 20;
    public static final int MAX_TICKS = 160;

    private MedicineTiming() {}

    public static int randomDurationTicks(RandomSource random) {
        return random.nextInt(MIN_TICKS, MAX_TICKS + 1);
    }
}
