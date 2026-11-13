package com.example.realismsurvival.event;

import com.example.realismsurvival.RealismSurvival;
import com.example.realismsurvival.api.water.WaterApi;
import com.example.realismsurvival.api.water.WaterQuality;
import com.example.realismsurvival.water.BottleEnchantmentTransfer;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Bucketable;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Scooping empty buckets / glass bottles from water sources:
 * <ul>
 *   <li>Graded fluid already in the world → that {@link WaterQuality}</li>
 *   <li>Vanilla water → biome-mapped quality (oceans stay {@code minecraft:water})</li>
 * </ul>
 */
@EventBusSubscriber(modid = RealismSurvival.MODID)
public final class WaterFillHandler {

    private WaterFillHandler() {}

    @SubscribeEvent
    static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        boolean bottle = stack.is(Items.GLASS_BOTTLE);
        boolean emptyBucket = stack.is(Items.BUCKET);
        if (!bottle && !emptyBucket) {
            return;
        }

        Level level = event.getLevel();

        if (bottle && hasDragonBreathCloud(level, player)) {
            return;
        }

        // Empty bucket aimed at a Bucketable (fish/axolotl/…) → let entity interact catch it.
        if (emptyBucket && lookingAtBucketable(player)) {
            return;
        }

        BlockHitResult hit = Item.getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        BlockPos pos = hit.getBlockPos();
        if (!level.mayInteract(player, pos)) {
            return;
        }

        BlockState blockState = level.getBlockState(pos);
        FluidState fluidState = blockState.getFluidState();
        if (!WaterApi.isInteractableWaterSource(fluidState)) {
            return;
        }

        WaterQuality graded = WaterQuality.fromFluidState(fluidState);
        WaterQuality quality = graded != null ? graded : WaterQuality.fromBiome(level.getBiome(pos));

        if (level.isClientSide()) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }

        // Buckets remove the source; bottles leave it (vanilla bottle behaviour).
        if (emptyBucket) {
            if (!(blockState.getBlock() instanceof BucketPickup pickup)) {
                return;
            }
            ItemStack removed = pickup.pickupBlock(player, level, pos, blockState);
            if (removed.isEmpty()) {
                return;
            }
        }

        ItemStack filled = bottle ? quality.createBottle() : quality.createBucket();
        if (bottle) {
            BottleEnchantmentTransfer.copyEnchantments(stack, filled);
        }
        ItemStack result = ItemUtils.createFilledResult(stack, player, filled);

        level.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                bottle ? SoundEvents.BOTTLE_FILL : SoundEvents.BUCKET_FILL,
                SoundSource.NEUTRAL,
                1.0F,
                1.0F
        );
        level.gameEvent(player, GameEvent.FLUID_PICKUP, pos);

        player.setItemInHand(event.getHand(), result);
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    private static boolean hasDragonBreathCloud(Level level, Player player) {
        List<AreaEffectCloud> clouds = level.getEntitiesOfClass(
                AreaEffectCloud.class,
                player.getBoundingBox().inflate(2.0),
                cloud -> cloud.isAlive() && cloud.getOwner() instanceof EnderDragon
        );
        return !clouds.isEmpty();
    }

    private static boolean lookingAtBucketable(Player player) {
        double reach = Math.max(player.blockInteractionRange(), player.entityInteractionRange());
        Vec3 from = player.getEyePosition(1.0F);
        Vec3 look = player.getViewVector(1.0F);
        Vec3 to = from.add(look.scale(reach));
        AABB box = player.getBoundingBox().expandTowards(look.scale(reach)).inflate(1.0);
        Predicate<Entity> pickable = entity -> entity instanceof Bucketable && entity.isAlive() && entity.isPickable();
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, from, to, box, pickable, reach);
        return hit != null;
    }
}
