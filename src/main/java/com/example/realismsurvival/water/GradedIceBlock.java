package com.example.realismsurvival.water;

import com.example.realismsurvival.registry.ModFluids;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.IceBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FluidState;
import org.jspecify.annotations.Nullable;

/**
 * Ice carrying {@link WaterQualityStates#QUALITY}. Melts / silk-break (without melt-prevent) into
 * graded water of the same quality.
 */
public class GradedIceBlock extends IceBlock {

    public static final MapCodec<GradedIceBlock> CODEC = simpleCodec(GradedIceBlock::new);

    public GradedIceBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(WaterQualityStates.QUALITY, 1));
    }

    @Override
    public MapCodec<? extends IceBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WaterQualityStates.QUALITY);
    }

    public static BlockState withQualityId(int qualityId) {
        return com.example.realismsurvival.registry.ModBlocks.GRADED_ICE.get()
                .defaultBlockState()
                .setValue(WaterQualityStates.QUALITY, WaterQualityStates.clampId(qualityId));
    }

    public static BlockState fromFluidState(FluidState fluid) {
        return withQualityId(WaterQualityStates.qualityId(fluid));
    }

    public static BlockState meltsInto(BlockState ice) {
        int qualityId = ice.hasProperty(WaterQualityStates.QUALITY)
                ? ice.getValue(WaterQualityStates.QUALITY)
                : WaterQualityStates.idOf(com.example.realismsurvival.registry.ModWaterQualities.FRESH.get());
        return ModFluids.graded().sourceOfId(qualityId, false).createLegacyBlock();
    }

    @Override
    public void playerDestroy(
            Level level,
            Player player,
            BlockPos pos,
            BlockState state,
            @Nullable BlockEntity blockEntity,
            ItemStack destroyedWith
    ) {
        // Block.playerDestroy for drops — never IceBlock.playerDestroy (that places Blocks.WATER).
        player.awardStat(net.minecraft.stats.Stats.BLOCK_MINED.get(this));
        player.causeFoodExhaustion(0.005F);
        dropResources(state, level, pos, blockEntity, player, destroyedWith);
        if (EnchantmentHelper.hasTag(destroyedWith, EnchantmentTags.PREVENTS_ICE_MELTING)) {
            return;
        }
        if (level.environmentAttributes().getValue(EnvironmentAttributes.WATER_EVAPORATES, pos)) {
            level.removeBlock(pos, false);
            return;
        }
        BlockState belowState = level.getBlockState(pos.below());
        if (belowState.blocksMotion() || belowState.liquid()) {
            level.setBlockAndUpdate(pos, meltsInto(state));
        }
    }

    @Override
    protected void melt(BlockState state, Level level, BlockPos pos) {
        if (level.environmentAttributes().getValue(EnvironmentAttributes.WATER_EVAPORATES, pos)) {
            level.removeBlock(pos, false);
        } else {
            BlockState water = meltsInto(state);
            level.setBlockAndUpdate(pos, water);
            level.neighborChanged(pos, water.getBlock(), null);
        }
    }
}
