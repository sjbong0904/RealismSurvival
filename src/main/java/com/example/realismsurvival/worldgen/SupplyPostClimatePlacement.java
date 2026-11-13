package com.example.realismsurvival.worldgen;

import com.example.realismsurvival.api.ModBiomeTags;
import com.example.realismsurvival.registry.ModFeatures;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

/**
 * Climate-weighted supply-post attempts. One roll per position so overlapping tags cannot
 * place two stalls. Ocean (and beaches in that tag) never place. Temperate includes
 * {@code #minecraft:is_forest} so woodlands match plains without editing the season tags.
 * Tropical and every other overworld land biome share the middle rate.
 */
public final class SupplyPostClimatePlacement extends PlacementFilter {

    public static final SupplyPostClimatePlacement INSTANCE = new SupplyPostClimatePlacement();
    public static final MapCodec<SupplyPostClimatePlacement> CODEC = MapCodec.unit(INSTANCE);

    /** Plains, forests, rivers: a short walk. */
    private static final int TEMPERATE = 140;
    /** Desert and badlands. */
    private static final int ARID = 300;
    /** Taiga and snow. */
    private static final int COLD = 400;
    /** Tropics and overworld land that is not in a climate tag. */
    private static final int OTHER = 200;

    private SupplyPostClimatePlacement() {}

    @Override
    protected boolean shouldPlace(PlacementContext context, RandomSource random, BlockPos origin) {
        Holder<Biome> biome = context.getLevel().getBiome(origin);
        if (biome.is(ModBiomeTags.CLIMATE_OCEAN)) {
            return false;
        }
        int chance = chanceFor(biome);
        return random.nextFloat() < 1.0F / chance;
    }

    private static int chanceFor(Holder<Biome> biome) {
        if (biome.is(ModBiomeTags.CLIMATE_TEMPERATE) || biome.is(BiomeTags.IS_FOREST)) {
            return TEMPERATE;
        }
        if (biome.is(ModBiomeTags.CLIMATE_ARID)) {
            return ARID;
        }
        if (biome.is(ModBiomeTags.CLIMATE_COLD)) {
            return COLD;
        }
        return OTHER;
    }

    @Override
    public PlacementModifierType<?> type() {
        return ModFeatures.SUPPLY_POST_CLIMATE.get();
    }
}
