package com.example.realismsurvival.client;

import java.util.Objects;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import org.jspecify.annotations.Nullable;

/**
 * Biome at a fluid cell for water tint.
 * <p>Chunk meshing (Sodium's {@code LevelSlice}) is a {@link BlockAndTintGetter} and not a
 * {@link LevelReader}, so {@code getBiome} is missing there. Waterlogged kelp, seagrass, and
 * mod water plants still report {@code minecraft:water}; without a biome they fall back to the
 * fresh-water blue. An optional lookup covers that slice.
 */
public final class WaterTintBiome {

    @FunctionalInterface
    public interface Lookup {
        @Nullable Holder<Biome> get(BlockAndTintGetter level, BlockPos pos);
    }

    private static volatile Lookup extra = (level, pos) -> null;

    private WaterTintBiome() {}

    public static void setLookup(Lookup lookup) {
        extra = Objects.requireNonNull(lookup);
    }

    static @Nullable Holder<Biome> get(@Nullable BlockAndTintGetter level, BlockPos pos) {
        if (level == null) {
            return null;
        }
        if (level instanceof LevelReader reader) {
            return reader.getBiome(pos);
        }
        return extra.get(level, pos);
    }
}
