package com.example.realismsurvival.environment;

import com.example.realismsurvival.RealismSurvivalConfig;
import com.example.realismsurvival.api.ModBlockTags;
import com.example.realismsurvival.api.environment.EnvironmentContext;
import com.example.realismsurvival.api.event.TemperatureCalculateEvent;
import com.example.realismsurvival.component.SurvivalData;
import com.example.realismsurvival.data.SurvivalDataManager;
import com.example.realismsurvival.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Body temperature from environmental ambient via °C/hour rates (spec §9.3).
 *
 * <p>{@link EnvironmentContext} is passed through for addon events; core ambient also uses
 * contact wetness / head-submersion and nearby heat/cold block scans.
 */
public final class TemperatureEvaluator {

    private TemperatureEvaluator() {}

    /** Vanilla biome base temperatures at or above this get the stronger desert night swing. */
    private static final float DESERT_BASE_TEMPERATURE = 1.5F;

    /** Default °C contribution for addon blocks in {@link ModBlockTags#HEAT_SOURCES}. */
    private static final float DEFAULT_TAGGED_HEAT = 2.0F;

    /** Default °C contribution for blocks in {@link ModBlockTags#COLD_SOURCES} without a hard weight. */
    private static final float DEFAULT_TAGGED_COLD = 2.0F;

    /** Cold-water immersion body-temp rate (°C/hour) before insulation (½-real × 1.5). */
    private static final float IMMERSION_RATE_C_PER_HOUR = -4.5F;

    /** Horizontal speed² threshold (~0.05 b/tick) to count as moving. */
    private static final double MOVING_HORIZONTAL_SPEED_SQR = 0.0025D;

    /**
     * Cap on |activity| °C/h — must stay below the smallest nonzero air-band midpoint (0.1125)
     * so environmental terms dominate whenever they are active.
     */
    private static final float ACTIVITY_MAX_C_PER_HOUR = 0.05F;
    private static final float ACTIVITY_SWIM_C_PER_HOUR = 0.04F;
    private static final float ACTIVITY_SPRINT_C_PER_HOUR = 0.05F;
    /** Walk pull toward normal: rate = clamp((36.5 − body) × this, ±{@link #ACTIVITY_MAX_C_PER_HOUR}). */
    private static final float WALK_CONVERGE_PER_HOUR = 0.05F;

    /** Player exertion for the activity heat term. */
    public enum Activity {
        IDLE,
        WALK,
        SWIM,
        SPRINT
    }

    public static SurvivalData evaluate(
            ServerPlayer player,
            ServerLevel level,
            Holder<Biome> biome,
            EnvironmentContext environment,
            SurvivalData data,
            float seconds
    ) {
        AmbientBreakdown ambientParts = computeAmbientBreakdown(player, level, biome);
        float insulation = SurvivalDataManager.wornInsulation(player);

        TemperatureCalculateEvent calc = new TemperatureCalculateEvent(
                player,
                level,
                biome,
                data,
                environment,
                ambientParts.total(),
                insulation,
                ambientParts.altitude(),
                ambientParts.dayNight(),
                ambientParts.wet(),
                ambientParts.heat(),
                ambientParts.cold(),
                ambientParts.season()
        );
        NeoForge.EVENT_BUS.post(calc);
        float ambient = calc.getAmbient();
        insulation = Mth.clamp(calc.getInsulation(), 0.0F, 1.0F);

        Activity activity = resolveActivity(player);
        float rate = bodyTempRatePerHour(ambient, insulation, player.isInWater(), activity, data.temperature());
        float multiplier = (float) RealismSurvivalConfig.TEMPERATURE_RATE_MULTIPLIER.getAsDouble();
        float next = data.temperature() + rate * multiplier * (Math.max(0.0F, seconds) / 3600.0F);
        return data.withTemperature(next);
    }

    /**
     * Body-temperature change in °C per hour (before {@code temperatureRateMultiplier}):
     * insulated environmental rate + weaker activity term (|activity| ≤ 0.05 &lt; min air band 0.075).
     *
     * <p>Walk pulls toward {@link SurvivalData#TEMPERATURE_DEFAULT}. Comfort 18–25 °C → env 0.
     * Immersion ({@code inWater}) uses −4.5 °C/h instead of the air band.
     */
    public static float bodyTempRatePerHour(
            float ambientC, float insulation, boolean inWater, Activity activity, float bodyTempC
    ) {
        float env = environmentalRatePerHour(ambientC, insulation, inWater);
        return env + activityRatePerHour(activity, bodyTempC);
    }

    /** @deprecated Use {@link #bodyTempRatePerHour(float, float, boolean, Activity, float)}. */
    @Deprecated
    public static float bodyTempRatePerHour(
            float ambientC, float insulation, boolean inWater, Activity activity
    ) {
        return bodyTempRatePerHour(ambientC, insulation, inWater, activity, SurvivalData.TEMPERATURE_DEFAULT);
    }

    /** @deprecated Use {@link #bodyTempRatePerHour(float, float, boolean, Activity, float)}. */
    @Deprecated
    public static float bodyTempRatePerHour(
            float ambientC, float insulation, boolean inWater, boolean sprinting
    ) {
        return bodyTempRatePerHour(
                ambientC, insulation, inWater, sprinting ? Activity.SPRINT : Activity.IDLE, SurvivalData.TEMPERATURE_DEFAULT
        );
    }

    static float environmentalRatePerHour(float ambientC, float insulation, boolean inWater) {
        float ins = Mth.clamp(insulation, 0.0F, 1.0F);
        if (inWater) {
            return IMMERSION_RATE_C_PER_HOUR * (1.0F - ins);
        }
        float base = airBandRatePerHour(ambientC);
        if (base == 0.0F) {
            return 0.0F;
        }
        float scale = ambientC >= 40.0F ? (1.0F - ins * 0.5F) : (1.0F - ins);
        return base * scale;
    }

    /**
     * Activity term (°C/h). Magnitude capped below environmental band midpoints.
     * Walk converges toward normal body temperature; swim/sprint add mild heat.
     */
    public static float activityRatePerHour(Activity activity, float bodyTempC) {
        return switch (activity) {
            case SPRINT -> ACTIVITY_SPRINT_C_PER_HOUR;
            case SWIM -> ACTIVITY_SWIM_C_PER_HOUR;
            case WALK -> walkConvergeRatePerHour(bodyTempC);
            case IDLE -> 0.0F;
        };
    }

    /** Toward {@link SurvivalData#TEMPERATURE_DEFAULT}, clamped to ±{@link #ACTIVITY_MAX_C_PER_HOUR}. */
    static float walkConvergeRatePerHour(float bodyTempC) {
        float gap = SurvivalData.TEMPERATURE_DEFAULT - bodyTempC;
        return Mth.clamp(gap * WALK_CONVERGE_PER_HOUR, -ACTIVITY_MAX_C_PER_HOUR, ACTIVITY_MAX_C_PER_HOUR);
    }

    /** Highest matching tier: sprint &gt; swim &gt; walk &gt; idle. */
    public static Activity resolveActivity(Player player) {
        if (player.isSprinting()) {
            return Activity.SPRINT;
        }
        boolean moving = isMovingHorizontally(player);
        if (player.isSwimming() || (player.isInWater() && moving)) {
            return Activity.SWIM;
        }
        if (moving && (player.onGround() || player.onClimbable())) {
            return Activity.WALK;
        }
        return Activity.IDLE;
    }

    private static boolean isMovingHorizontally(Player player) {
        return player.getDeltaMovement().horizontalDistanceSqr() > MOVING_HORIZONTAL_SPEED_SQR;
    }

    /** Piecewise air ambient → °C/h (½-real midpoints × 1.5) with linear lerp between band edges. */
    static float airBandRatePerHour(float ambientC) {
        if (ambientC <= -10.0F) {
            return -1.6875F;
        }
        if (ambientC < 0.0F) {
            return lerpRate(ambientC, -10.0F, 0.0F, -1.6875F, -0.5625F);
        }
        if (ambientC < 10.0F) {
            return -0.5625F;
        }
        if (ambientC < 15.0F) {
            return -0.1125F;
        }
        if (ambientC < 18.0F) {
            return lerpRate(ambientC, 15.0F, 18.0F, -0.1125F, 0.0F);
        }
        if (ambientC <= 25.0F) {
            return 0.0F;
        }
        if (ambientC < 32.0F) {
            return lerpRate(ambientC, 25.0F, 32.0F, 0.0F, 0.1125F);
        }
        if (ambientC < 35.0F) {
            return 0.1125F;
        }
        if (ambientC < 40.0F) {
            return lerpRate(ambientC, 35.0F, 40.0F, 0.1125F, 0.5625F);
        }
        if (ambientC < 45.0F) {
            return lerpRate(ambientC, 40.0F, 45.0F, 0.5625F, 2.25F);
        }
        return 2.25F;
    }

    private static float lerpRate(float x, float x0, float x1, float y0, float y1) {
        float t = (x - x0) / (x1 - x0);
        return y0 + (y1 - y0) * Mth.clamp(t, 0.0F, 1.0F);
    }

    /**
     * Ambient air temperature in °C around the player. Works on client and server.
     * Does not fire {@link TemperatureCalculateEvent} (use {@link #evaluate} for the full path).
     */
    public static float computeAmbient(Player player, Level level, Holder<Biome> biome) {
        return computeAmbientBreakdown(player, level, biome).total();
    }

    /**
     * Core ambient terms. Addons adjust the summed result via {@link TemperatureCalculateEvent}.
     */
    public static AmbientBreakdown computeAmbientBreakdown(
            Player player,
            Level level,
            Holder<Biome> biome
    ) {
        BlockPos pos = player.blockPosition();
        int y = pos.getY();
        int sea = level.getSeaLevel();
        Season season = SeasonCalendar.from(level).season();
        int blendRadius = RealismSurvivalConfig.BIOME_TEMP_BLEND_RADIUS.getAsInt();
        ClimateTemperature.BiomeClimateBlend blend =
                ClimateTemperature.blendNearby(level, pos, blendRadius, season);
        // Fall back to the foot biome if blend is disabled (radius 0 already returns foot).
        float base = blend.vanillaBase();
        float biomeAmbient = ClimateTemperature.toCelsius(base);
        float altitude = ClimateTemperature.altitudeCelsiusOffset(base, y, sea);
        float dayNight = dayNightTerm(level.getOverworldClockTime(), base);
        float wet = contactWetTerm(player, level);
        float heat = nearbyHeatBonus(level, pos, RealismSurvivalConfig.HEAT_SCAN_RADIUS.getAsInt());
        // Skip open-water cells while already immersed — wetness covers contact cooling.
        float cold = nearbyColdPenalty(
                level,
                pos,
                RealismSurvivalConfig.COLD_SCAN_RADIUS.getAsInt(),
                player.isInWater()
        );
        float seasonTerm = blend.seasonOffsetC();
        return new AmbientBreakdown(biomeAmbient, altitude, dayNight, wet, heat, cold, seasonTerm);
    }

    /**
     * @param biomeBase sea-level climate °C from {@link ClimateTemperature#toCelsius}
     * @param altitude  vanilla altitude lapse expressed in °C (usually ≤ 0)
     * @param wet       wetness / submersion / lava / fire offsets
     * @param heat      nearby heat sources (≥ 0)
     * @param cold      nearby cold sources (≤ 0)
     * @param season    climate-zone × season offset (°C)
     */
    public record AmbientBreakdown(
            float biomeBase, float altitude, float dayNight, float wet, float heat, float cold, float season
    ) {
        public float total() {
            return biomeBase + altitude + dayNight + wet + heat + cold + season;
        }
    }

    /**
     * Continuous day/night swing around noon. Noon → 0, midnight → −amplitude
     * (6 °C normally, 12 °C in hot biomes).
     */
    static float dayNightTerm(long dayTime, float biomeBaseTemperature) {
        float amplitude = biomeBaseTemperature >= DESERT_BASE_TEMPERATURE ? 12.0F : 6.0F;
        long timeOfDay = dayTime % 24_000L;
        double phase = Math.cos((timeOfDay - 6000L) / 12_000.0 * Math.PI);
        return (float) ((phase - 1.0) * 0.5 * amplitude);
    }

    /** Rain / body-in-water / head-submerged / lava / fire contact offsets. */
    static float contactWetTerm(Player player, Level level) {
        float wet = 0.0F;
        if (player.isInWaterOrRain() || player.isInPowderSnow) {
            wet += (float) RealismSurvivalConfig.TEMPERATURE_WETNESS_OFFSET.getAsDouble();
        }
        FluidState headFluid = level.getFluidState(BlockPos.containing(player.getX(), player.getEyeY(), player.getZ()));
        if (!headFluid.isEmpty() && headFluid.is(FluidTags.WATER)) {
            wet += (float) RealismSurvivalConfig.TEMPERATURE_SUBMERGED_OFFSET.getAsDouble();
        }
        if (player.isInLava()) {
            wet += 18.0F;
        } else if (player.isOnFire()) {
            wet += 10.0F;
        }
        return wet;
    }

    /**
     * Weighted nearby heat, clamped to the config max (default +12 °C).
     */
    static float nearbyHeatBonus(LevelReader level, BlockPos origin, int radius) {
        float max = (float) RealismSurvivalConfig.HEAT_SOURCE_MAX_BONUS.getAsDouble();
        if (max <= 0.0F || radius < 0) {
            return 0.0F;
        }

        float heat = 0.0F;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    heat += heatContribution(level.getBlockState(cursor));
                    if (heat >= max) {
                        return max;
                    }
                }
            }
        }
        return Math.min(heat, max);
    }

    /**
     * Weighted nearby cold as a negative ambient offset, clamped to {@code -coldSourceMaxPenalty}.
     *
     * @param skipWaterFluids when true (player already in water), open-water cells are ignored so
     *                        immersion cooling is not double-counted with {@link #contactWetTerm}
     */
    static float nearbyColdPenalty(LevelReader level, BlockPos origin, int radius, boolean skipWaterFluids) {
        float max = (float) RealismSurvivalConfig.COLD_SOURCE_MAX_PENALTY.getAsDouble();
        if (max <= 0.0F || radius < 0) {
            return 0.0F;
        }

        float cold = 0.0F;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    cold += coldContribution(level.getBlockState(cursor), skipWaterFluids);
                    if (cold >= max) {
                        return -max;
                    }
                }
            }
        }
        return -Math.min(cold, max);
    }

    private static float heatContribution(BlockState state) {
        float weighted = weightedHeat(state);
        if (weighted > 0.0F) {
            return weighted;
        }
        if (state.is(ModBlockTags.HEAT_SOURCES)) {
            if (state.hasProperty(BlockStateProperties.LIT) && !state.getValue(BlockStateProperties.LIT)) {
                return 0.0F;
            }
            return DEFAULT_TAGGED_HEAT;
        }
        return 0.0F;
    }

    private static float coldContribution(BlockState state, boolean skipWaterFluids) {
        float weighted = weightedCold(state, skipWaterFluids);
        if (weighted > 0.0F) {
            return weighted;
        }
        if (state.is(ModBlockTags.COLD_SOURCES)) {
            return DEFAULT_TAGGED_COLD;
        }
        return 0.0F;
    }

    private static float weightedHeat(BlockState state) {
        if (state.is(Blocks.LAVA) || state.getFluidState().is(FluidTags.LAVA)) {
            return 8.0F;
        }
        if (state.is(Blocks.MAGMA_BLOCK) || state.is(Blocks.LAVA_CAULDRON)) {
            return 4.0F;
        }
        if (state.is(BlockTags.CAMPFIRES) && isLit(state)) {
            return 6.0F;
        }
        if (state.is(BlockTags.FIRE)) {
            return 5.0F;
        }
        if ((state.is(Blocks.FURNACE) || state.is(Blocks.BLAST_FURNACE) || state.is(Blocks.SMOKER)) && isLit(state)) {
            return 4.0F;
        }
        if (state.is(Blocks.TORCH)
                || state.is(Blocks.WALL_TORCH)
                || state.is(Blocks.SOUL_TORCH)
                || state.is(Blocks.SOUL_WALL_TORCH)
                || state.is(Blocks.LANTERN)
                || state.is(Blocks.SOUL_LANTERN)
                || state.is(Blocks.JACK_O_LANTERN)) {
            return 1.0F;
        }
        return 0.0F;
    }

    private static float weightedCold(BlockState state, boolean skipWaterFluids) {
        if (state.is(Blocks.BLUE_ICE)) {
            return 7.0F;
        }
        if (state.is(Blocks.PACKED_ICE)) {
            return 6.0F;
        }
        if (state.is(Blocks.ICE)
                || state.is(Blocks.FROSTED_ICE)
                || state.is(ModBlocks.GRADED_ICE.get())) {
            return 5.0F;
        }
        if (state.is(Blocks.POWDER_SNOW)) {
            return 4.0F;
        }
        if (state.is(Blocks.SNOW_BLOCK)) {
            return 3.0F;
        }
        if (state.is(Blocks.SNOW)) {
            return 1.0F;
        }
        // Standing near open water cools slightly; skipped while already immersed.
        if (!skipWaterFluids && state.getFluidState().is(FluidTags.WATER)) {
            return 1.5F;
        }
        return 0.0F;
    }

    private static boolean isLit(BlockState state) {
        return state.hasProperty(BlockStateProperties.LIT) && state.getValue(BlockStateProperties.LIT);
    }
}
