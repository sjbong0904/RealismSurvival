package com.example.realismsurvival.api;

import com.example.realismsurvival.RealismSurvival;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/** Public block tags for addon integration (spec §15.3). */
public final class ModBlockTags {

    private ModBlockTags() {}

    /**
     * Blocks that contribute ambient heat near the player.
     * Unknown tagged blocks use a default contribution; known vanillas keep weighted strengths.
     */
    public static final TagKey<Block> HEAT_SOURCES = TagKey.create(Registries.BLOCK, RealismSurvival.id("heat_sources"));

    /**
     * Blocks that cool ambient temperature when nearby (ice / snow / addon cryo coils).
     * Core applies weighted hardcodes for vanilla ice/snow/water plus this tag fallback.
     */
    public static final TagKey<Block> COLD_SOURCES = TagKey.create(Registries.BLOCK, RealismSurvival.id("cold_sources"));

    /**
     * Blocks that count as roof / tent / tarp for {@link com.example.realismsurvival.environment.EnvironmentProbe}
     * even if they would not normally occlude (e.g. wool, leaves used as shelter).
     */
    public static final TagKey<Block> SHELTER_BLOCKS = TagKey.create(Registries.BLOCK, RealismSurvival.id("shelter_blocks"));

    /** Soft comfort surfaces (beds, carpets, furniture) for rest / spa addons to query. */
    public static final TagKey<Block> COMFORT_BLOCKS = TagKey.create(Registries.BLOCK, RealismSurvival.id("comfort_blocks"));
}
