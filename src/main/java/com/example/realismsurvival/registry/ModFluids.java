package com.example.realismsurvival.registry;

import com.example.realismsurvival.RealismSurvival;
import com.example.realismsurvival.water.GradedFlowingFluid;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * One still + flowing pair for all water qualities ({@link GradedFlowingFluid} with QUALITY state).
 */
public final class ModFluids {

    private ModFluids() {}

    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, RealismSurvival.MODID);

    public static final DeferredHolder<Fluid, FlowingFluid> GRADED_WATER =
            FLUIDS.register("graded_water", () -> new GradedFlowingFluid.Source(props()));
    public static final DeferredHolder<Fluid, FlowingFluid> FLOWING_GRADED_WATER =
            FLUIDS.register("flowing_graded_water", () -> new GradedFlowingFluid.Flowing(props()));

    /** @deprecated Prefer {@link #GRADED_WATER}; kept so old references compile during migration. */
    @Deprecated
    public static final DeferredHolder<Fluid, FlowingFluid> DISTILLED_WATER = GRADED_WATER;
    @Deprecated
    public static final DeferredHolder<Fluid, FlowingFluid> FLOWING_DISTILLED_WATER = FLOWING_GRADED_WATER;
    @Deprecated
    public static final DeferredHolder<Fluid, FlowingFluid> FRESH_WATER = GRADED_WATER;
    @Deprecated
    public static final DeferredHolder<Fluid, FlowingFluid> FLOWING_FRESH_WATER = FLOWING_GRADED_WATER;
    @Deprecated
    public static final DeferredHolder<Fluid, FlowingFluid> STAGNANT_WATER = GRADED_WATER;
    @Deprecated
    public static final DeferredHolder<Fluid, FlowingFluid> FLOWING_STAGNANT_WATER = FLOWING_GRADED_WATER;
    @Deprecated
    public static final DeferredHolder<Fluid, FlowingFluid> SALT_WATER = GRADED_WATER;
    @Deprecated
    public static final DeferredHolder<Fluid, FlowingFluid> FLOWING_SALT_WATER = FLOWING_GRADED_WATER;

    private static BaseFlowingFluid.Properties props() {
        return new BaseFlowingFluid.Properties(ModFluidTypes.GRADED_WATER, GRADED_WATER, FLOWING_GRADED_WATER)
                .block(ModBlocks.GRADED_WATER)
                .bucket(ModItems.FRESH_WATER_BUCKET)
                // Match vanilla WaterFluid (BaseFlowingFluid default is 1).
                .explosionResistance(100.0F);
    }

    public static GradedFlowingFluid graded() {
        return (GradedFlowingFluid) GRADED_WATER.get();
    }

    public static void register(IEventBus modEventBus) {
        FLUIDS.register(modEventBus);
    }
}
