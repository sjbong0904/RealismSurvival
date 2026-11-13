package com.example.realismsurvival.clinic;

import com.example.realismsurvival.RealismSurvival;
import com.example.realismsurvival.RealismSurvivalConfig;
import com.example.realismsurvival.registry.ModBlocks;
import com.example.realismsurvival.registry.ModVillagers;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;

/**
 * Ensures each Overworld village structure start gets one clinic table + doctor (once).
 */
@EventBusSubscriber(modid = RealismSurvival.MODID)
public final class VillageDoctorSpawner {

    private static final Set<Long> SEEDED = new HashSet<>();

    private VillageDoctorSpawner() {}

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || level.dimension() != ServerLevel.OVERWORLD
                || !RealismSurvivalConfig.VILLAGE_DOCTOR_SPAWN.getAsBoolean()) {
            return;
        }
        if (!(event.getChunk() instanceof LevelChunk chunk)) {
            return;
        }
        level.getServer().execute(() -> seedChunk(level, chunk));
    }

    private static void seedChunk(ServerLevel level, LevelChunk chunk) {
        var structures = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        for (StructureStart start : chunk.getAllStarts().values()) {
            if (start == null || !start.isValid()) {
                continue;
            }
            Holder<Structure> holder = structures.wrapAsHolder(start.getStructure());
            if (!holder.is(StructureTags.VILLAGE)) {
                continue;
            }
            BoundingBox box = start.getBoundingBox();
            long key = BlockPos.asLong(box.minX() >> 4, 0, box.minZ() >> 4);
            if (!SEEDED.add(key)) {
                continue;
            }
            BlockPos center = box.getCenter();
            if (hasDoctorNearby(level, center)) {
                continue;
            }
            placeDoctor(level, center);
        }
    }

    private static boolean hasDoctorNearby(ServerLevel level, BlockPos center) {
        return !level
                .getEntitiesOfClass(
                        Villager.class,
                        AABB.ofSize(center.getCenter(), 96, 64, 96),
                        v -> ModVillagers.isDoctor(v.getVillagerData().profession().value()))
                .isEmpty();
    }

    private static void placeDoctor(ServerLevel level, BlockPos near) {
        BlockPos.MutableBlockPos cursor = near.mutable();
        BlockPos ground = null;
        for (int dy = 16; dy >= -16; dy--) {
            cursor.set(near.getX(), near.getY() + dy, near.getZ());
            BlockState feet = level.getBlockState(cursor);
            BlockState below = level.getBlockState(cursor.below());
            if (feet.isAir() && below.isFaceSturdy(level, cursor.below(), Direction.UP)) {
                ground = cursor.immutable();
                break;
            }
        }
        if (ground == null) {
            ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, near);
        }
        BlockPos tablePos = ground;
        if (!level.getBlockState(tablePos).canBeReplaced()) {
            tablePos = ground.north();
        }
        BlockPos standPos = tablePos.above();
        level.setBlock(tablePos, ModBlocks.CLINIC_TABLE.get().defaultBlockState(), 3);
        Villager villager = EntityType.VILLAGER.create(level, EntitySpawnReason.MOB_SUMMONED);
        if (villager == null) {
            return;
        }
        villager.snapTo(standPos.getX() + 0.5, standPos.getY(), standPos.getZ() + 0.5, 0.0F, 0.0F);
        villager.setVillagerData(villager.getVillagerData()
                .withProfession(level.registryAccess(), ModVillagers.DOCTOR_KEY)
                .withLevel(2));
        level.addFreshEntity(villager);
        level.getPoiManager().add(tablePos, level.holderOrThrow(ModVillagers.CLINIC_POI_KEY));
    }
}
