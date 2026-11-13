package com.example.realismsurvival.water;

import com.example.realismsurvival.api.water.WaterApi;
import com.example.realismsurvival.api.water.WaterQuality;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

/**
 * Shared “one water surface” tests between graded water and vanilla water / waterlogged cells.
 * Grades share one Fluid — {@link Fluid#isSame} already unifies them; this bridges vanilla water.
 */
public final class WaterSurfaceCompat {

    private WaterSurfaceCompat() {}

    public static boolean isWaterSurface(Fluid fluid) {
        if (fluid == Fluids.EMPTY) {
            return false;
        }
        if (fluid == Fluids.WATER || fluid == Fluids.FLOWING_WATER) {
            return true;
        }
        if (WaterApi.isGradedWaterFluid(fluid)) {
            return true;
        }
        return fluid.getFluidType().getIsWaterLike();
    }

    public static boolean isWaterSurface(FluidState state) {
        return !state.isEmpty() && isWaterSurface(state.getType());
    }

    /**
     * Flowing vanilla (or other water-like) may be overwritten by a graded <em>source</em> fluid
     * from any side. Graded↔graded uses normal same-fluid flow (no displace).
     */
    public static boolean canDisplaceFlowingWaterLike(FluidState existing, Fluid incoming) {
        if (existing.isEmpty() || existing.isSource()) {
            return false;
        }
        if (!isWaterSurface(existing) || !isWaterSurface(incoming)) {
            return false;
        }
        if (existing.getType().isSame(incoming)) {
            return false;
        }
        return incoming.defaultFluidState().isSource();
    }

    public static boolean connects(FluidState a, FluidState b) {
        return isWaterSurface(a) && isWaterSurface(b);
    }

    public static boolean connects(Fluid a, FluidState b) {
        return isWaterSurface(a) && isWaterSurface(b);
    }

    public static boolean connectsFluids(Fluid a, Fluid b) {
        return isWaterSurface(a) && isWaterSurface(b);
    }
}
