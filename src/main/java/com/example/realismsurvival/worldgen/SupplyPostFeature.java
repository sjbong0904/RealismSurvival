package com.example.realismsurvival.worldgen;

import com.example.realismsurvival.blockentity.SupplyPostMarkerBlockEntity;
import com.example.realismsurvival.compat.shop.SupplyCategory;
import com.example.realismsurvival.environment.ClimateZone;
import com.example.realismsurvival.registry.ModBlocks;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Small dry stall (no open water). Climate changes the shell — gable, parapet, snow hut, or leaf canopy —
 * and the merchant category changes the plan: counter, lean-to, alcove, or lockup.
 * Places a {@code supply_post_marker}.
 */
public final class SupplyPostFeature extends Feature<NoneFeatureConfiguration> {

    private static final StallLook OAK = look(
            Blocks.STRIPPED_OAK_LOG.defaultBlockState(),
            Blocks.OAK_PLANKS.defaultBlockState(),
            Blocks.OAK_FENCE.defaultBlockState(),
            Blocks.OAK_PLANKS.defaultBlockState(),
            Blocks.OAK_STAIRS.defaultBlockState(),
            Blocks.OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM),
            Blocks.STONE_BRICKS.defaultBlockState(),
            Blocks.LANTERN.defaultBlockState(),
            Blocks.OAK_LEAVES.defaultBlockState());
    /** One block of rise across the pad is shaved flat. Steeper pads are skipped. */
    private static final int SLOPE = 1;
    /** Clears through the roof peak. The merchant stands in y=1..2. */
    private static final int HEADROOM = 4;
    /** If the rolled column is a slope, look for a shelf a few blocks away in the same attempt. */
    private static final int[][] SHIFTS = {{0, 0}, {4, 0}, {-4, 0}, {0, 4}, {0, -4}};

    public SupplyPostFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        SupplyCategory category = SupplyCategory.roll(context.random());
        for (int[] shift : SHIFTS) {
            if (tryPlace(level, origin.offset(shift[0], 0, shift[1]), category)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Floor sits on the lowest solid column, so nothing is bridged. A column one block higher
     * is cut down to that floor and replaced with planks; the block under every plank stays terrain.
     */
    private static boolean tryPlace(WorldGenLevel level, BlockPos origin, SupplyCategory category) {
        BlockPos center = findGround(level, origin);
        if (center == null) {
            return false;
        }

        int[] surfaces = new int[25];
        int index = 0;
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        int hint = center.getY() - 1;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                int surface = surfaceY(level, center.getX() + dx, center.getZ() + dz, hint);
                if (surface == Integer.MIN_VALUE) {
                    return false;
                }
                surfaces[index++] = surface;
                min = Math.min(min, surface);
                max = Math.max(max, surface);
            }
        }
        if (max - min > SLOPE) {
            return false;
        }

        BlockPos ground = new BlockPos(center.getX(), min + 1, center.getZ());
        if (!padIsDry(level, ground, surfaces)) {
            return false;
        }
        StallLook look = lookFor(level, ground);
        ClimateZone zone = ClimateZone.of(level.getBiome(ground));

        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                BlockPos at = ground.offset(dx, 0, dz);
                level.setBlock(at, Math.abs(dx) == 2 || Math.abs(dz) == 2 ? look.floor() : look.deck(), 2);
                clearColumn(level, at.above(), HEADROOM);
            }
        }
        switch (category) {
            case FOOD -> buildFood(level, ground, look, zone);
            case WEAPONS -> buildWeapons(level, ground, look, zone);
            case POTIONS -> buildPotions(level, ground, look, zone);
            case MINERALS -> buildMinerals(level, ground, look, zone);
        }

        BlockPos markerPos = ground.offset(0, 1, 1);
        level.setBlock(markerPos, ModBlocks.SUPPLY_POST_MARKER.get().defaultBlockState(), 2);
        if (level.getBlockEntity(markerPos) instanceof SupplyPostMarkerBlockEntity marker) {
            marker.setCategory(category);
        }
        return true;
    }

    /** Open counter. Temperate gable, arid parapet, cold hut, tropical leaf roof. */
    private static void buildFood(WorldGenLevel level, BlockPos ground, StallLook look, ClimateZone zone) {
        corners(level, ground, look, zone == ClimateZone.TROPICAL ? 3 : 2);
        put(level, ground, 0, 1, -1, south(look.stair()));
        put(level, ground, -1, 1, -1, Blocks.COMPOSTER.defaultBlockState());
        put(level, ground, 1, 1, -1, Blocks.BARREL.defaultBlockState());
        put(level, ground, -2, 1, 0, Blocks.HAY_BLOCK.defaultBlockState());
        switch (zone) {
            case ARID -> {
                flatRoof(level, ground, look.slab());
                parapet(level, ground, look.wall());
                put(level, ground, -2, 1, -2, Blocks.ORANGE_TERRACOTTA.defaultBlockState());
                put(level, ground, 2, 1, -2, Blocks.ORANGE_TERRACOTTA.defaultBlockState());
            }
            case COLD -> {
                sideWalls(level, ground, look.wall(), 2);
                gable(level, ground, look.wall(), Blocks.SNOW_BLOCK.defaultBlockState());
                chimney(level, ground, look.heavy());
            }
            case TROPICAL -> canopy(level, ground, look);
            default -> {
                gable(level, ground, look.wall(), look.slab());
                awning(level, ground, look);
            }
        }
        hangLight(level, ground, look);
    }

    /** Back-weighted lean-to. Cold closes the sides, arid raises a wind wall, tropical stays a rack. */
    private static void buildWeapons(WorldGenLevel level, BlockPos ground, StallLook look, ClimateZone zone) {
        boolean open = zone == ClimateZone.TROPICAL;
        BlockState back = open ? look.post() : look.wall();
        for (int x = -2; x <= 2; x++) {
            put(level, ground, x, 1, -2, back);
            put(level, ground, x, 2, -2, open ? look.post() : look.wall());
        }
        if (zone == ClimateZone.ARID) {
            for (int x = -2; x <= 2; x++) {
                put(level, ground, x, 3, -2, look.wall());
            }
        }
        if (zone == ClimateZone.COLD) {
            for (int z = -1; z <= 0; z++) {
                put(level, ground, -2, 1, z, look.wall());
                put(level, ground, 2, 1, z, look.wall());
                put(level, ground, -2, 2, z, look.wall());
                put(level, ground, 2, 2, z, look.wall());
            }
            put(level, ground, 1, 1, 0, Blocks.CAMPFIRE.defaultBlockState()
                    .setValue(CampfireBlock.LIT, false)
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH));
        } else {
            put(level, ground, -2, 1, -1, look.wall());
            put(level, ground, -2, 2, -1, look.post());
        }
        put(level, ground, 0, 1, -1, south(Blocks.GRINDSTONE.defaultBlockState()
                .setValue(FaceAttachedHorizontalDirectionalBlock.FACE, AttachFace.FLOOR)));
        put(level, ground, 1, 1, -1, Blocks.SMITHING_TABLE.defaultBlockState());
        put(level, ground, -1, 1, -1, open ? Blocks.FLETCHING_TABLE.defaultBlockState() : south(Blocks.ANVIL.defaultBlockState()));
        BlockState roof = zone == ClimateZone.TROPICAL ? look.leaf() : zone == ClimateZone.COLD ? Blocks.SNOW_BLOCK.defaultBlockState() : look.slab();
        for (int x = -2; x <= 2; x++) {
            for (int z = -1; z <= 0; z++) {
                put(level, ground, x, 3, z, roof);
            }
        }
        if (zone != ClimateZone.ARID && zone != ClimateZone.TROPICAL) {
            for (int x = -2; x <= 2; x++) {
                put(level, ground, x, 3, 0, look.floor());
            }
        }
        hangLight(level, ground, look);
    }

    /** Three-sided alcove. Cold fills the walls, tropical drops them to rails, arid keeps a solid band. */
    private static void buildPotions(WorldGenLevel level, BlockPos ground, StallLook look, ClimateZone zone) {
        int wallHeight = zone == ClimateZone.TROPICAL ? 1 : 2;
        BlockState upper = switch (zone) {
            case COLD -> look.wall();
            case ARID -> Blocks.ORANGE_TERRACOTTA.defaultBlockState();
            case TROPICAL -> look.post();
            default -> look.post();
        };
        for (int x = -2; x <= 2; x++) {
            put(level, ground, x, 1, -2, look.wall());
            if (wallHeight > 1) {
                put(level, ground, x, 2, -2, upper);
            }
        }
        for (int z = -1; z <= 1; z++) {
            put(level, ground, -2, 1, z, look.wall());
            put(level, ground, 2, 1, z, look.wall());
            if (wallHeight > 1) {
                put(level, ground, -2, 2, z, upper);
                put(level, ground, 2, 2, z, upper);
            }
        }
        put(level, ground, 0, 1, -1, Blocks.BREWING_STAND.defaultBlockState());
        put(level, ground, -1, 1, -1, Blocks.CAULDRON.defaultBlockState());
        put(level, ground, 1, 1, -1, south(look.stair()));
        switch (zone) {
            case ARID -> {
                flatRoof(level, ground, look.slab());
                parapet(level, ground, look.wall());
            }
            case COLD -> {
                flatRoof(level, ground, Blocks.SNOW_BLOCK.defaultBlockState());
                chimney(level, ground, look.heavy());
            }
            case TROPICAL -> canopy(level, ground, look);
            default -> {
                gable(level, ground, look.wall(), look.slab());
                awning(level, ground, look);
            }
        }
        hangLight(level, ground, look);
    }

    /** Knee-high lockup with a door gap. Cold stacks the wall, arid raises a parapet, tropical opens the top. */
    private static void buildMinerals(WorldGenLevel level, BlockPos ground, StallLook look, ClimateZone zone) {
        BlockState wall = look.heavy();
        int height = zone == ClimateZone.COLD || zone == ClimateZone.ARID ? 2 : 1;
        for (int x = -2; x <= 2; x++) {
            for (int y = 1; y <= height; y++) {
                put(level, ground, x, y, -2, wall);
                if (Math.abs(x) > 1) {
                    put(level, ground, x, y, 2, wall);
                }
            }
        }
        for (int z = -1; z <= 1; z++) {
            for (int y = 1; y <= height; y++) {
                put(level, ground, -2, y, z, wall);
                put(level, ground, 2, y, z, wall);
            }
        }
        if (height == 1 && zone != ClimateZone.TROPICAL) {
            for (int x = -2; x <= 2; x++) {
                if (Math.abs(x) > 1) {
                    put(level, ground, x, 2, -2, look.post());
                    put(level, ground, x, 2, 2, look.post());
                }
            }
        }
        put(level, ground, 0, 1, -1, south(Blocks.STONECUTTER.defaultBlockState()));
        put(level, ground, 1, 1, -1, Blocks.BARREL.defaultBlockState());
        BlockState sample = switch (zone) {
            case ARID -> Blocks.RAW_COPPER_BLOCK.defaultBlockState();
            case COLD -> Blocks.RAW_IRON_BLOCK.defaultBlockState();
            case TROPICAL -> Blocks.MOSS_BLOCK.defaultBlockState();
            default -> Blocks.COAL_BLOCK.defaultBlockState();
        };
        put(level, ground, -1, 1, -1, sample);
        switch (zone) {
            case ARID -> {
                flatRoof(level, ground, look.slab());
                parapet(level, ground, look.wall());
            }
            case COLD -> {
                flatRoof(level, ground, Blocks.SNOW_BLOCK.defaultBlockState());
                chimney(level, ground, Blocks.COBBLESTONE.defaultBlockState());
            }
            case TROPICAL -> canopy(level, ground, look);
            default -> flatRoof(level, ground, look.slab());
        }
        hangLight(level, ground, look);
    }

    private static void corners(WorldGenLevel level, BlockPos ground, StallLook look, int height) {
        int[][] corners = {{-2, -2}, {-2, 2}, {2, -2}, {2, 2}};
        for (int[] corner : corners) {
            for (int y = 1; y <= height; y++) {
                put(level, ground, corner[0], y, corner[1], look.post());
            }
        }
    }

    private static void sideWalls(WorldGenLevel level, BlockPos ground, BlockState wall, int height) {
        for (int z = -1; z <= 1; z++) {
            for (int y = 1; y <= height; y++) {
                put(level, ground, -2, y, z, wall);
                put(level, ground, 2, y, z, wall);
            }
        }
    }

    /** Ridge of full blocks, eaves of slabs, one peak. */
    private static void gable(WorldGenLevel level, BlockPos ground, BlockState ridge, BlockState eave) {
        for (int z = -2; z <= 2; z++) {
            put(level, ground, 0, 3, z, ridge);
            put(level, ground, -1, 3, z, eave);
            put(level, ground, 1, 3, z, eave);
        }
        put(level, ground, 0, 4, 0, ridge);
        put(level, ground, 0, 4, -1, ridge);
    }

    private static void flatRoof(WorldGenLevel level, BlockPos ground, BlockState roof) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                put(level, ground, x, 3, z, roof);
            }
        }
    }

    /** Desert screen: a raised rim with the front middle left open. */
    private static void parapet(WorldGenLevel level, BlockPos ground, BlockState wall) {
        for (int x = -2; x <= 2; x++) {
            put(level, ground, x, 3, -2, wall);
            if (Math.abs(x) > 1) {
                put(level, ground, x, 3, 2, wall);
            }
        }
        for (int z = -1; z <= 1; z++) {
            put(level, ground, -2, 3, z, wall);
            put(level, ground, 2, 3, z, wall);
        }
    }

    private static void canopy(WorldGenLevel level, BlockPos ground, StallLook look) {
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                if (Math.abs(x) == 2 && Math.abs(z) == 2) {
                    continue;
                }
                put(level, ground, x, 3, z, look.leaf());
            }
        }
        put(level, ground, 0, 4, 0, look.leaf());
    }

    /** Front lintel. Logs do not pop without a side attachment. */
    private static void awning(WorldGenLevel level, BlockPos ground, StallLook look) {
        for (int x = -1; x <= 1; x++) {
            put(level, ground, x, 2, 2, look.floor());
        }
    }

    private static void chimney(WorldGenLevel level, BlockPos ground, BlockState stack) {
        put(level, ground, 2, 1, -2, stack);
        put(level, ground, 2, 2, -2, stack);
        put(level, ground, 2, 3, -2, stack);
        put(level, ground, 2, 4, -2, Blocks.SNOW_BLOCK.defaultBlockState());
    }

    private static void hangLight(WorldGenLevel level, BlockPos ground, StallLook look) {
        if (level.getBlockState(ground.offset(2, 2, 2)).isAir()) {
            put(level, ground, 2, 1, 2, look.post());
            put(level, ground, 2, 2, 2, look.post());
        }
        put(level, ground, 2, 3, 2, look.light());
    }

    private static void put(WorldGenLevel level, BlockPos ground, int x, int y, int z, BlockState state) {
        if (Math.abs(x) > 2 || Math.abs(z) > 2 || y < 1 || y > 4) {
            return;
        }
        level.setBlock(ground.offset(x, y, z), state, 2);
    }

    private static BlockState south(BlockState state) {
        return state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                ? state.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH)
                : state;
    }

    private static BlockPos findGround(WorldGenLevel level, BlockPos origin) {
        BlockPos.MutableBlockPos cursor = origin.mutable();
        while (cursor.getY() < level.getMaxY() - 4 && isTerrain(level, cursor)) {
            cursor.move(Direction.UP);
        }
        while (cursor.getY() > level.getMinY() + 4 && !isTerrain(level, cursor)) {
            cursor.move(Direction.DOWN);
        }
        if (!isTerrain(level, cursor)) {
            return null;
        }
        BlockPos above = cursor.above();
        if (!level.getFluidState(above).isEmpty()) {
            return null;
        }
        return above;
    }

    /** Solid ground that is not a trunk, canopy, or ice. */
    private static boolean isTerrain(WorldGenLevel level, BlockPos pos) {
        if (!level.getFluidState(pos).isEmpty()) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        return state.isSolid()
                && !state.is(BlockTags.LEAVES)
                && !state.is(BlockTags.LOGS)
                && !state.is(BlockTags.ICE);
    }

    /**
     * Top of the terrain in this column, within {@link #SLOPE} of {@code hint}.
     * Logs and leaves are skipped so a trunk does not lift the stall into the canopy.
     * Any fluid in the searched span rejects the column.
     */
    private static int surfaceY(WorldGenLevel level, int x, int z, int hint) {
        for (int y = hint + SLOPE; y >= hint - SLOPE; y--) {
            BlockPos pos = new BlockPos(x, y, z);
            if (!level.getFluidState(pos).isEmpty()) {
                return Integer.MIN_VALUE;
            }
            if (!isTerrain(level, pos)) {
                continue;
            }
            if (!level.getFluidState(pos.above()).isEmpty()) {
                return Integer.MIN_VALUE;
            }
            return y;
        }
        return Integer.MIN_VALUE;
    }

    private static boolean padIsDry(WorldGenLevel level, BlockPos ground, int[] surfaces) {
        int index = 0;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                int surface = surfaces[index++];
                for (int y = surface + 1; y <= ground.getY() + HEADROOM; y++) {
                    if (!level.getFluidState(new BlockPos(ground.getX() + dx, y, ground.getZ() + dz)).isEmpty()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static void clearColumn(WorldGenLevel level, BlockPos start, int height) {
        for (int y = 0; y < height; y++) {
            BlockPos at = start.above(y);
            if (!level.getFluidState(at).isEmpty()) {
                continue;
            }
            BlockState state = level.getBlockState(at);
            if (state.isAir()
                    || state.canBeReplaced()
                    || state.is(BlockTags.LEAVES)
                    || state.is(BlockTags.LOGS)
                    || state.is(BlockTags.SAPLINGS)) {
                level.setBlock(at, Blocks.AIR.defaultBlockState(), 2);
            }
        }
    }

    private static StallLook lookFor(WorldGenLevel level, BlockPos pos) {
        ClimateZone zone = ClimateZone.of(level.getBiome(pos));
        return switch (zone) {
            case ARID -> look(
                    Blocks.CUT_SANDSTONE.defaultBlockState(),
                    Blocks.SMOOTH_SANDSTONE.defaultBlockState(),
                    Blocks.ACACIA_FENCE.defaultBlockState(),
                    Blocks.SANDSTONE.defaultBlockState(),
                    Blocks.SANDSTONE_STAIRS.defaultBlockState(),
                    Blocks.SANDSTONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM),
                    Blocks.CUT_SANDSTONE.defaultBlockState(),
                    Blocks.LANTERN.defaultBlockState(),
                    Blocks.TERRACOTTA.defaultBlockState());
            case COLD -> look(
                    Blocks.SPRUCE_LOG.defaultBlockState(),
                    Blocks.SPRUCE_PLANKS.defaultBlockState(),
                    Blocks.SPRUCE_FENCE.defaultBlockState(),
                    Blocks.SPRUCE_PLANKS.defaultBlockState(),
                    Blocks.SPRUCE_STAIRS.defaultBlockState(),
                    Blocks.SPRUCE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM),
                    Blocks.COBBLESTONE.defaultBlockState(),
                    Blocks.SOUL_LANTERN.defaultBlockState(),
                    Blocks.SPRUCE_LEAVES.defaultBlockState());
            case TROPICAL -> look(
                    Blocks.JUNGLE_LOG.defaultBlockState(),
                    Blocks.JUNGLE_PLANKS.defaultBlockState(),
                    Blocks.JUNGLE_FENCE.defaultBlockState(),
                    Blocks.JUNGLE_PLANKS.defaultBlockState(),
                    Blocks.JUNGLE_STAIRS.defaultBlockState(),
                    Blocks.JUNGLE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM),
                    Blocks.MOSSY_COBBLESTONE.defaultBlockState(),
                    Blocks.LANTERN.defaultBlockState(),
                    Blocks.JUNGLE_LEAVES.defaultBlockState());
            default -> OAK;
        };
    }

    private static StallLook look(
            BlockState floor,
            BlockState deck,
            BlockState post,
            BlockState wall,
            BlockState stair,
            BlockState slab,
            BlockState heavy,
            BlockState light,
            BlockState leaf) {
        BlockState persistentLeaf = leaf.hasProperty(LeavesBlock.PERSISTENT)
                ? leaf.setValue(LeavesBlock.PERSISTENT, true).setValue(LeavesBlock.DISTANCE, 1)
                : leaf;
        BlockState sittingLight = light.hasProperty(BlockStateProperties.HANGING)
                ? light.setValue(BlockStateProperties.HANGING, false)
                : light;
        return new StallLook(floor, deck, post, wall, stair, slab, heavy, sittingLight, persistentLeaf);
    }

    private record StallLook(
            BlockState floor,
            BlockState deck,
            BlockState post,
            BlockState wall,
            BlockState stair,
            BlockState slab,
            BlockState heavy,
            BlockState light,
            BlockState leaf) {}
}
