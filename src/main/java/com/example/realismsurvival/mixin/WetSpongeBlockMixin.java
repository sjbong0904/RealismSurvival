package com.example.realismsurvival.mixin;

import com.example.realismsurvival.water.WaterQualityStates;
import com.example.realismsurvival.water.WetSpongeItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WetSpongeBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds {@link WaterQualityStates#QUALITY} to vanilla wet sponge (shared block + grade). */
@Mixin(WetSpongeBlock.class)
public abstract class WetSpongeBlockMixin extends Block {

    private WetSpongeBlockMixin(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WaterQualityStates.QUALITY);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void realismsurvival$defaultQuality(BlockBehaviour.Properties properties, CallbackInfo ci) {
        this.registerDefaultState(
                this.stateDefinition.any().setValue(WaterQualityStates.QUALITY, WetSpongeItems.DEFAULT_QUALITY_ID)
        );
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        return WetSpongeItems.create(WetSpongeItems.qualityId(state));
    }
}
