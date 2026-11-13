package com.example.realismsurvival.water;

import com.example.realismsurvival.api.water.WaterApi;
import com.example.realismsurvival.api.water.WaterQuality;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

/**
 * Sponge absorption that records absorbed {@link WaterQuality} and places wet sponge with that
 * grade (majority vote; tie → higher {@link WaterQuality#resolvePriority()}).
 */
public final class SpongeAbsorb {

    private static final int MAX_DEPTH = 6;
    private static final int MAX_COUNT = 64;
    private static final Direction[] ALL_DIRECTIONS = Direction.values();

    private SpongeAbsorb() {}

    /**
     * @return {@code true} if water was absorbed and the sponge became wet
     */
    public static boolean tryAbsorb(Level level, BlockPos startPos) {
        if (level.isClientSide()) {
            return false;
        }

        int[] counts = new int[WaterQualityStates.MAX_ID + 1];
        BlockState spongeState = level.getBlockState(startPos);
        int accepted = BlockPos.breadthFirstTraversal(
                startPos,
                MAX_DEPTH,
                MAX_COUNT + 1,
                (pos, consumer) -> {
                    for (Direction direction : ALL_DIRECTIONS) {
                        consumer.accept(pos.relative(direction));
                    }
                },
                pos -> {
                    if (pos.equals(startPos)) {
                        return BlockPos.TraversalNodeStatus.ACCEPT;
                    }

                    BlockState state = level.getBlockState(pos);
                    FluidState fluidState = level.getFluidState(pos);
                    if (!spongeState.canBeHydrated(level, startPos, fluidState, pos)) {
                        return BlockPos.TraversalNodeStatus.SKIP;
                    }

                    if (state.getBlock() instanceof BucketPickup bucketPickup
                            && !bucketPickup.pickupBlock(null, level, pos, state).isEmpty()) {
                        recordQuality(level, pos, fluidState, counts);
                        return BlockPos.TraversalNodeStatus.ACCEPT;
                    }

                    if (state.getBlock() instanceof LiquidBlock) {
                        recordQuality(level, pos, fluidState, counts);
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                        return BlockPos.TraversalNodeStatus.ACCEPT;
                    }

                    if (!state.is(Blocks.KELP)
                            && !state.is(Blocks.KELP_PLANT)
                            && !state.is(Blocks.SEAGRASS)
                            && !state.is(Blocks.TALL_SEAGRASS)) {
                        return BlockPos.TraversalNodeStatus.SKIP;
                    }

                    recordQuality(level, pos, fluidState, counts);
                    BlockEntity blockEntity = state.hasBlockEntity() ? level.getBlockEntity(pos) : null;
                    Block.dropResources(state, level, pos, blockEntity);
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    return BlockPos.TraversalNodeStatus.ACCEPT;
                }
        );

        if (accepted <= 1) {
            return false;
        }

        int qualityId = vote(counts);
        level.setBlock(startPos, WetSpongeItems.blockState(qualityId), 2);
        level.playSound(null, startPos, SoundEvents.SPONGE_ABSORB, SoundSource.BLOCKS, 1.0F, 1.0F);
        return true;
    }

    private static void recordQuality(Level level, BlockPos pos, FluidState fluid, int[] counts) {
        WaterQuality quality = WaterApi.qualityAt(level, pos, fluid);
        int id = WaterQualityStates.idOf(quality);
        counts[id]++;
    }

    /** Majority; ties broken by higher {@link WaterQuality#resolvePriority()}. */
    static int vote(int[] counts) {
        int bestId = WetSpongeItems.DEFAULT_QUALITY_ID;
        int bestCount = -1;
        int bestPriority = Integer.MIN_VALUE;
        boolean any = false;

        for (int id = 0; id < counts.length; id++) {
            int count = counts[id];
            if (count <= 0) {
                continue;
            }
            any = true;
            int priority = WaterQualityStates.byId(id).resolvePriority();
            if (count > bestCount || (count == bestCount && priority > bestPriority)) {
                bestCount = count;
                bestPriority = priority;
                bestId = id;
            }
        }

        return any ? bestId : WetSpongeItems.DEFAULT_QUALITY_ID;
    }
}
