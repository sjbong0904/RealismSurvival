package com.example.realismsurvival.registry;

import com.example.realismsurvival.RealismSurvival;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Shared water-like {@link FluidType}.
 *
 * <p>{@code canHydrate(true)} so sponge / concrete / sugar-cane see graded water as water.
 * Farmland still uses {@link com.example.realismsurvival.water.WaterHydration}; coral is
 * salt-only via mixins (spec §9.9).
 */
public final class ModFluidTypes {

    private ModFluidTypes() {}

    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, RealismSurvival.MODID);

    public static final DeferredHolder<FluidType, FluidType> GRADED_WATER =
            FLUID_TYPES.register("graded_water", () -> new FluidType(waterLikeProperties()) {
                @Override
                public boolean canConvertToSource(FluidState state, LevelReader reader, BlockPos pos) {
                    if (reader instanceof ServerLevel level) {
                        if (!level.getGameRules().get(GameRules.WATER_SOURCE_CONVERSION)) {
                            return false;
                        }
                    }
                    // Infinite source only among matching quality (enforced in GradedFlowingFluid.getNewLiquid).
                    return true;
                }
            });

    private static FluidType.Properties waterLikeProperties() {
        return FluidType.Properties.create()
                .descriptionId("fluid_type.realismsurvival.graded_water")
                .fallDistanceModifier(0.0F)
                .canExtinguish(true)
                .canConvertToSource(true)
                .supportsBoating(true)
                // Block hydration (sponge/concrete/…). Per-grade farmland: WaterHydration.
                .canHydrate(true)
                .isWaterLike(true)
                .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
                .sound(SoundActions.FLUID_VAPORIZE, SoundEvents.FIRE_EXTINGUISH)
                .addDripstoneDripping(
                        PointedDripstoneBlock.WATER_TRANSFER_PROBABILITY_PER_RANDOM_TICK,
                        ParticleTypes.DRIPPING_DRIPSTONE_WATER,
                        Blocks.WATER_CAULDRON,
                        SoundEvents.POINTED_DRIPSTONE_DRIP_WATER_INTO_CAULDRON
                );
    }

    public static void register(IEventBus modEventBus) {
        FLUID_TYPES.register(modEventBus);
    }
}
