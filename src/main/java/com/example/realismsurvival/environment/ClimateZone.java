package com.example.realismsurvival.environment;

import com.example.realismsurvival.api.ModBiomeTags;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;

/**
 * Coarse climate bands from biome tags. Priority: cold &gt; arid &gt; tropical &gt; ocean &gt; temperate.
 */
public enum ClimateZone {
    COLD,
    ARID,
    TROPICAL,
    OCEAN,
    TEMPERATE;

    public static ClimateZone of(Holder<Biome> biome) {
        if (biome.is(ModBiomeTags.CLIMATE_COLD)) {
            return COLD;
        }
        if (biome.is(ModBiomeTags.CLIMATE_ARID)) {
            return ARID;
        }
        if (biome.is(ModBiomeTags.CLIMATE_TROPICAL)) {
            return TROPICAL;
        }
        if (biome.is(ModBiomeTags.CLIMATE_OCEAN)) {
            return OCEAN;
        }
        return TEMPERATE;
    }
}
