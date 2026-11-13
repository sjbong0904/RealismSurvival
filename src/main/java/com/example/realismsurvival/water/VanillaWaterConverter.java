package com.example.realismsurvival.water;

import com.example.realismsurvival.api.water.WaterApi;
import com.example.realismsurvival.api.water.WaterQuality;
import com.example.realismsurvival.component.SurvivalAttachments;
import com.example.realismsurvival.registry.ModBlocks;
import com.example.realismsurvival.registry.ModFluids;
import com.example.realismsurvival.registry.ModWaterQualities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.jspecify.annotations.Nullable;

/**
 * Replaces vanilla {@link Blocks#WATER} with shared graded water + QUALITY, and vanilla
 * {@link Blocks#ICE} with {@link ModBlocks#GRADED_ICE} + QUALITY.
 * Biome resolves salt / swamp stagnant; rivers stay fresh; other inland water becomes stagnant
 * (ponds, lakes, aquifers — not river channels).
 * Also migrates legacy per-grade liquid blocks to {@link ModBlocks#GRADED_WATER}.
 *
 * <p><b>Invariant:</b> open {@link Blocks#WATER} must not remain after conversion / safety-net
 * paths — see SPEC §9.9 and {@code .cursor/rules/no-vanilla-water.mdc}.
 */
public final class VanillaWaterConverter {

    private VanillaWaterConverter() {}

    /** @return true if the block was converted */
    public static boolean tryConvert(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.is(Blocks.WATER)) {
            FluidState old = state.getFluidState();
            if (old.getType() != Fluids.WATER && old.getType() != Fluids.FLOWING_WATER) {
                return false;
            }
            return replaceVanillaInLevel(level, pos, old);
        }
        if (state.is(Blocks.ICE)) {
            return replaceIceInLevel(level, pos);
        }
        if (isLegacyGradedLiquid(state)) {
            return level.setBlock(pos, toCanonical(state), Block.UPDATE_CLIENTS);
        }
        return false;
    }

    /**
     * Worldgen / ProtoChunk path: scan the chunk, write graded fluids without neighbor updates,
     * then mark {@link SurvivalAttachments#WATER_CONVERTED}.
     *
     * @return number of liquid water blocks replaced / migrated
     */
    public static int convertChunkAccess(ChunkAccess chunk) {
        if (chunk.getData(SurvivalAttachments.WATER_CONVERTED)) {
            // Still migrate legacy graded blocks if a world was converted before unification.
            return migrateLegacyInChunk(chunk);
        }
        int[] count = {0};
        chunk.findBlocks(
                state -> state.is(Blocks.WATER) || state.is(Blocks.ICE) || isLegacyGradedLiquid(state),
                (pos, state) -> {
                    if (state.is(Blocks.WATER)) {
                        FluidState old = state.getFluidState();
                        if (old.getType() == Fluids.WATER || old.getType() == Fluids.FLOWING_WATER) {
                            if (replaceVanillaInAccess(chunk, pos, old)) {
                                count[0]++;
                            }
                        }
                    } else if (state.is(Blocks.ICE)) {
                        if (replaceIceInAccess(chunk, pos)) {
                            count[0]++;
                        }
                    } else if (isLegacyGradedLiquid(state)) {
                        chunk.setBlockState(pos, toCanonical(state), 0);
                        count[0]++;
                    }
                }
        );
        chunk.setData(SurvivalAttachments.WATER_CONVERTED, true);
        return count[0];
    }

    /** Legacy load migration: converts every {@link Blocks#WATER} in the chunk. */
    public static int convertChunk(ServerLevel level, LevelChunk chunk) {
        return convertChunkAccess(chunk);
    }

    private static int migrateLegacyInChunk(ChunkAccess chunk) {
        int[] count = {0};
        chunk.findBlocks(
                state -> isLegacyGradedLiquid(state) || state.is(Blocks.ICE),
                (pos, state) -> {
                    if (state.is(Blocks.ICE)) {
                        if (replaceIceInAccess(chunk, pos)) {
                            count[0]++;
                        }
                    } else {
                        chunk.setBlockState(pos, toCanonical(state), 0);
                        count[0]++;
                    }
                }
        );
        return count[0];
    }

    private static boolean replaceVanillaInAccess(ChunkAccess chunk, BlockPos pos, FluidState old) {
        Holder<Biome> biome = chunk.getNoiseBiome(
                QuartPos.fromBlock(pos.getX()),
                QuartPos.fromBlock(pos.getY()),
                QuartPos.fromBlock(pos.getZ())
        );
        WaterQuality quality = prepare(pos, biome, old);
        if (quality == null) {
            return false;
        }
        chunk.setBlockState(pos, createReplacement(old, quality), 0);
        return true;
    }

    private static boolean replaceVanillaInLevel(ServerLevel level, BlockPos pos, FluidState old) {
        Holder<Biome> biome = level.getBiome(pos);
        WaterQuality quality = prepare(pos, biome, old);
        if (quality == null) {
            return false;
        }
        return level.setBlock(pos, createReplacement(old, quality), Block.UPDATE_CLIENTS);
    }

    private static boolean replaceIceInAccess(ChunkAccess chunk, BlockPos pos) {
        Holder<Biome> biome = chunk.getNoiseBiome(
                QuartPos.fromBlock(pos.getX()),
                QuartPos.fromBlock(pos.getY()),
                QuartPos.fromBlock(pos.getZ())
        );
        WaterQuality quality = classifyNaturalWater(biome, WaterApi.resolveFromBiome(biome));
        chunk.setBlockState(pos, GradedIceBlock.withQualityId(WaterQualityStates.idOf(quality)), 0);
        return true;
    }

    private static boolean replaceIceInLevel(ServerLevel level, BlockPos pos) {
        Holder<Biome> biome = level.getBiome(pos);
        WaterQuality quality = classifyNaturalWater(biome, WaterApi.resolveFromBiome(biome));
        return level.setBlock(
                pos,
                GradedIceBlock.withQualityId(WaterQualityStates.idOf(quality)),
                Block.UPDATE_CLIENTS
        );
    }

    private static @Nullable WaterQuality prepare(BlockPos pos, Holder<Biome> biome, FluidState old) {
        WaterQuality resolved = classifyNaturalWater(biome, WaterApi.resolveFromBiome(biome));
        return WaterApi.prepareConversion(pos, biome, old, resolved);
    }

    /**
     * Worldgen / scoop conversion grades:
     * salt biomes → salt; swamp tags → stagnant; river biomes → fresh;
     * default fresh inland (ponds, lakes) → stagnant so drinkable fresh is mainly rivers.
     * Addon qualities other than built-in fresh are left unchanged.
     */
    public static WaterQuality classifyNaturalWater(Holder<Biome> biome, WaterQuality resolved) {
        WaterQuality salt = ModWaterQualities.SALT.get();
        WaterQuality stagnant = ModWaterQualities.STAGNANT.get();
        WaterQuality fresh = ModWaterQualities.FRESH.get();
        if (resolved == salt) {
            return salt;
        }
        if (resolved == stagnant) {
            return stagnant;
        }
        if (biome.is(BiomeTags.IS_RIVER)) {
            return fresh;
        }
        if (resolved == fresh) {
            return stagnant;
        }
        return resolved;
    }

    private static BlockState createReplacement(FluidState old, WaterQuality quality) {
        GradedFlowingFluid fluid = ModFluids.graded();
        boolean falling = old.hasProperty(net.minecraft.world.level.material.FlowingFluid.FALLING)
                && old.getValue(net.minecraft.world.level.material.FlowingFluid.FALLING);
        FluidState replacement = old.isSource()
                ? fluid.sourceOf(quality, falling)
                : fluid.flowingOf(quality, old.getAmount(), falling);
        return replacement.createLegacyBlock();
    }

    static boolean isLegacyGradedLiquid(BlockState state) {
        Block block = state.getBlock();
        return block == ModBlocks.DISTILLED_WATER.get()
                || block == ModBlocks.FRESH_WATER.get()
                || block == ModBlocks.STAGNANT_WATER.get()
                || block == ModBlocks.SALT_WATER.get();
    }

    /** Map legacy block (+ LEVEL/QUALITY) onto canonical {@link ModBlocks#GRADED_WATER}. */
    static BlockState toCanonical(BlockState legacy) {
        int qualityId = legacy.hasProperty(WaterQualityStates.QUALITY)
                ? legacy.getValue(WaterQualityStates.QUALITY)
                : defaultQualityIdForLegacy(legacy.getBlock());
        int level = legacy.hasProperty(LiquidBlock.LEVEL) ? legacy.getValue(LiquidBlock.LEVEL) : 0;
        return ModBlocks.GRADED_WATER.get().defaultBlockState()
                .setValue(LiquidBlock.LEVEL, level)
                .setValue(WaterQualityStates.QUALITY, WaterQualityStates.clampId(qualityId));
    }

    private static int defaultQualityIdForLegacy(Block block) {
        if (block == ModBlocks.DISTILLED_WATER.get()) {
            return WaterQualityStates.idOf(ModWaterQualities.DISTILLED.get());
        }
        if (block == ModBlocks.STAGNANT_WATER.get()) {
            return WaterQualityStates.idOf(ModWaterQualities.STAGNANT.get());
        }
        if (block == ModBlocks.SALT_WATER.get()) {
            return WaterQualityStates.idOf(ModWaterQualities.SALT.get());
        }
        return WaterQualityStates.idOf(ModWaterQualities.FRESH.get());
    }
}
