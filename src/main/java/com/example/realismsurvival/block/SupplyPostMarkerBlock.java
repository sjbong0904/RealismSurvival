package com.example.realismsurvival.block;

import com.example.realismsurvival.blockentity.SupplyPostMarkerBlockEntity;
import com.example.realismsurvival.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** Worldgen stall anchor; server-ticks until a tagged supply merchant is present. */
public final class SupplyPostMarkerBlock extends BaseEntityBlock {

    public static final MapCodec<SupplyPostMarkerBlock> CODEC = simpleCodec(SupplyPostMarkerBlock::new);

    public SupplyPostMarkerBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<SupplyPostMarkerBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SupplyPostMarkerBlockEntity(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide()
                ? null
                : createTickerHelper(type, ModBlockEntities.SUPPLY_POST_MARKER.get(), SupplyPostMarkerBlockEntity::serverTick);
    }
}
