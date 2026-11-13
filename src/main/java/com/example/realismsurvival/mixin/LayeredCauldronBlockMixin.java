package com.example.realismsurvival.mixin;

import com.example.realismsurvival.registry.ModWaterQualities;
import com.example.realismsurvival.water.CauldronWater;
import com.example.realismsurvival.water.WaterQualityStates;
import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Adds {@link WaterQualityStates#QUALITY} to layered cauldrons (water + powder snow).
 * Water cauldron drip / rain respect grade; worldgen default is distilled.
 */
@Mixin(LayeredCauldronBlock.class)
public abstract class LayeredCauldronBlockMixin extends Block {

    @Shadow
    @Final
    private Biome.Precipitation precipitationType;

    private LayeredCauldronBlockMixin(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Inject(method = "createBlockStateDefinition", at = @At("RETURN"))
    private void realismsurvival$addQuality(StateDefinition.Builder<Block, BlockState> builder, CallbackInfo ci) {
        builder.add(WaterQualityStates.QUALITY);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void realismsurvival$defaultQuality(
            Biome.Precipitation precipitationType,
            CauldronInteraction.Dispatcher interactions,
            BlockBehaviour.Properties properties,
            CallbackInfo ci
    ) {
        // Same pattern as WetSpongeBlockMixin — call Block.registerDefaultState via subclass, no @Shadow.
        this.registerDefaultState(
                this.stateDefinition
                        .any()
                        .setValue(LayeredCauldronBlock.LEVEL, 1)
                        .setValue(WaterQualityStates.QUALITY, CauldronWater.DEFAULT_QUALITY_ID)
        );
    }

    @Inject(method = "canReceiveStalactiteDrip", at = @At("HEAD"), cancellable = true)
    private void realismsurvival$dripWaterLike(Fluid fluid, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this != Blocks.WATER_CAULDRON) {
            return;
        }
        cir.setReturnValue(CauldronWater.isDrippableWater(fluid) && this.precipitationType == Biome.Precipitation.RAIN);
    }

    @Inject(method = "receiveStalactiteDrip", at = @At("HEAD"), cancellable = true)
    private void realismsurvival$dripQuality(BlockState state, Level level, BlockPos pos, Fluid fluid, CallbackInfo ci) {
        if (!CauldronWater.isWaterCauldron(state)) {
            return;
        }
        LayeredCauldronBlock self = (LayeredCauldronBlock) (Object) this;
        if (self.isFull(state) || !CauldronWater.isDrippableWater(fluid)) {
            ci.cancel();
            return;
        }
        var dripQuality = CauldronWater.dripQuality(level, pos);
        if (!CauldronWater.accepts(state, dripQuality)) {
            ci.cancel();
            return;
        }
        BlockState newState = CauldronWater.withQuality(
                state.setValue(LayeredCauldronBlock.LEVEL, state.getValue(LayeredCauldronBlock.LEVEL) + 1),
                dripQuality
        );
        level.setBlockAndUpdate(pos, newState);
        level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(newState));
        level.levelEvent(1047, pos, 0);
        ci.cancel();
    }

    @Inject(method = "handlePrecipitation", at = @At("HEAD"), cancellable = true)
    private void realismsurvival$rainFreshOnly(
            BlockState state, Level level, BlockPos pos, Biome.Precipitation precipitation, CallbackInfo ci
    ) {
        if (!CauldronWater.isWaterCauldron(state) || precipitation != Biome.Precipitation.RAIN) {
            return;
        }
        if (level.getRandom().nextFloat() >= 0.05F) {
            ci.cancel();
            return;
        }
        if (state.getValue(LayeredCauldronBlock.LEVEL) == LayeredCauldronBlock.MAX_FILL_LEVEL) {
            ci.cancel();
            return;
        }
        var fresh = ModWaterQualities.FRESH.get();
        if (!CauldronWater.accepts(state, fresh)) {
            ci.cancel();
            return;
        }
        BlockState newState = CauldronWater.withQuality(state.cycle(LayeredCauldronBlock.LEVEL), fresh);
        level.setBlockAndUpdate(pos, newState);
        level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(newState));
        ci.cancel();
    }

    /**
     * Powder-snow cauldron extinguish → fresh water cauldron at the same fill level (vanilla then
     * lowers one level). Without this, {@code WATER_CAULDRON.defaultBlockState()} drops QUALITY
     * handling and can look like “evaporated / half” when scooping.
     */
    @Inject(method = "handleEntityOnFireInside", at = @At("HEAD"), cancellable = true)
    private void realismsurvival$powderSnowToFresh(
            BlockState state, Level level, BlockPos pos, CallbackInfo ci
    ) {
        if (this.precipitationType != Biome.Precipitation.SNOW) {
            return;
        }
        int levelValue = state.getValue(LayeredCauldronBlock.LEVEL);
        BlockState water = CauldronWater.waterCauldron(levelValue, ModWaterQualities.FRESH.get());
        LayeredCauldronBlock.lowerFillLevel(water, level, pos);
        ci.cancel();
    }
}
