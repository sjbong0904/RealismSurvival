package com.example.realismsurvival.event;

import com.example.realismsurvival.api.water.WaterApi;
import com.example.realismsurvival.api.water.WaterQuality;
import com.example.realismsurvival.registry.ModWaterQualities;
import com.example.realismsurvival.water.CauldronWater;
import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.neoforge.event.RegisterCauldronInteractionEvent;

/**
 * Graded bucket / bottle ↔ water cauldron. Vanilla water bucket / water potion map to fresh /
 * distilled pour rules; scoop always returns the cauldron's {@link WaterQuality}.
 *
 * <p>Registered on the mod bus from {@link RealismSurvival}.
 */
public final class CauldronInteractionHandler {

    private static final Identifier EMPTY = Identifier.withDefaultNamespace("empty");
    private static final Identifier WATER = Identifier.withDefaultNamespace("water");

    private CauldronInteractionHandler() {}

    public static void register(RegisterCauldronInteractionEvent.Interaction event) {
        event.register(EMPTY, Items.WATER_BUCKET, CauldronInteractionHandler::fillFromVanillaWaterBucket);
        event.register(WATER, Items.WATER_BUCKET, CauldronInteractionHandler::fillFromVanillaWaterBucket);

        for (WaterQuality quality : WaterApi.registry()) {
            Item bucket = quality.bucketItem();
            event.register(EMPTY, bucket, fillFromGradedBucket(quality));
            event.register(WATER, bucket, fillFromGradedBucket(quality));

            Item bottle = quality.bottleItem();
            event.register(EMPTY, bottle, emptyBottle(quality));
            event.register(WATER, bottle, emptyBottle(quality));
        }

        event.register(WATER, Items.BUCKET, CauldronInteractionHandler::emptyToGradedBucket);
        event.register(WATER, Items.GLASS_BOTTLE, CauldronInteractionHandler::fillGradedBottle);
        event.register(EMPTY, Items.POTION, CauldronInteractionHandler::emptyVanillaWaterPotion);
        event.register(WATER, Items.POTION, CauldronInteractionHandler::emptyVanillaWaterPotion);
    }

    private static CauldronInteraction fillFromGradedBucket(WaterQuality quality) {
        return (state, level, pos, player, hand, stack) -> {
            if (CauldronWater.isWaterCauldron(state) && !CauldronWater.accepts(state, quality)) {
                // FAIL (not TRY_WITH_EMPTY_HAND): otherwise BucketItem.use places water on/above the cauldron.
                return InteractionResult.FAIL;
            }
            return emptyBucket(level, pos, player, hand, stack, CauldronWater.full(quality));
        };
    }

    private static InteractionResult fillFromVanillaWaterBucket(
            BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, ItemStack stack
    ) {
        // Leftover vanilla buckets behave as fresh when poured.
        WaterQuality quality = ModWaterQualities.FRESH.get();
        if (CauldronWater.isWaterCauldron(state) && !CauldronWater.accepts(state, quality)) {
            return InteractionResult.FAIL;
        }
        return emptyBucket(level, pos, player, hand, stack, CauldronWater.full(quality));
    }

    private static CauldronInteraction emptyBottle(WaterQuality quality) {
        return (state, level, pos, player, hand, stack) -> {
            if (CauldronWater.isWaterCauldron(state)) {
                if (state.getValue(LayeredCauldronBlock.LEVEL) == LayeredCauldronBlock.MAX_FILL_LEVEL) {
                    return InteractionResult.TRY_WITH_EMPTY_HAND;
                }
                if (!CauldronWater.accepts(state, quality)) {
                    return InteractionResult.FAIL;
                }
                if (!level.isClientSide()) {
                    player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.GLASS_BOTTLE)));
                    player.awardStat(Stats.USE_CAULDRON);
                    player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
                    BlockState newState = CauldronWater.withQuality(state.cycle(LayeredCauldronBlock.LEVEL), quality);
                    level.setBlockAndUpdate(pos, newState);
                    level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
                    level.gameEvent(null, GameEvent.FLUID_PLACE, pos);
                }
                return InteractionResult.SUCCESS;
            }
            if (!state.is(Blocks.CAULDRON)) {
                return InteractionResult.TRY_WITH_EMPTY_HAND;
            }
            if (!level.isClientSide()) {
                player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.GLASS_BOTTLE)));
                player.awardStat(Stats.USE_CAULDRON);
                player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
                level.setBlockAndUpdate(pos, CauldronWater.levelOne(quality));
                level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
                level.gameEvent(null, GameEvent.FLUID_PLACE, pos);
            }
            return InteractionResult.SUCCESS;
        };
    }

    private static InteractionResult emptyVanillaWaterPotion(
            BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, ItemStack stack
    ) {
        PotionContents potion = stack.get(DataComponents.POTION_CONTENTS);
        if (potion == null || !potion.is(Potions.WATER)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        // Worldgen / leftover water potions count as distilled.
        return emptyBottle(ModWaterQualities.DISTILLED.get()).interact(state, level, pos, player, hand, stack);
    }

    private static InteractionResult emptyToGradedBucket(
            BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, ItemStack stack
    ) {
        if (!CauldronWater.isWaterCauldron(state)
                || state.getValue(LayeredCauldronBlock.LEVEL) != LayeredCauldronBlock.MAX_FILL_LEVEL) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!level.isClientSide()) {
            Item used = stack.getItem();
            player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, CauldronWater.createBucket(state)));
            player.awardStat(Stats.USE_CAULDRON);
            player.awardStat(Stats.ITEM_USED.get(used));
            level.setBlockAndUpdate(pos, Blocks.CAULDRON.defaultBlockState());
            level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
            level.gameEvent(null, GameEvent.FLUID_PICKUP, pos);
        }
        return InteractionResult.SUCCESS;
    }

    private static InteractionResult fillGradedBottle(
            BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, ItemStack stack
    ) {
        if (!CauldronWater.isWaterCauldron(state)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!level.isClientSide()) {
            Item used = stack.getItem();
            player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, CauldronWater.createBottle(state)));
            player.awardStat(Stats.USE_CAULDRON);
            player.awardStat(Stats.ITEM_USED.get(used));
            LayeredCauldronBlock.lowerFillLevel(state, level, pos);
            level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
            level.gameEvent(null, GameEvent.FLUID_PICKUP, pos);
        }
        return InteractionResult.SUCCESS;
    }

    private static InteractionResult emptyBucket(
            Level level, BlockPos pos, Player player, InteractionHand hand, ItemStack stack, BlockState newState
    ) {
        if (!level.isClientSide()) {
            Item used = stack.getItem();
            player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.BUCKET)));
            player.awardStat(Stats.FILL_CAULDRON);
            player.awardStat(Stats.ITEM_USED.get(used));
            level.setBlockAndUpdate(pos, newState);
            level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
            level.gameEvent(null, GameEvent.FLUID_PLACE, pos);
        }
        return InteractionResult.SUCCESS;
    }
}
