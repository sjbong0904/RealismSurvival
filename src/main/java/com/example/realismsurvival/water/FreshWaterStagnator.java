package com.example.realismsurvival.water;

import com.example.realismsurvival.RealismSurvivalConfig;
import com.example.realismsurvival.api.water.WaterApi;
import com.example.realismsurvival.api.water.WaterQuality;
import com.example.realismsurvival.registry.ModBlocks;
import com.example.realismsurvival.registry.ModFluids;
import com.example.realismsurvival.registry.ModWaterQualities;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.FluidState;
import org.jspecify.annotations.Nullable;

/**
 * Small fresh-water source pools stagnate after a grace period proportional to pool size
 * (spec §9.9). Connected fresh sources ≥ threshold never stagnate.
 */
public final class FreshWaterStagnator {

    private FreshWaterStagnator() {}

    public static int threshold() {
        return RealismSurvivalConfig.FRESH_POOL_STAGNATE_THRESHOLD.getAsInt();
    }

    public static int graceTicksPerSource() {
        return RealismSurvivalConfig.FRESH_POOL_GRACE_TICKS_PER_SOURCE.getAsInt();
    }

    /** Grace duration for a pool of the given source count ({@link Long#MAX_VALUE} if immune). */
    public static long graceTicksForSize(int size) {
        if (size <= 0 || size >= threshold()) {
            return Long.MAX_VALUE;
        }
        return (long) size * graceTicksPerSource();
    }

    public static boolean shouldStagnate(int size, long ageTicks) {
        if (size <= 0 || size >= threshold()) {
            return false;
        }
        return ageTicks >= graceTicksForSize(size);
    }

    public static boolean isFreshSource(FluidState fluid) {
        return fluid.isSource()
                && WaterApi.isGradedWaterFluid(fluid.getType())
                && WaterApi.fromFluidState(fluid) == ModWaterQualities.FRESH.get();
    }

    public static boolean isFreshSource(ServerLevel level, BlockPos pos) {
        return isFreshSource(level.getFluidState(pos));
    }

    /** Register a freshly placed / observed fresh source for the grace clock. */
    public static void noticeFreshSource(ServerLevel level, BlockPos pos) {
        if (!RealismSurvivalConfig.FRESH_POOL_STAGNATION_ENABLED.getAsBoolean()) {
            return;
        }
        if (!isFreshSource(level, pos)) {
            return;
        }
        FreshPoolDecayData.get(level).notice(pos, level.getGameTime());
    }

    /**
     * Periodic pass over ticking chunks + tracked cells.
     *
     * @return number of pools converted to stagnant
     */
    public static int tick(ServerLevel level) {
        if (!RealismSurvivalConfig.FRESH_POOL_STAGNATION_ENABLED.getAsBoolean()) {
            return 0;
        }
        long gameTime = level.getGameTime();
        int interval = RealismSurvivalConfig.FRESH_POOL_SCAN_INTERVAL_TICKS.getAsInt();
        if (gameTime % interval != 0) {
            return 0;
        }

        int budget = RealismSurvivalConfig.FRESH_POOL_CHUNKS_PER_SCAN.getAsInt();
        LongOpenHashSet visited = new LongOpenHashSet();
        AtomicInteger converted = new AtomicInteger();

        List<ChunkPos> candidates = collectScanChunks(level);
        if (!candidates.isEmpty()) {
            int start = (int) ((gameTime / interval) % candidates.size());
            for (int i = 0; i < Math.min(budget, candidates.size()); i++) {
                ChunkPos chunkPos = candidates.get(Math.floorMod(start + i, candidates.size()));
                if (!level.hasChunk(chunkPos.x(), chunkPos.z())) {
                    continue;
                }
                LevelChunk chunk = level.getChunk(chunkPos.x(), chunkPos.z());
                scanChunk(level, chunk, visited, gameTime, converted);
            }
        }

        converted.addAndGet(scanTracked(level, visited, gameTime));
        return converted.get();
    }

    /** Chunks near players (entity-ticking vicinity) — ChunkMap internals are package-private. */
    private static List<ChunkPos> collectScanChunks(ServerLevel level) {
        LongOpenHashSet seen = new LongOpenHashSet();
        List<ChunkPos> out = new ArrayList<>();
        for (ServerPlayer player : level.players()) {
            ChunkPos center = player.chunkPosition();
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    ChunkPos pos = new ChunkPos(center.x() + dx, center.z() + dz);
                    if (seen.add(pos.pack())) {
                        out.add(pos);
                    }
                }
            }
        }
        return out;
    }

    /** Evaluate the pool containing {@code origin} immediately (GameTests / commands). */
    public static boolean processPoolAt(ServerLevel level, BlockPos origin) {
        if (!isFreshSource(level, origin)) {
            return false;
        }
        Pool pool = collectPool(level, origin);
        if (pool == null) {
            return false;
        }
        return evaluatePool(level, pool, level.getGameTime());
    }

    private static void scanChunk(
            ServerLevel level,
            LevelChunk chunk,
            LongOpenHashSet visited,
            long gameTime,
            AtomicInteger converted
    ) {
        int freshId = WaterQualityStates.idOf(ModWaterQualities.FRESH.get());
        chunk.findBlocks(
                state -> isFreshSourceBlock(state, freshId),
                (pos, state) -> processSeed(level, pos.immutable(), visited, gameTime, converted)
        );
    }

    private static int scanTracked(ServerLevel level, LongOpenHashSet visited, long gameTime) {
        FreshPoolDecayData data = FreshPoolDecayData.get(level);
        AtomicInteger converted = new AtomicInteger();
        long[] keys = data.map().keySet().toLongArray();
        for (long key : keys) {
            if (visited.contains(key)) {
                continue;
            }
            BlockPos pos = BlockPos.of(key);
            if (!level.isLoaded(pos)) {
                continue;
            }
            if (!isFreshSource(level, pos)) {
                data.removeKey(key);
                continue;
            }
            processSeed(level, pos, visited, gameTime, converted);
        }
        return converted.get();
    }

    private static void processSeed(
            ServerLevel level,
            BlockPos seed,
            LongOpenHashSet visited,
            long gameTime,
            AtomicInteger converted
    ) {
        long key = seed.asLong();
        if (!visited.add(key)) {
            return;
        }
        if (!isFreshSource(level, seed)) {
            return;
        }
        Pool pool = collectPool(level, seed);
        if (pool == null) {
            return;
        }
        for (BlockPos pos : pool.sources()) {
            visited.add(pos.asLong());
        }
        if (evaluatePool(level, pool, gameTime)) {
            converted.incrementAndGet();
        }
    }

    private static boolean evaluatePool(ServerLevel level, Pool pool, long gameTime) {
        FreshPoolDecayData data = FreshPoolDecayData.get(level);
        if (pool.immune() || pool.sources().size() >= threshold()) {
            data.removeAll(pool.sources());
            return false;
        }

        long firstSeen = Long.MAX_VALUE;
        for (BlockPos pos : pool.sources()) {
            data.notice(pos, gameTime);
            long seen = data.getFirstSeen(pos);
            if (seen != Long.MIN_VALUE && seen < firstSeen) {
                firstSeen = seen;
            }
        }
        if (firstSeen == Long.MAX_VALUE) {
            firstSeen = gameTime;
        }

        long age = Math.max(0L, gameTime - firstSeen);
        if (!shouldStagnate(pool.sources().size(), age)) {
            return false;
        }

        convertToStagnant(level, pool.sources());
        data.removeAll(pool.sources());
        return true;
    }

    public static void convertToStagnant(ServerLevel level, List<BlockPos> sources) {
        WaterQuality stagnant = ModWaterQualities.STAGNANT.get();
        BlockState stagnantBlock = ModFluids.graded().sourceOf(stagnant, false).createLegacyBlock();
        for (BlockPos pos : sources) {
            if (!isFreshSource(level, pos)) {
                continue;
            }
            level.setBlock(pos, stagnantBlock, Block.UPDATE_ALL);
        }
    }

    /**
     * BFS connected fresh sources from {@code origin}.
     *
     * @return pool, or {@code null} if origin is not a fresh source
     */
    public static @Nullable Pool collectPool(ServerLevel level, BlockPos origin) {
        if (!isFreshSource(level, origin)) {
            return null;
        }
        int limit = threshold();
        List<BlockPos> sources = new ArrayList<>();
        LongOpenHashSet seen = new LongOpenHashSet();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(origin.immutable());
        seen.add(origin.asLong());

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        while (!queue.isEmpty()) {
            BlockPos pos = queue.removeFirst();
            sources.add(pos);
            if (sources.size() >= limit) {
                return new Pool(sources, true);
            }
            for (Direction direction : Direction.values()) {
                cursor.setWithOffset(pos, direction);
                long nextKey = BlockPos.asLong(cursor.getX(), cursor.getY(), cursor.getZ());
                if (!seen.add(nextKey)) {
                    continue;
                }
                if (!level.isLoaded(cursor)) {
                    continue;
                }
                if (isFreshSource(level, cursor)) {
                    queue.add(cursor.immutable());
                }
            }
        }
        return new Pool(sources, false);
    }

    private static boolean isFreshSourceBlock(BlockState state, int freshId) {
        if (!state.is(ModBlocks.GRADED_WATER.get())
                && !state.is(ModBlocks.FRESH_WATER.get())
                && !state.is(ModBlocks.DISTILLED_WATER.get())
                && !state.is(ModBlocks.STAGNANT_WATER.get())
                && !state.is(ModBlocks.SALT_WATER.get())) {
            return false;
        }
        if (!state.hasProperty(LiquidBlock.LEVEL) || !state.hasProperty(WaterQualityStates.QUALITY)) {
            return false;
        }
        return state.getValue(LiquidBlock.LEVEL) == 0
                && state.getValue(WaterQualityStates.QUALITY) == freshId;
    }

    /** Connected fresh-source component. {@code immune} when BFS hit the threshold. */
    public record Pool(List<BlockPos> sources, boolean immune) {}
}
