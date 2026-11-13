package com.example.realismsurvival.event;

import com.example.realismsurvival.RealismSurvival;
import com.example.realismsurvival.api.water.WaterApi;
import com.example.realismsurvival.api.water.WaterQuality;
import com.example.realismsurvival.water.VanillaWaterConverter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.dispenser.OptionalDispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;

/**
 * Dispenser support for graded water buckets / bottles, and post-place conversion for vanilla
 * {@code water_bucket} (EntityPlaceEvent does not fire for dispensers).
 *
 * <p>Addons may call {@link #registerBucket(Item, boolean)} for custom bucket items that are not on a
 * {@link WaterQuality} entry (registered qualities are covered automatically in {@link #register()}).
 */
public final class WaterDispenseBehaviors {

    private WaterDispenseBehaviors() {}

    public static void register() {
        registerBucket(Items.WATER_BUCKET, true);
        for (WaterQuality quality : WaterApi.registry()) {
            registerBucket(quality.bucketItem(), false);
        }
        registerGlassBottle();
        RealismSurvival.LOGGER.info("[RealismSurvival] Registered graded / converting water dispense behaviors");
    }

    private static void registerGlassBottle() {
        DispenserBlock.registerBehavior(Items.GLASS_BOTTLE, new OptionalDispenseItemBehavior() {
            @Override
            protected ItemStack execute(BlockSource source, ItemStack stack) {
                this.setSuccess(false);
                ServerLevel level = source.level();
                BlockPos target = source.pos().relative(source.state().getValue(DispenserBlock.FACING));
                BlockState state = level.getBlockState(target);
                if (state.is(BlockTags.BEEHIVES, s -> s.hasProperty(BeehiveBlock.HONEY_LEVEL) && s.getBlock() instanceof BeehiveBlock)
                        && state.getValue(BeehiveBlock.HONEY_LEVEL) >= 5) {
                    ((BeehiveBlock) state.getBlock())
                            .releaseBeesAndResetHoneyLevel(
                                    level, state, target, null, BeehiveBlockEntity.BeeReleaseStatus.BEE_RELEASED);
                    this.setSuccess(true);
                    level.gameEvent(null, GameEvent.FLUID_PICKUP, source.pos());
                    return this.consumeWithRemainder(source, stack, new ItemStack(Items.HONEY_BOTTLE));
                }
                FluidState fluid = level.getFluidState(target);
                if (fluid.is(FluidTags.WATER) || WaterApi.isGradedWaterFluid(fluid.getType())) {
                    WaterQuality quality = WaterApi.qualityAt(level, target, fluid);
                    this.setSuccess(true);
                    level.gameEvent(null, GameEvent.FLUID_PICKUP, source.pos());
                    return this.consumeWithRemainder(source, stack, quality.createBottle());
                }
                return super.execute(source, stack);
            }
        });
    }

    /** Public helper for addon buckets (e.g. custom {@link BucketItem} not linked via {@link WaterQuality}). */
    public static void registerBucket(Item bucketItem, boolean convertVanilla) {
        DispenserBlock.registerBehavior(bucketItem, convertingBucketBehavior(bucketItem, convertVanilla));
    }

    private static OptionalDispenseItemBehavior convertingBucketBehavior(Item bucketItem, boolean convertVanilla) {
        DefaultDispenseItemBehavior fallback = new DefaultDispenseItemBehavior();
        return new OptionalDispenseItemBehavior() {
            @Override
            protected ItemStack execute(BlockSource source, ItemStack stack) {
                if (!(bucketItem instanceof BucketItem bucket)) {
                    return fallback.dispense(source, stack);
                }
                Level level = source.level();
                BlockPos target = source.pos().relative(source.state().getValue(DispenserBlock.FACING));
                this.setSuccess(false);

                if (bucket.emptyContents(null, level, target, null, stack)) {
                    bucket.checkExtraContent(null, level, stack, target);
                    if (convertVanilla && level instanceof ServerLevel serverLevel) {
                        // Dispenser places Blocks.WATER; convert to biome grade immediately.
                        VanillaWaterConverter.tryConvert(serverLevel, target);
                        // Also convert waterlogged placements (emptyContents into LiquidBlockContainer).
                        convertIfVanillaWaterPresent(serverLevel, target);
                    }
                    this.setSuccess(true);
                    return this.consumeWithRemainder(source, stack, new ItemStack(Items.BUCKET));
                }

                // Vanilla retry: waterloggable neighbour (same as DispenseItemBehavior filled bucket).
                BlockState state = level.getBlockState(target);
                if (state.getBlock() instanceof LiquidBlockContainer container) {
                    Fluid content = bucket.getContent();
                    if (container.canPlaceLiquid(null, level, target, state, content)
                            && container.placeLiquid(level, target, state, content.defaultFluidState())) {
                        bucket.checkExtraContent(null, level, stack, target);
                        if (convertVanilla && level instanceof ServerLevel serverLevel) {
                            convertIfVanillaWaterPresent(serverLevel, target);
                        }
                        this.setSuccess(true);
                        return this.consumeWithRemainder(source, stack, new ItemStack(Items.BUCKET));
                    }
                }

                return fallback.dispense(source, stack);
            }
        };
    }

    private static void convertIfVanillaWaterPresent(ServerLevel level, BlockPos pos) {
        VanillaWaterConverter.tryConvert(level, pos);
        // Waterlogged: cannot swap fluid type; hydration rules handle salt-biome waterlogged separately.
    }
}
