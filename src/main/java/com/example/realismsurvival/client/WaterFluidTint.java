package com.example.realismsurvival.client;

import com.example.realismsurvival.api.water.WaterApi;
import com.example.realismsurvival.api.water.WaterQuality;
import com.example.realismsurvival.water.WaterColors;
import java.util.Objects;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.client.fluid.FluidTintSource;
import org.jspecify.annotations.Nullable;

/**
 * Soft mid-band colour where water qualities meet on the shared graded fluid.
 * Alone → solid self colour. Near another grade → Euclidean neighbourhood average
 * (flowing mixes freely; sources keep more identity but still soften across a wide band).
 */
public final class WaterFluidTint implements FluidTintSource {

    public static final WaterFluidTint INSTANCE = new WaterFluidTint();

    /** How far (blocks) a foreign grade can trigger / contribute to blending. */
    private static final int BLEND_RADIUS = 10;
    /** Source cells: keep self colour dominant so open ocean stays readable. */
    private static final float SOURCE_SELF_WEIGHT = 12.0F;
    /** Source cells: how much foreign grades pull the tint (higher = softer shore). */
    private static final float SOURCE_FOREIGN_SCALE = 0.40F;

    private WaterFluidTint() {}

    @Override
    public int color(FluidState state) {
        return withAlpha(baseRgb(null, BlockPos.ZERO, state));
    }

    @Override
    public int colorInWorld(FluidState fluidState, BlockState blockState, BlockAndTintGetter level, BlockPos pos) {
        int selfRgb = baseRgb(level, pos, fluidState);
        if (!isBlendableWater(fluidState)) {
            return withAlpha(selfRgb);
        }

        WaterQuality selfGrade = gradeOf(fluidState, level, pos);
        if (selfGrade == null || !hasForeignGradeNearby(level, pos, selfGrade, BLEND_RADIUS)) {
            return withAlpha(selfRgb);
        }

        if (fluidState.isSource()) {
            return withAlpha(blendSource(level, pos, selfGrade, selfRgb));
        }
        return withAlpha(blendFlowing(level, pos, selfRgb));
    }

    private static int blendFlowing(BlockAndTintGetter level, BlockPos pos, int selfRgb) {
        float totalRed = ARGB.red(selfRgb);
        float totalGreen = ARGB.green(selfRgb);
        float totalBlue = ARGB.blue(selfRgb);
        float totalWeight = 1.0F;

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -BLEND_RADIUS; dx <= BLEND_RADIUS; dx++) {
            for (int dz = -BLEND_RADIUS; dz <= BLEND_RADIUS; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                float dist = Mth.sqrt(dx * dx + dz * dz);
                if (dist > BLEND_RADIUS) {
                    continue;
                }
                cursor.set(pos.getX() + dx, pos.getY(), pos.getZ() + dz);
                FluidState sample = level.getFluidState(cursor);
                if (!isBlendableWater(sample)) {
                    continue;
                }
                float falloff = BLEND_RADIUS - dist + 1.0F;
                float weight = falloff * falloff;
                int rgb = baseRgb(level, cursor, sample);
                totalRed += ARGB.red(rgb) * weight;
                totalGreen += ARGB.green(rgb) * weight;
                totalBlue += ARGB.blue(rgb) * weight;
                totalWeight += weight;
            }
        }

        return ARGB.color(
                Mth.clamp(Math.round(totalRed / totalWeight), 0, 255),
                Mth.clamp(Math.round(totalGreen / totalWeight), 0, 255),
                Mth.clamp(Math.round(totalBlue / totalWeight), 0, 255)
        );
    }

    private static int blendSource(
            BlockAndTintGetter level,
            BlockPos pos,
            WaterQuality selfGrade,
            int selfRgb
    ) {
        float totalRed = ARGB.red(selfRgb) * SOURCE_SELF_WEIGHT;
        float totalGreen = ARGB.green(selfRgb) * SOURCE_SELF_WEIGHT;
        float totalBlue = ARGB.blue(selfRgb) * SOURCE_SELF_WEIGHT;
        float totalWeight = SOURCE_SELF_WEIGHT;

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -BLEND_RADIUS; dx <= BLEND_RADIUS; dx++) {
            for (int dz = -BLEND_RADIUS; dz <= BLEND_RADIUS; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                float dist = Mth.sqrt(dx * dx + dz * dz);
                if (dist > BLEND_RADIUS) {
                    continue;
                }
                cursor.set(pos.getX() + dx, pos.getY(), pos.getZ() + dz);
                FluidState sample = level.getFluidState(cursor);
                if (!isBlendableWater(sample)) {
                    continue;
                }
                WaterQuality sampleGrade = gradeOf(sample, level, cursor);
                if (sampleGrade == null) {
                    continue;
                }
                float falloff = BLEND_RADIUS - dist + 1.0F;
                float weight = falloff * falloff;
                if (!Objects.equals(sampleGrade, selfGrade)) {
                    weight *= SOURCE_FOREIGN_SCALE;
                }
                int rgb = baseRgb(level, cursor, sample);
                totalRed += ARGB.red(rgb) * weight;
                totalGreen += ARGB.green(rgb) * weight;
                totalBlue += ARGB.blue(rgb) * weight;
                totalWeight += weight;
            }
        }

        return ARGB.color(
                Mth.clamp(Math.round(totalRed / totalWeight), 0, 255),
                Mth.clamp(Math.round(totalGreen / totalWeight), 0, 255),
                Mth.clamp(Math.round(totalBlue / totalWeight), 0, 255)
        );
    }

    private static boolean hasForeignGradeNearby(
            BlockAndTintGetter level,
            BlockPos pos,
            WaterQuality selfGrade,
            int radius
    ) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        float radiusSq = radius * radius;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                if (dx * dx + dz * dz > radiusSq) {
                    continue;
                }
                cursor.set(pos.getX() + dx, pos.getY(), pos.getZ() + dz);
                FluidState sample = level.getFluidState(cursor);
                if (!isBlendableWater(sample)) {
                    continue;
                }
                WaterQuality sampleGrade = gradeOf(sample, level, cursor);
                if (sampleGrade != null && !Objects.equals(sampleGrade, selfGrade)) {
                    return true;
                }
            }
        }
        return false;
    }

    static int baseRgb(@Nullable BlockAndTintGetter level, BlockPos pos, FluidState fluid) {
        WaterQuality graded = WaterApi.fromFluidState(fluid);
        if (graded != null) {
            return WaterColors.of(graded);
        }
        Holder<Biome> biome = WaterTintBiome.get(level, pos);
        if (fluid.is(FluidTags.WATER) && biome != null) {
            return WaterColors.of(WaterQuality.fromBiome(biome));
        }
        if (fluid.is(FluidTags.WATER)) {
            return WaterColors.FRESH;
        }
        return 0x3F76E4;
    }

    private static @Nullable WaterQuality gradeOf(FluidState fluid, BlockAndTintGetter level, BlockPos pos) {
        WaterQuality graded = WaterApi.fromFluidState(fluid);
        if (graded != null) {
            return graded;
        }
        Holder<Biome> biome = WaterTintBiome.get(level, pos);
        if (fluid.is(FluidTags.WATER) && biome != null) {
            return WaterQuality.fromBiome(biome);
        }
        return null;
    }

    private static boolean isBlendableWater(FluidState fluid) {
        return !fluid.isEmpty()
                && (WaterApi.isGradedWaterFluid(fluid.getType()) || fluid.is(FluidTags.WATER));
    }

    private static int withAlpha(int rgb) {
        return 0xFF000000 | (rgb & 0xFFFFFF);
    }
}
