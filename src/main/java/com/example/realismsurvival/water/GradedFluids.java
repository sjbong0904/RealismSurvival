package com.example.realismsurvival.water;

import com.example.realismsurvival.api.water.WaterApi;
import com.example.realismsurvival.api.water.WaterQuality;
import com.example.realismsurvival.registry.ModDataComponents;
import com.example.realismsurvival.registry.ModFluids;
import com.example.realismsurvival.registry.ModWaterQualities;
import net.minecraft.core.component.DataComponentPatch;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import org.jspecify.annotations.Nullable;

/** Graded water as NeoForge {@link FluidStack} / {@link FluidResource} (QUALITY via data component). */
public final class GradedFluids {

    public static final int BUCKET_MB = 1000;

    private GradedFluids() {}

    public static FluidResource resource(WaterQuality quality) {
        DataComponentPatch patch = DataComponentPatch.builder()
                .set(ModDataComponents.WATER_QUALITY.get(), WaterQualityStates.idOf(quality))
                .build();
        return FluidResource.of(ModFluids.GRADED_WATER.get(), patch);
    }

    public static FluidStack stack(WaterQuality quality, int amountMb) {
        return resource(quality).toStack(amountMb);
    }

    public static @Nullable WaterQuality qualityOf(FluidResource resource) {
        if (resource.isEmpty() || !WaterApi.isGradedWaterFluid(resource.getFluid())) {
            return null;
        }
        Integer id = resource.get(ModDataComponents.WATER_QUALITY.get());
        if (id == null) {
            return ModWaterQualities.FRESH.get();
        }
        return WaterQualityStates.byId(id);
    }

    public static @Nullable WaterQuality qualityOf(FluidStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        return qualityOf(FluidResource.of(stack));
    }
}
