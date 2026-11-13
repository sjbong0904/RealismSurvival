package com.example.realismsurvival.client.sodium;

import com.example.realismsurvival.client.WaterTintBiome;
import com.example.realismsurvival.mixin.sodium.LevelSliceBiomeAccess;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import org.jspecify.annotations.Nullable;

/** Resolves biomes inside Sodium's chunk compile slice so waterlogged plants match graded water. */
public final class SodiumWaterTint {

    private SodiumWaterTint() {}

    public static void register() {
        WaterTintBiome.setLookup(SodiumWaterTint::biomeOnSlice);
    }

    private static @Nullable Holder<Biome> biomeOnSlice(BlockAndTintGetter level, BlockPos pos) {
        if (!(level instanceof LevelSliceBiomeAccess access)) {
            return null;
        }
        try {
            return access.realismsurvival$biomeSlice().getBiome(pos.getX(), pos.getY(), pos.getZ());
        } catch (ArrayIndexOutOfBoundsException outOfSlice) {
            // Grade blend samples past the biome window Sodium keeps for one section.
            return null;
        }
    }
}
