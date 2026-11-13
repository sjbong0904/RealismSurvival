package com.example.realismsurvival.water;

import com.example.realismsurvival.api.water.WaterQuality;
import com.example.realismsurvival.event.SurvivalTickHandler;
import java.util.function.Supplier;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Freshwater-grade ice in a bucket (1..3 fill): place graded ice when full; sip in air
 * (thirst / cooling / quality debuffs from drink datapack + {@link WaterQuality}).
 */
public final class GradedIceBucketItem extends Item {

    private static final int FILL_BAR_COLOR = 0xA0_D0_FF;

    private final Supplier<WaterQuality> quality;

    public GradedIceBucketItem(Supplier<WaterQuality> quality, Properties properties) {
        super(properties);
        this.quality = quality;
    }

    public WaterQuality quality() {
        return quality.get();
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE);
        if (hit.getType() == HitResult.Type.MISS) {
            player.startUsingItem(hand);
            return InteractionResult.CONSUME;
        }
        if (hit.getType() != HitResult.Type.BLOCK) {
            return InteractionResult.PASS;
        }
        if (!BucketFill.isFull(stack)) {
            return InteractionResult.FAIL;
        }

        BlockPos clicked = hit.getBlockPos();
        Direction face = hit.getDirection();
        BlockPos placePos = clicked.relative(face);
        if (!level.mayInteract(player, clicked) || !player.mayUseItemAt(placePos, face, stack)) {
            return InteractionResult.FAIL;
        }

        BlockState at = level.getBlockState(placePos);
        if (!at.canBeReplaced()) {
            return InteractionResult.FAIL;
        }

        if (!level.isClientSide()) {
            BlockState ice = GradedIceBlock.withQualityId(WaterQualityStates.idOf(quality()));
            if (!level.setBlock(placePos, ice, Block.UPDATE_ALL)) {
                return InteractionResult.FAIL;
            }
            level.playSound(null, placePos, SoundEvents.GLASS_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
            if (player instanceof ServerPlayer serverPlayer) {
                CriteriaTriggers.PLACED_BLOCK.trigger(serverPlayer, placePos, stack);
            }
            player.awardStat(Stats.ITEM_USED.get(this));
        }

        ItemStack empty = player.hasInfiniteMaterials() ? stack : new ItemStack(Items.BUCKET);
        return InteractionResult.SUCCESS.heldItemTransformedTo(
                ItemUtils.createFilledResult(stack, player, empty)
        );
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return Consumables.DEFAULT_DRINK.consumeTicks();
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.DRINK;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        entity.gameEvent(net.minecraft.world.level.gameevent.GameEvent.DRINK);
        entity.playSound(
                SoundEvents.GENERIC_DRINK.value(),
                0.5F,
                level.getRandom().triangle(1.0F, 0.1F)
        );

        if (!level.isClientSide() && entity instanceof ServerPlayer player) {
            if (!SurvivalTickHandler.applyIceBucketSip(player, stack, quality())) {
                return stack;
            }
        }

        if (entity instanceof Player player && player.hasInfiniteMaterials()) {
            return stack;
        }

        ItemStack sipped = BucketFill.afterPour(stack);
        if (stack.getCount() > 1) {
            stack.shrink(1);
            if (entity instanceof Player player && !player.getInventory().add(sipped)) {
                player.drop(sipped, false);
            }
            return stack;
        }
        return sipped;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return BucketFill.getFill(stack) < BucketFill.MAX;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Mth.clamp(Math.round(13.0F * BucketFill.getFill(stack) / BucketFill.MAX), 1, 13);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return FILL_BAR_COLOR;
    }
}
