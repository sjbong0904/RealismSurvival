package com.example.realismsurvival.water;

import com.example.realismsurvival.api.water.WaterQuality;

/**
 * Client item-model tint colors for graded water (RGB, no alpha).
 * Used by {@code assets/.../items/*_water_*.json} {@code minecraft:constant} tints — keep in sync.
 */
public final class WaterColors {

    private WaterColors() {}

    /** Pale / clear — distilled. */
    public static final int DISTILLED = 0xB8E8FF;
    /** Close to vanilla water-potion blue. */
    public static final int FRESH = 0x385DC6;
    /** Murky swamp green. */
    public static final int STAGNANT = 0x6B7A32;
    /** Deeper sea teal. */
    public static final int SALT = 0x1F6B8C;

    public static int of(WaterQuality quality) {
        return quality.tintRgb();
    }
}
