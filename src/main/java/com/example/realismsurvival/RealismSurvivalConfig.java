package com.example.realismsurvival;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Server-side gameplay switches. Registered as {@code ModConfig.Type.SERVER}, so the values live in the
 * world save ({@code <world>/serverconfig/realismsurvival-server.toml}) and are synced to joining clients.
 *
 * <p>Keep <b>balancing numbers that vary per biome / item / crop</b> out of here — those belong in the
 * data pack (spec §7). This file holds global toggles and engine-level knobs that must not be
 * overridable by a data pack.
 *
 * <p>Time-based thirst restore / penalty damage values are <b>per second</b> and scaled by the
 * evaluation window ({@code tickInterval / 20}). Changing {@code tickInterval} alone must not change
 * per-second gameplay rates.
 */
public final class RealismSurvivalConfig {

    private RealismSurvivalConfig() {}

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue THIRST_ENABLED = BUILDER
            .comment("Master switch for the thirst system (drain, drinking, dehydration damage).")
            .define("thirstEnabled", true);

    public static final ModConfigSpec.BooleanValue TEMPERATURE_ENABLED = BUILDER
            .comment("Master switch for the body temperature system.")
            .define("temperatureEnabled", true);

    public static final ModConfigSpec.IntValue TICK_INTERVAL = BUILDER
            .comment(
                    "How often (in ticks) survival stats are evaluated per player. 20 = once per second.",
                    "Performance / smoothness only: per-second drain, swim restore, and penalty damage",
                    "are scaled by elapsed seconds so rates stay the same when this changes."
            )
            .defineInRange("tickInterval", 20, 1, 200);

    public static final ModConfigSpec.DoubleValue FALLBACK_THIRST_PER_SECOND = BUILDER
            .comment("Thirst points drained per second when NO data-pack biome_thirst entry matches the player's biome.")
            .defineInRange("fallbackThirstPerSecond", 0.05D, 0.0D, 100.0D);

    public static final ModConfigSpec.DoubleValue DEHYDRATION_DAMAGE = BUILDER
            .comment(
                    "Dehydration damage (half-hearts) per second while thirst is 0. Scaled by evaluation window.",
                    "At default tickInterval=20 this matches the old per-evaluation amount of 1.0. Set 0 to disable."
            )
            .defineInRange("dehydrationDamage", 1.0D, 0.0D, 40.0D);

    public static final ModConfigSpec.DoubleValue WORLD_DRINK_BOTTLE_FRACTION = BUILDER
            .comment(
                    "World source sip (shift + empty hand) thirst/temp as a fraction of the grade's bottle drink data.",
                    "Keeps an emergency drink path while bottles/filters/stills stay more efficient. Quality effects stay full."
            )
            .defineInRange("worldDrinkBottleFraction", 0.25D, 0.05D, 1.0D);

    public static final ModConfigSpec.DoubleValue HYPERTHERMIA_THIRST_MULTIPLIER = BUILDER
            .comment("Thirst drain multiplier while body temperature is above the hyperthermia threshold (38.5 C).")
            .defineInRange("hyperthermiaThirstMultiplier", 1.5D, 1.0D, 5.0D);

    /** @deprecated Unused — body temp uses the °C/hour rate table (kept so old serverconfig keys still load). */
    @Deprecated
    public static final ModConfigSpec.DoubleValue TEMPERATURE_CONVERGENCE_RATE = BUILDER
            .comment(
                    "DEPRECATED — ignored. Body temperature uses ambient °C/hour bands (spec §9.3).",
                    "Kept so existing serverconfig files still parse."
            )
            .defineInRange("temperatureConvergenceRate", 0.008D, 0.001D, 1.0D);

    /** @deprecated Unused — see {@link #TEMPERATURE_RATE_MULTIPLIER}. */
    @Deprecated
    public static final ModConfigSpec.DoubleValue TEMPERATURE_AMBIENT_FACTOR = BUILDER
            .comment(
                    "DEPRECATED — ignored. Use temperatureRateMultiplier to scale the °C/hour table.",
                    "Kept so existing serverconfig files still parse."
            )
            .defineInRange("temperatureAmbientFactor", 0.10D, 0.0D, 1.0D);

    public static final ModConfigSpec.DoubleValue TEMPERATURE_RATE_MULTIPLIER = BUILDER
            .comment(
                    "Global scale on environmental body-temperature °C/hour rates (comfort band stays 0).",
                    "1.0 = realistic table midpoints; raise for faster gameplay swings."
            )
            .defineInRange("temperatureRateMultiplier", 1.0D, 0.0D, 10.0D);

    public static final ModConfigSpec.DoubleValue TEMPERATURE_WETNESS_OFFSET = BUILDER
            .comment("Ambient °C offset while the player is in water or rain.")
            .defineInRange("temperatureWetnessOffset", -8.0D, -40.0D, 0.0D);

    public static final ModConfigSpec.DoubleValue TEMPERATURE_SUBMERGED_OFFSET = BUILDER
            .comment("Extra ambient °C while the player's head is underwater (stacked on wetness).")
            .defineInRange("temperatureSubmergedOffset", -4.0D, -40.0D, 0.0D);

    public static final ModConfigSpec.IntValue BIOME_TEMP_BLEND_RADIUS = BUILDER
            .comment(
                    "Chebyshev radius (blocks) for averaging nearby biome base temperatures / season offsets.",
                    "Smooths sharp biome borders (e.g. savanna → beach). 0 = foot biome only. Samples every 4 blocks."
            )
            .defineInRange("biomeTempBlendRadius", 16, 0, 32);

    public static final ModConfigSpec.IntValue HEAT_SCAN_RADIUS = BUILDER
            .comment("Chebyshev radius (blocks) scanned for lava / campfires / furnaces / torches.")
            .defineInRange("heatScanRadius", 4, 0, 8);

    public static final ModConfigSpec.DoubleValue HEAT_SOURCE_MAX_BONUS = BUILDER
            .comment("Maximum ambient °C bonus from nearby heat sources.")
            .defineInRange("heatSourceMaxBonus", 12.0D, 0.0D, 40.0D);

    public static final ModConfigSpec.IntValue COLD_SCAN_RADIUS = BUILDER
            .comment("Chebyshev radius (blocks) scanned for ice / snow / nearby water / #cold_sources.")
            .defineInRange("coldScanRadius", 4, 0, 8);

    public static final ModConfigSpec.DoubleValue COLD_SOURCE_MAX_PENALTY = BUILDER
            .comment("Maximum ambient °C penalty from nearby cold sources (applied as a negative offset).")
            .defineInRange("coldSourceMaxPenalty", 10.0D, 0.0D, 40.0D);

    public static final ModConfigSpec.DoubleValue SWIM_THIRST_DISTILLED_PER_SECOND = BUILDER
            .comment("Thirst restored per second while immersed in distilled water (before dive multiplier).")
            .defineInRange("swimThirstDistilledPerSecond", 0.2D, 0.0D, 10.0D);

    public static final ModConfigSpec.DoubleValue SWIM_THIRST_FRESH_PER_SECOND = BUILDER
            .comment("Thirst restored per second while immersed in fresh water (before dive multiplier).")
            .defineInRange("swimThirstFreshPerSecond", 0.05D, 0.0D, 10.0D);

    /** @deprecated Unused — replaced by {@link #SWIM_THIRST_DISTILLED_PER_SECOND} / {@link #SWIM_THIRST_FRESH_PER_SECOND}. */
    @Deprecated
    public static final ModConfigSpec.DoubleValue SWIM_THIRST_RESTORE_PER_SECOND = BUILDER
            .comment(
                    "DEPRECATED — ignored. Use swimThirstDistilledPerSecond / swimThirstFreshPerSecond.",
                    "Kept so existing serverconfig files still parse."
            )
            .defineInRange("swimThirstRestorePerSecond", 0.10D, 0.0D, 10.0D);

    public static final ModConfigSpec.DoubleValue SWIM_THIRST_DIVE_MULTIPLIER = BUILDER
            .comment("Multiplier on immersion thirst restore while the head is submerged.")
            .defineInRange("swimThirstDiveMultiplier", 1.5D, 1.0D, 5.0D);

    public static final ModConfigSpec.BooleanValue CREATIVE_BYPASS = BUILDER
            .comment("Skip all survival processing for players in Creative or Spectator mode.")
            .define("creativeBypass", true);

    // ---- Respawn (T-02) -----------------------------------------------------------------------------------------------

    public static final ModConfigSpec.BooleanValue RESPAWN_RESET_ON_DEATH = BUILDER
            .comment(
                    "If true, death respawn sets thirst/temperature to respawnThirst / respawnTemperature.",
                    "If false, values copied through death (copyOnDeath) are kept.",
                    "End-portal return (dragon defeated) always keeps current values."
            )
            .define("respawnResetOnDeath", true);

    public static final ModConfigSpec.DoubleValue RESPAWN_THIRST = BUILDER
            .comment("Thirst after a death respawn when respawnResetOnDeath is true.")
            .defineInRange("respawnThirst", 60.0D, 0.0D, 100.0D);

    public static final ModConfigSpec.DoubleValue RESPAWN_TEMPERATURE = BUILDER
            .comment("Body temperature (°C) after a death respawn when respawnResetOnDeath is true.")
            .defineInRange("respawnTemperature", 36.5D, 30.0D, 42.0D);

    // ---- Status penalties (T-08) --------------------------------------------------------------------------------------

    public static final ModConfigSpec.BooleanValue PENALTIES_ENABLED = BUILDER
            .comment("Apply MobEffect debuffs and extreme freeze/heat damage from survival state.")
            .define("penaltiesEnabled", true);

    public static final ModConfigSpec.IntValue PENALTY_EFFECT_DURATION_TICKS = BUILDER
            .comment(
                    "Duration of refreshed penalty effects.",
                    "Should exceed tickInterval so effects do not flicker; runtime clamps to tickInterval + 1 if lower."
            )
            .defineInRange("penaltyEffectDurationTicks", 60, 20, 400);

    public static final ModConfigSpec.DoubleValue THIRST_DEBUFF_THRESHOLD = BUILDER
            .comment("At or below this thirst (but above 0), apply Weakness I. 0 still uses dehydrationDamage.")
            .defineInRange("thirstDebuffThreshold", 20.0D, 0.0D, 100.0D);

    public static final ModConfigSpec.DoubleValue HYPOTHERMIA_THRESHOLD = BUILDER
            .comment("Body temp (°C) below which mild hypothermia starts (Slowness I).")
            .defineInRange("hypothermiaThreshold", 35.0D, 30.0D, 36.5D);

    public static final ModConfigSpec.DoubleValue HYPOTHERMIA_MODERATE_THRESHOLD = BUILDER
            .comment("Below this: Slowness I + Mining Fatigue I.")
            .defineInRange("hypothermiaModerateThreshold", 33.0D, 30.0D, 36.5D);

    public static final ModConfigSpec.DoubleValue HYPOTHERMIA_SEVERE_THRESHOLD = BUILDER
            .comment("At or below this: Slowness II + Mining Fatigue II + freeze damage.")
            .defineInRange("hypothermiaSevereThreshold", 31.0D, 30.0D, 36.5D);

    public static final ModConfigSpec.DoubleValue HYPERTHERMIA_THRESHOLD = BUILDER
            .comment("Body temp (°C) above which mild hyperthermia starts (Hunger I + thirst multiplier).")
            .defineInRange("hyperthermiaThreshold", 38.5D, 36.5D, 42.0D);

    public static final ModConfigSpec.DoubleValue HYPERTHERMIA_MODERATE_THRESHOLD = BUILDER
            .comment("Above this: Hunger I + Weakness I.")
            .defineInRange("hyperthermiaModerateThreshold", 39.5D, 36.5D, 42.0D);

    public static final ModConfigSpec.DoubleValue HYPERTHERMIA_SEVERE_THRESHOLD = BUILDER
            .comment("At or above this: moderate effects + onFire damage.")
            .defineInRange("hyperthermiaSevereThreshold", 41.0D, 36.5D, 42.0D);

    public static final ModConfigSpec.DoubleValue FREEZE_DAMAGE = BUILDER
            .comment(
                    "Freeze damage (half-hearts) per second while at severe hypothermia. Scaled by evaluation window.",
                    "0 disables."
            )
            .defineInRange("freezeDamage", 1.0D, 0.0D, 40.0D);

    public static final ModConfigSpec.DoubleValue HEAT_DAMAGE = BUILDER
            .comment(
                    "Heat damage (half-hearts) per second while at severe hyperthermia. Scaled by evaluation window.",
                    "0 disables."
            )
            .defineInRange("heatDamage", 1.0D, 0.0D, 40.0D);

    // ---- Natural health regen -----------------------------------------------------------------------------------------

    public static final ModConfigSpec.BooleanValue SLOW_NATURAL_REGEN = BUILDER
            .comment(
                    "Slow vanilla food/saturation HP regen. Potions, Regeneration effect, and heals >= 4 HP are unchanged.",
                    "Independent of thirstEnabled / temperatureEnabled: those toggles only skip their own regen gates.",
                    "Penalty damage (starve / thirst / temp) is never reduced."
            )
            .define("slowNaturalRegen", true);

    public static final ModConfigSpec.DoubleValue NATURAL_REGEN_MULTIPLIER = BUILDER
            .comment("Multiplier applied to natural heal amounts. 0.25 ≈ 4× slower. 0 disables natural regen entirely.")
            .defineInRange("naturalRegenMultiplier", 0.25D, 0.0D, 1.0D);

    public static final ModConfigSpec.DoubleValue NATURAL_REGEN_MIN_THIRST = BUILDER
            .comment(
                    "Natural regen is blocked while thirst is below this value (recovery items still work).",
                    "Ignored when thirstEnabled is false."
            )
            .defineInRange("naturalRegenMinThirst", 40.0D, 0.0D, 100.0D);

    // ---- Water / irrigation -------------------------------------------------------------------------------------------

    public static final ModConfigSpec.IntValue FILTER_PROCESS_TICKS = BUILDER
            .comment("Ticks per bottle cycle in the water filter (default 200 = 10s).")
            .defineInRange("filterProcessTicks", 200, 20, 72000);

    public static final ModConfigSpec.IntValue STILL_PROCESS_TICKS = BUILDER
            .comment("Ticks per bottle cycle in the water still (default 400 = 20s). Full-bucket output uses 3× this.")
            .defineInRange("stillProcessTicks", 400, 20, 72000);

    public static final ModConfigSpec.DoubleValue STILL_SOLID_PROCESS_MULTIPLIER = BUILDER
            .comment(
                    "Extra time multiplier when the still input is ice or snow (melt + distill).",
                    "Output is still the same distilled water; only duration changes."
            )
            .defineInRange("stillSolidProcessMultiplier", 1.75D, 1.0D, 5.0D);

    public static final ModConfigSpec.BooleanValue SALT_CONTAMINATES_IRRIGATION = BUILDER
            .comment(
                    "Farmland irrigation when both hydrating and salt water are in range (4 blocks):",
                    "false (default) = hydrating wins — distilled/fresh/stagnant keeps farmland wet.",
                    "true = salt pollutes — any salt (or vanilla water in ocean/beach biomes) cancels irrigation."
            )
            .define("saltContaminatesIrrigation", false);

    public static final ModConfigSpec.DoubleValue CROP_GROWTH_MULTIPLIER_MAX = BUILDER
            .comment(
                    "Upper bound on the combined crop growth multiplier (biome × light × season × rain × coast).",
                    "Above 1.0 the excess is the chance to force a growth stage on a random tick (1.75 → 75%)."
            )
            .defineInRange("cropGrowthMultiplierMax", 1.75D, 1.0D, 2.0D);

    public static final ModConfigSpec.DoubleValue SALT_COAST_GROWTH_MULTIPLIER = BUILDER
            .comment(
                    "Crop growth multiplier when hydrating water AND salt are both in the farmland window.",
                    "Does not dry farmland (see saltContaminatesIrrigation). 1.0 = no penalty, 0.75 ≈ 25% slower."
            )
            .defineInRange("saltCoastGrowthMultiplier", 0.75D, 0.0D, 1.0D);

    public static final ModConfigSpec.BooleanValue FRESH_POOL_STAGNATION_ENABLED = BUILDER
            .comment(
                    "Small fresh-water source pools stagnate over time (spec §9.9).",
                    "Connected fresh sources below freshPoolStagnateThreshold become stagnant after a size-scaled grace."
            )
            .define("freshPoolStagnationEnabled", true);

    public static final ModConfigSpec.IntValue FRESH_POOL_STAGNATE_THRESHOLD = BUILDER
            .comment(
                    "Connected fresh SOURCE cells at or above this count never stagnate (rivers / large reservoirs).",
                    "Below this, grace = size × freshPoolGraceTicksPerSource."
            )
            .defineInRange("freshPoolStagnateThreshold", 64, 2, 1024);

    public static final ModConfigSpec.IntValue FRESH_POOL_GRACE_TICKS_PER_SOURCE = BUILDER
            .comment(
                    "Grace ticks per connected fresh source before the pool stagnates.",
                    "Default 24000 = 1 Minecraft day per source (size 1 → 1 day, size 32 → 32 days)."
            )
            .defineInRange("freshPoolGraceTicksPerSource", 24000, 20, 720000);

    public static final ModConfigSpec.IntValue FRESH_POOL_SCAN_INTERVAL_TICKS = BUILDER
            .comment("How often (ticks) loaded chunks are scanned for fresh pools that may stagnate.")
            .defineInRange("freshPoolScanIntervalTicks", 200, 20, 72000);

    public static final ModConfigSpec.IntValue FRESH_POOL_CHUNKS_PER_SCAN = BUILDER
            .comment("Max loaded chunks inspected per stagnation scan pass.")
            .defineInRange("freshPoolChunksPerScan", 4, 1, 64);

    public static final ModConfigSpec.BooleanValue SUPPLY_COUNTER_ENABLED = BUILDER
            .comment(
                    "Supply merchants buy through the RS sell counter (sneak + use on the merchant).",
                    "When true, Q Shop supply stalls are buy-only so sell limits cannot be used twice.",
                    "When false, Q Shop stalls keep their SELL entries and the counter does not open."
            )
            .define("supplyCounterEnabled", true);

    public static final ModConfigSpec.BooleanValue WAYSTONE_GOLD_TOLL_ENABLED = BUILDER
            .comment(
                    "Charge inventory gold for Waystone / warp-stone teleports (scrolls stay free).",
                    "Disable Waystones XP costs in waystones-common.toml (enableXpCosts=false)."
            )
            .define("waystoneGoldTollEnabled", true);

    public static final ModConfigSpec.IntValue WAYSTONE_GOLD_TOLL_BASE = BUILDER
            .comment("Flat gold cost added to every paid Waystone teleport.")
            .defineInRange("waystoneGoldTollBase", 15, 0, 10_000);

    public static final ModConfigSpec.IntValue WAYSTONE_GOLD_TOLL_PER_100_BLOCKS = BUILDER
            .comment("Extra gold per 100 blocks of horizontal+vertical distance (rounded up).")
            .defineInRange("waystoneGoldTollPer100Blocks", 1, 0, 100);

    public static final ModConfigSpec.IntValue WAYSTONE_GOLD_TOLL_MAX = BUILDER
            .comment("Hard cap on a single Waystone gold toll.")
            .defineInRange("waystoneGoldTollMax", 80, 0, 10_000);

    public static final ModConfigSpec.IntValue DAILY_BOARD_CLAIM_LIMIT = BUILDER
            .comment("Max paid daily-board quest claims per Overworld day (game time).")
            .defineInRange("dailyBoardClaimLimit", 3, 1, 20);

    public static final ModConfigSpec.BooleanValue VILLAGE_DOCTOR_SPAWN = BUILDER
            .comment("Ensure each Overworld village gets at least one clinic table and doctor villager.")
            .define("villageDoctorSpawn", true);

    public static final ModConfigSpec SPEC = BUILDER.build();

    /**
     * Logs inconsistent threshold / duration combinations. Does not rewrite the TOML;
     * gameplay code clamps ordering and effect duration at use sites.
     */
    public static void logValidationWarnings() {
        int interval = TICK_INTERVAL.getAsInt();
        int effectDuration = PENALTY_EFFECT_DURATION_TICKS.getAsInt();
        if (effectDuration <= interval) {
            LOGGER.warn(
                    "[RealismSurvival] penaltyEffectDurationTicks ({}) should exceed tickInterval ({}); "
                            + "runtime will use tickInterval + 1 so effects do not flicker.",
                    effectDuration,
                    interval
            );
        }

        double hypoMild = HYPOTHERMIA_THRESHOLD.getAsDouble();
        double hypoMod = HYPOTHERMIA_MODERATE_THRESHOLD.getAsDouble();
        double hypoSev = HYPOTHERMIA_SEVERE_THRESHOLD.getAsDouble();
        if (!(hypoSev <= hypoMod && hypoMod <= hypoMild)) {
            LOGGER.warn(
                    "[RealismSurvival] Hypothermia thresholds should satisfy severe ≤ moderate ≤ mild "
                            + "(got severe={}, moderate={}, mild={}). Runtime will sort them.",
                    hypoSev,
                    hypoMod,
                    hypoMild
            );
        }

        double hyperMild = HYPERTHERMIA_THRESHOLD.getAsDouble();
        double hyperMod = HYPERTHERMIA_MODERATE_THRESHOLD.getAsDouble();
        double hyperSev = HYPERTHERMIA_SEVERE_THRESHOLD.getAsDouble();
        if (!(hyperMild <= hyperMod && hyperMod <= hyperSev)) {
            LOGGER.warn(
                    "[RealismSurvival] Hyperthermia thresholds should satisfy mild ≤ moderate ≤ severe "
                            + "(got mild={}, moderate={}, severe={}). Runtime will sort them.",
                    hyperMild,
                    hyperMod,
                    hyperSev
            );
        }
    }

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(RealismSurvivalConfig.class);
}
