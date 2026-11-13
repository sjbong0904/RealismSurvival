package com.example.realismsurvival.api;

import com.example.realismsurvival.RealismSurvival;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

/** Public biome tags for water-quality resolution (addons may add biomes via datapack). */
public final class ModBiomeTags {

    private ModBiomeTags() {}

    /**
     * Biomes that resolve to salt water when no higher-priority quality matches.
     * Includes vanilla ocean/beach tags plus {@code stony_shore} (not in {@code #is_beach}).
     */
    public static final TagKey<Biome> SALT_WATER = TagKey.create(Registries.BIOME, RealismSurvival.id("salt_water"));

    /** Biomes that resolve to stagnant water when no higher-priority quality matches. */
    public static final TagKey<Biome> STAGNANT_WATER = TagKey.create(Registries.BIOME, RealismSurvival.id("stagnant_water"));

    // ---- Climate zones (season offsets / precip) --------------------------------------------------------------------

    public static final TagKey<Biome> CLIMATE_TEMPERATE =
            TagKey.create(Registries.BIOME, RealismSurvival.id("climate_temperate"));
    public static final TagKey<Biome> CLIMATE_COLD =
            TagKey.create(Registries.BIOME, RealismSurvival.id("climate_cold"));
    public static final TagKey<Biome> CLIMATE_ARID =
            TagKey.create(Registries.BIOME, RealismSurvival.id("climate_arid"));
    public static final TagKey<Biome> CLIMATE_TROPICAL =
            TagKey.create(Registries.BIOME, RealismSurvival.id("climate_tropical"));
    public static final TagKey<Biome> CLIMATE_OCEAN =
            TagKey.create(Registries.BIOME, RealismSurvival.id("climate_ocean"));
}
