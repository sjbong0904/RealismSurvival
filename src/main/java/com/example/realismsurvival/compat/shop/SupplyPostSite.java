package com.example.realismsurvival.compat.shop;

import com.example.realismsurvival.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;

/**
 * The generated stall is a 5×5 pad, four blocks tall, with the floor included.
 * The marker sits at ground {@code (0, 1, 1)}.
 */
public final class SupplyPostSite {

    private SupplyPostSite() {}

    public static boolean contains(BlockGetter level, BlockPos pos) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = pos.getX() - 2; x <= pos.getX() + 2; x++) {
            for (int y = pos.getY() - 3; y <= pos.getY() + 1; y++) {
                for (int z = pos.getZ() - 1; z <= pos.getZ() + 3; z++) {
                    cursor.set(x, y, z);
                    if (!level.getBlockState(cursor).is(ModBlocks.SUPPLY_POST_MARKER.get())) {
                        continue;
                    }
                    if (inFootprint(cursor, pos)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** {@code pos} is inside the stall owned by {@code marker}. */
    private static boolean inFootprint(BlockPos marker, BlockPos pos) {
        int dx = pos.getX() - marker.getX();
        int dy = pos.getY() - (marker.getY() - 1);
        int dz = pos.getZ() - (marker.getZ() - 1);
        return Math.abs(dx) <= 2 && dz >= -2 && dz <= 2 && dy >= 0 && dy <= 4;
    }
}
