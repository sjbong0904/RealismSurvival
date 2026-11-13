package com.example.realismsurvival.water;

import com.example.realismsurvival.api.water.WaterQuality;
import com.example.realismsurvival.event.SurvivalTickHandler;
import com.example.realismsurvival.registry.ModWaterQualities;
import java.util.function.Supplier;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jspecify.annotations.Nullable;

/**
 * Graded water bucket: place/discard on block click; hold-use in air to sip (fill −1).
 *
 * <p>Pouring into a water-like stream replaces that cell with a shared graded-water
 * <em>source</em> carrying this bucket's {@link WaterQuality} on {@link WaterQualityStates#QUALITY}.
 */
public final class GradedWaterBucketItem extends BucketItem {

    /** RGB bar color (light blue). */
    private static final int FILL_BAR_COLOR = 0x40_8C_FF;

    private final Supplier<WaterQuality> quality;

    public GradedWaterBucketItem(Fluid content, Supplier<WaterQuality> quality, Properties properties) {
        super(content, properties);
        this.quality = quality;
    }

    public WaterQuality quality() {
        return quality.get();
    }

    /**
     * Hit water-like fluids with {@link ClipContext.Fluid#ANY} so pouring into a stream targets
     * that cell (vanilla {@code Fluid.NONE} skips fluids and often places beside the path).
     * Air / miss → start drinking.
     */
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY);
        if (hit.getType() == HitResult.Type.MISS) {
            player.startUsingItem(hand);
            return InteractionResult.CONSUME;
        }
        if (hit.getType() != HitResult.Type.BLOCK) {
            return InteractionResult.PASS;
        }

        BlockPos pos = hit.getBlockPos();
        Direction direction = hit.getDirection();
        BlockPos offsetPos = pos.relative(direction);
        if (!level.mayInteract(player, pos) || !player.mayUseItemAt(offsetPos, direction, stack)) {
            return InteractionResult.FAIL;
        }

        BlockState clicked = level.getBlockState(pos);
        FluidState clickedFluid = clicked.getFluidState();
        BlockPos placePos;
        if (WaterSurfaceCompat.isWaterSurface(clickedFluid)) {
            // Replace the stream / source cell we aimed at (A-path ← B source).
            placePos = pos;
        } else if (clicked.getBlock() instanceof LiquidBlockContainer container
                && container.canPlaceLiquid(player, level, pos, clicked, getContent())) {
            placePos = pos;
        } else {
            placePos = offsetPos;
        }

        if (emptyContents(player, level, placePos, hit, stack)) {
            checkExtraContent(player, level, stack, placePos);
            if (player instanceof ServerPlayer serverPlayer) {
                CriteriaTriggers.PLACED_BLOCK.trigger(serverPlayer, placePos, stack);
            }
            player.awardStat(Stats.ITEM_USED.get(this));
            ItemStack empty = player.hasInfiniteMaterials() ? stack : new ItemStack(Items.BUCKET);
            return InteractionResult.SUCCESS.heldItemTransformedTo(
                    ItemUtils.createFilledResult(stack, player, empty)
            );
        }
        return InteractionResult.FAIL;
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
            if (!SurvivalTickHandler.applyBucketSip(player, stack, quality())) {
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

    /**
     * Full bucket: place this grade's source. Water-like cells (any grade / vanilla water, flowing
     * or source) are always replaced in-place. Partial fill still discards without placing.
     */
    @Override
    public boolean emptyContents(
            @Nullable LivingEntity user,
            Level level,
            BlockPos pos,
            @Nullable BlockHitResult hitResult,
            @Nullable ItemStack containerItem
    ) {
        if (containerItem != null && !BucketFill.isFull(containerItem)) {
            return true;
        }
        if (!(getContent() instanceof FlowingFluid flowing)) {
            return false;
        }

        BlockState state = level.getBlockState(pos);
        FluidState existing = state.getFluidState();

        if (WaterSurfaceCompat.isWaterSurface(existing)) {
            if (level instanceof ServerLevel serverLevel) {
                placeGradedSource(serverLevel, pos, flowing);
            }
            playEmptySound(user, level, pos);
            return true;
        }

        if (super.emptyContents(user, level, pos, hitResult, containerItem)) {
            if (level instanceof ServerLevel serverLevel && flowing instanceof GradedFlowingFluid) {
                // BucketItem places default QUALITY=0; rewrite to this bucket's grade.
                FluidState placedFluid = serverLevel.getFluidState(pos);
                if (placedFluid.getType().isSame(flowing)) {
                    placeGradedSource(serverLevel, pos, flowing);
                }
            }
            return true;
        }
        return false;
    }

    private void placeGradedSource(ServerLevel serverLevel, BlockPos pos, FlowingFluid flowing) {
        FluidState sourceState = flowing instanceof GradedFlowingFluid graded
                ? graded.sourceOf(quality(), false)
                : flowing.getSource(false);
        BlockState placed = sourceState.createLegacyBlock();
        if (!serverLevel.setBlock(pos, placed, Block.UPDATE_ALL)) {
            serverLevel.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            serverLevel.setBlock(pos, placed, Block.UPDATE_ALL);
        }
        flowing.tick(serverLevel, pos, placed, sourceState);
        if (quality() == ModWaterQualities.FRESH.get()) {
            FreshWaterStagnator.noticeFreshSource(serverLevel, pos);
        }
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
