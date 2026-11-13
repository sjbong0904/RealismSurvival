package com.example.realismsurvival.water;

import com.example.realismsurvival.RealismSurvivalConfig;
import com.example.realismsurvival.api.water.WaterQuality;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.common.FarmlandWaterManager;

/**
 * Farmland irrigation rules for graded water.
 *
 * <p><b>Default ({@code saltContaminatesIrrigation=false}): hydrating wins.</b>
 * Distilled / fresh / stagnant keep farmland wet; nearby salt is ignored.
 * Coastal farms need a non-salt source (bucket, river, pond, rain).
 *
 * <p><b>Hardcore ({@code saltContaminatesIrrigation=true}): salt pollutes.</b>
 * Any salt (or vanilla water in a salt biome) in range cancels irrigation even when
 * hydrating water is also present.
 *
 * <p>When hydrating and salt both sit in range, crop growth is slowed by
 * {@code saltCoastGrowthMultiplier} (farmland moisture unchanged under the default config).
 *
 * <p>Vanilla waterlogged blocks always hold {@code minecraft:water}; we treat them by the
 * <em>biome at the fluid position</em>, so seagrass in an ocean does not water crops.
 */
public final class WaterHydration {

    private WaterHydration() {}

    public enum Verdict {
        /** Not water, or irrelevant to irrigation. */
        NONE,
        /** Distilled / fresh / stagnant (or vanilla water in a non-salt biome). */
        HYDRATING,
        /** Salt (or vanilla water in a salt biome). */
        CONTAMINANT
    }

    /** Result of scanning the farmland water window around a block. */
    public record Scan(boolean hydrating, boolean contaminant) {
        /** Fresh/stagnant/distilled and salt both present in range. */
        public boolean mixedCoast() {
            return hydrating && contaminant;
        }
    }

    /** Whether this fluid should count as a farmland water source at {@code fluidPos}. */
    public static Verdict verdict(LevelReader level, BlockPos fluidPos, FluidState fluid) {
        if (fluid.isEmpty()) {
            return Verdict.NONE;
        }

        WaterQuality graded = WaterQuality.fromFluidState(fluid);
        if (graded != null) {
            return verdictOf(graded);
        }

        if (!fluid.is(FluidTags.WATER)) {
            return Verdict.NONE;
        }

        // Vanilla water / waterlogged: grade by biome at the fluid cell.
        return verdictOf(WaterQuality.fromBiome(level.getBiome(fluidPos)));
    }

    private static Verdict verdictOf(WaterQuality quality) {
        if (quality.canHydrateFarmland()) {
            return Verdict.HYDRATING;
        }
        if (quality.contaminatesIrrigation()) {
            return Verdict.CONTAMINANT;
        }
        return Verdict.NONE;
    }

    /** Farmland water window: origin ±4 X/Z, Y..Y+1 (same as vanilla {@code FarmlandBlock}). */
    public static Scan scan(LevelReader level, BlockPos origin) {
        boolean sawHydrating = false;
        boolean sawContaminant = false;

        for (BlockPos fluidPos : BlockPos.betweenClosed(
                origin.offset(-4, 0, -4),
                origin.offset(4, 1, 4)
        )) {
            FluidState fluid = level.getFluidState(fluidPos);
            switch (verdict(level, fluidPos, fluid)) {
                case HYDRATING -> sawHydrating = true;
                case CONTAMINANT -> sawContaminant = true;
                case NONE -> {
                }
            }
            if (sawHydrating && sawContaminant) {
                break;
            }
        }
        return new Scan(sawHydrating, sawContaminant);
    }

    /**
     * Replacement for {@code FarmlandBlock.isNearWater}, including NeoForge water tickets.
     */
    public static boolean isNearHydratingWater(LevelReader level, BlockPos farmlandPos) {
        Scan scan = scan(level, farmlandPos);
        boolean irrigated;
        if (RealismSurvivalConfig.SALT_CONTAMINATES_IRRIGATION.getAsBoolean()) {
            irrigated = scan.hydrating() && !scan.contaminant();
        } else {
            irrigated = scan.hydrating();
        }

        return irrigated || FarmlandWaterManager.hasBlockWaterTicket(level, farmlandPos);
    }

    /**
     * Growth multiplier for crops when hydrating water and salt share the farmland window.
     * Uses the farmland below the crop when present, otherwise the crop position.
     */
    public static float cropGrowthMultiplier(LevelReader level, BlockPos cropPos) {
        BlockPos origin = level.getBlockState(cropPos.below()).getBlock() instanceof FarmlandBlock
                ? cropPos.below()
                : cropPos;
        if (!scan(level, origin).mixedCoast()) {
            return 1.0F;
        }
        return RealismSurvivalConfig.SALT_COAST_GROWTH_MULTIPLIER.get().floatValue();
    }
}
