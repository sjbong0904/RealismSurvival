package com.example.realismsurvival.compat.shop;

import com.example.realismsurvival.RealismSurvival;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Spawns / keeps an Easy NPC merchant tagged {@link ShopMods#NPC_SHOP_TAG} at a supply post.
 */
public final class SupplyMerchantSpawner {

    private static final Identifier VILLAGER_NPC = Identifier.fromNamespaceAndPath(ShopMods.EASY_NPC, "villager");
    private static final double SCAN_RANGE = 8.0;

    private SupplyMerchantSpawner() {}

    /** @return true when a merchant is present (or Easy NPC is absent — stop retrying). */
    public static boolean ensureNearby(ServerLevel level, BlockPos markerPos, SupplyCategory category) {
        if (!ShopMods.easyNpc()) {
            return true;
        }
        BlockPos stand = standPos(level, markerPos);
        Vec3 feet = stand.getBottomCenter();
        AABB box = new AABB(markerPos).inflate(SCAN_RANGE);
        for (Entity entity : level.getEntities((Entity) null, box, SupplyShopAccess::isSupplyMerchant)) {
            if (entity.distanceToSqr(markerPos.getCenter()) > SCAN_RANGE * SCAN_RANGE) {
                continue;
            }
            if (entity.getY() > feet.y + 0.2) {
                entity.teleportTo(feet.x, feet.y, feet.z);
                face(entity, markerPos.getCenter());
            }
            dress(entity, category);
            return true;
        }
        return spawnMerchant(level, stand, markerPos, category);
    }

    /** Floor beside the barrel marker. The marker itself is a full block, so standing on it puts the head in the roof. */
    private static BlockPos standPos(ServerLevel level, BlockPos markerPos) {
        BlockPos[] candidates = {
            markerPos.relative(Direction.WEST),
            markerPos.relative(Direction.EAST),
            markerPos.offset(-1, 0, -1),
            markerPos.offset(1, 0, -1),
            markerPos.relative(Direction.SOUTH),
            markerPos.relative(Direction.NORTH)
        };
        for (BlockPos candidate : candidates) {
            if (isStandable(level, candidate)) {
                return candidate;
            }
        }
        return markerPos.above();
    }

    private static boolean isStandable(ServerLevel level, BlockPos pos) {
        if (!level.getFluidState(pos).isEmpty() || !level.getFluidState(pos.above()).isEmpty()) {
            return false;
        }
        BlockState here = level.getBlockState(pos);
        BlockState head = level.getBlockState(pos.above());
        if ((!here.isAir() && !here.canBeReplaced()) || (!head.isAir() && !head.canBeReplaced())) {
            return false;
        }
        return level.getBlockState(pos.below()).isSolid();
    }

    private static void face(Entity entity, Vec3 lookAt) {
        double dz = lookAt.z - entity.getZ();
        double dx = lookAt.x - entity.getX();
        float yaw = Mth.wrapDegrees((float) (Mth.atan2(dz, dx) * (180.0F / (float) Math.PI)) - 90.0F);
        entity.setYRot(yaw);
        entity.setXRot(0.0F);
        entity.setYHeadRot(yaw);
        if (entity instanceof Mob mob) {
            mob.setYBodyRot(yaw);
        }
    }

    private static void dress(Entity entity, SupplyCategory category) {
        for (SupplyCategory other : SupplyCategory.values()) {
            if (other != category) {
                entity.removeTag(other.entityTag());
            }
        }
        entity.addTag(ShopMods.NPC_SHOP_TAG);
        entity.addTag(category.entityTag());
        entity.setCustomName(Component.translatable(category.nameKey()));
        entity.setCustomNameVisible(true);
        if (entity instanceof Villager villager) {
            villager.setVillagerData(villager.getVillagerData()
                    .withProfession(BuiltInRegistries.VILLAGER_PROFESSION.getOrThrow(category.profession())));
        }
    }

    private static boolean spawnMerchant(ServerLevel level, BlockPos standPos, BlockPos markerPos, SupplyCategory category) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(VILLAGER_NPC);
        if (type == null) {
            RealismSurvival.LOGGER.warn("[RealismSurvival] Easy NPC entity {} missing; supply merchant not spawned", VILLAGER_NPC);
            return true;
        }
        Entity entity = type.create(level, EntitySpawnReason.STRUCTURE);
        if (entity == null) {
            return false;
        }
        Vec3 feet = standPos.getBottomCenter();
        entity.snapTo(feet.x, feet.y, feet.z, 0.0F, 0.0F);
        face(entity, markerPos.getCenter());
        entity.setInvulnerable(true);
        if (entity instanceof Mob mob) {
            mob.setPersistenceRequired();
            mob.setNoAi(true);
        }
        if (!level.addFreshEntity(entity)) {
            return false;
        }
        // finalizeSpawn clears the profession, so the outfit is applied after the entity joins.
        dress(entity, category);
        RealismSurvival.LOGGER.debug(
                "[RealismSurvival] Spawned supply merchant at {} in {}", standPos, level.dimension().identifier());
        return true;
    }
}
