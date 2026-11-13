package com.example.realismsurvival.event;

import com.example.realismsurvival.RealismSurvival;
import com.example.realismsurvival.component.SurvivalAttachments;
import com.example.realismsurvival.water.FreshWaterStagnator;
import com.example.realismsurvival.water.VanillaWaterConverter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Safety net for vanilla water that was not rewritten at worldgen:
 * <ul>
 *   <li>Legacy chunks without {@link SurvivalAttachments#WATER_CONVERTED} (deferred tick convert)</li>
 *   <li>Entity-placed water (buckets, etc.)</li>
 *   <li>Neighbor notifies / ice break that leave vanilla water behind</li>
 * </ul>
 * New chunks are converted in {@code ChunkStatusTasks#initializeLight} before lighting.
 */
@EventBusSubscriber(modid = RealismSurvival.MODID)
public final class WaterWorldHandler {

    private WaterWorldHandler() {}

    /** Soft cap so ocean-heavy login does not hitch a single tick. */
    private static final int CHUNKS_PER_TICK = 2;

    private static final Set<PendingChunk> PENDING = ConcurrentHashMap.newKeySet();

    private record PendingChunk(ResourceKey<Level> dimension, ChunkPos pos) {}

    /** Old chunks still waiting for vanilla → graded conversion in {@code dimension} (measurement, 7.5). */
    public static int pendingChunks(ResourceKey<Level> dimension) {
        int count = 0;
        for (PendingChunk pending : PENDING) {
            if (pending.dimension().equals(dimension)) {
                count++;
            }
        }
        return count;
    }

    @SubscribeEvent
    static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        LevelChunk chunk = event.getChunk();
        if (chunk.getData(SurvivalAttachments.WATER_CONVERTED)) {
            return;
        }
        PENDING.add(new PendingChunk(level.dimension(), chunk.getPos()));
    }

    @SubscribeEvent
    static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        ResourceKey<Level> dim = level.dimension();
        List<PendingChunk> mine = new ArrayList<>();
        for (PendingChunk pending : PENDING) {
            if (pending.dimension().equals(dim)) {
                mine.add(pending);
                if (mine.size() >= CHUNKS_PER_TICK) {
                    break;
                }
            }
        }
        for (PendingChunk pending : mine) {
            PENDING.remove(pending);
            ChunkPos pos = pending.pos();
            if (!level.hasChunk(pos.x(), pos.z())) {
                continue;
            }
            LevelChunk chunk = level.getChunk(pos.x(), pos.z());
            if (chunk.getData(SurvivalAttachments.WATER_CONVERTED)) {
                continue;
            }
            int converted = VanillaWaterConverter.convertChunk(level, chunk);
            chunk.setData(SurvivalAttachments.WATER_CONVERTED, true);
            chunk.markUnsaved();
            if (converted > 0 && RealismSurvival.LOGGER.isDebugEnabled()) {
                RealismSurvival.LOGGER.debug(
                        "[RealismSurvival] Graded {} water blocks in chunk {}",
                        converted,
                        pending.pos()
                );
            }
        }

        FreshWaterStagnator.tick(level);
    }

    @SubscribeEvent
    static void onEntityPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        BlockState placed = event.getPlacedBlock();
        if (!placed.is(Blocks.WATER)) {
            return;
        }
        BlockPos pos = event.getPos().immutable();
        // Run after the place resolves so the liquid block is actually in the world.
        level.getServer().execute(() -> VanillaWaterConverter.tryConvert(level, pos));
    }

    @SubscribeEvent
    static void onNeighborNotify(BlockEvent.NeighborNotifyEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        BlockState state = event.getState();
        if (!state.is(Blocks.WATER)) {
            return;
        }
        if (state.getFluidState().getType() != Fluids.WATER
                && state.getFluidState().getType() != Fluids.FLOWING_WATER) {
            return;
        }
        // Ice melt, sponge release, dispenser spill leftovers, flowing updates, …
        VanillaWaterConverter.tryConvert(level, event.getPos());
    }

    /**
     * Breaking ice / frosted ice can leave vanilla water in the cell (no EntityPlaceEvent).
     * Convert on the next server task after vanilla finishes the break.
     */
    @SubscribeEvent
    static void onBlockBreak(BreakBlockEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        BlockState state = event.getState();
        if (!state.is(Blocks.ICE)
                && !state.is(Blocks.FROSTED_ICE)
                && !state.is(com.example.realismsurvival.registry.ModBlocks.GRADED_ICE.get())) {
            return;
        }
        BlockPos pos = event.getPos().immutable();
        level.getServer().execute(() -> VanillaWaterConverter.tryConvert(level, pos));
    }
}
