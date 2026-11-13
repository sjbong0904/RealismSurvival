package com.example.realismsurvival.water;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Live → dead coral without stripping waterlogging.
 *
 * <p>Vanilla sets {@code WATERLOGGED=false} on death, which deletes the fluid in that cell.
 * Neighboring graded water then reflows / waterlogs and looks like flow “resets”. Keeping the
 * live block’s waterlogged flag avoids the hole.
 */
public final class CoralDeath {

    private CoralDeath() {}

    public static void replaceWithDead(ServerLevel level, BlockPos pos, BlockState live, Block deadBlock) {
        BlockState dead = deadBlock.defaultBlockState();
        if (live.hasProperty(BlockStateProperties.WATERLOGGED) && dead.hasProperty(BlockStateProperties.WATERLOGGED)) {
            dead = dead.setValue(BlockStateProperties.WATERLOGGED, live.getValue(BlockStateProperties.WATERLOGGED));
        }
        if (live.hasProperty(BlockStateProperties.FACING) && dead.hasProperty(BlockStateProperties.FACING)) {
            dead = dead.setValue(BlockStateProperties.FACING, live.getValue(BlockStateProperties.FACING));
        }
        // Same flags as vanilla coral tick (client sync, no neighbor flood).
        level.setBlock(pos, dead, Block.UPDATE_CLIENTS);
    }
}
