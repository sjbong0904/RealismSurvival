package com.example.realismsurvival.water;

import com.example.realismsurvival.api.water.WaterApi;
import com.example.realismsurvival.api.water.WaterQuality;
import com.example.realismsurvival.registry.ModWaterQualities;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;

/**
 * Encodes {@link WaterQuality} as an int on fluid / liquid-block state (0..{@link #MAX_ID}).
 */
public final class WaterQualityStates {

    /** Inclusive max; enough for built-ins + a handful of addon grades. */
    public static final int MAX_ID = 15;

    public static final IntegerProperty QUALITY = IntegerProperty.create("quality", 0, MAX_ID);

    private WaterQualityStates() {}

    public static int idOf(WaterQuality quality) {
        int id = WaterApi.registry().getId(quality);
        if (id < 0 || id > MAX_ID) {
            return idOf(ModWaterQualities.FRESH.get());
        }
        return id;
    }

    public static WaterQuality byId(int id) {
        WaterQuality quality = WaterApi.registry().byId(id);
        return quality != null ? quality : ModWaterQualities.FRESH.get();
    }

    public static int qualityId(FluidState state) {
        if (state.hasProperty(QUALITY)) {
            return state.getValue(QUALITY);
        }
        return idOf(ModWaterQualities.FRESH.get());
    }

    public static FluidState withQuality(FluidState state, WaterQuality quality) {
        if (!state.hasProperty(QUALITY)) {
            return state;
        }
        return state.setValue(QUALITY, idOf(quality));
    }

    public static FluidState withQualityId(FluidState state, int qualityId) {
        if (!state.hasProperty(QUALITY)) {
            return state;
        }
        return state.setValue(QUALITY, clampId(qualityId));
    }

    public static int clampId(int id) {
        if (id < 0 || id > MAX_ID) {
            return idOf(ModWaterQualities.FRESH.get());
        }
        return id;
    }

    public static boolean sameQuality(FluidState a, FluidState b) {
        return qualityId(a) == qualityId(b);
    }
}
