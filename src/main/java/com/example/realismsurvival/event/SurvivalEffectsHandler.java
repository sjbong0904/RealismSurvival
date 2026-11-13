package com.example.realismsurvival.event;

import com.example.realismsurvival.RealismSurvivalConfig;
import com.example.realismsurvival.component.SurvivalData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * Status penalties from thirst / temperature (spec T-08 / §9.3).
 *
 * <p>Called once per survival evaluation after stats are written. Effects use a short duration that is
 * refreshed while the condition holds, so they expire naturally when the player recovers — no manual
 * {@code removeEffect} bookkeeping. Extreme freeze/heat damage is applied as a <b>per-second</b> rate
 * scaled by the evaluation window so {@code tickInterval} does not change DPS.
 *
 * <pre>
 * thirst ≤ thirstDebuffThreshold → WEAKNESS I
 * thirst = 0                     → dryOut damage (SurvivalTickHandler)
 *
 * temp &lt; hypo mild               → SLOWNESS I
 * temp &lt; hypo moderate           → SLOWNESS I + MINING_FATIGUE I
 * temp ≤ hypo severe             → SLOWNESS II + MINING_FATIGUE II + freeze damage
 *
 * temp &gt; hyper mild              → thirst × mult (SurvivalTickHandler) + HUNGER I
 * temp &gt; hyper moderate          → HUNGER I + WEAKNESS I
 * temp ≥ hyper severe            → + onFire damage
 * </pre>
 */
public final class SurvivalEffectsHandler {

    private SurvivalEffectsHandler() {}

    public static void apply(ServerPlayer player, ServerLevel level, SurvivalData data, float windowSeconds) {
        if (!RealismSurvivalConfig.PENALTIES_ENABLED.getAsBoolean()) {
            return;
        }

        int duration = effectivePenaltyDurationTicks();
        if (RealismSurvivalConfig.THIRST_ENABLED.getAsBoolean()) {
            applyThirstPenalties(player, data, duration);
        }
        if (RealismSurvivalConfig.TEMPERATURE_ENABLED.getAsBoolean()) {
            applyTemperaturePenalties(player, level, data, duration, windowSeconds);
        }
    }

    /** Ensures refreshed effects outlive the next evaluation tick. */
    static int effectivePenaltyDurationTicks() {
        int configured = RealismSurvivalConfig.PENALTY_EFFECT_DURATION_TICKS.getAsInt();
        int interval = RealismSurvivalConfig.TICK_INTERVAL.getAsInt();
        return Math.max(configured, interval + 1);
    }

    private static void applyThirstPenalties(ServerPlayer player, SurvivalData data, int duration) {
        float threshold = (float) RealismSurvivalConfig.THIRST_DEBUFF_THRESHOLD.getAsDouble();
        if (data.thirst() <= threshold && data.thirst() > SurvivalData.THIRST_MIN) {
            refresh(player, new MobEffectInstance(MobEffects.WEAKNESS, duration, 0, false, true, true));
        }
        // thirst == 0 damage is handled in SurvivalTickHandler (dryOut)
    }

    private static void applyTemperaturePenalties(
            ServerPlayer player,
            ServerLevel level,
            SurvivalData data,
            int duration,
            float windowSeconds
    ) {
        float temp = data.temperature();
        float[] hypo = orderedHypothermiaThresholds();
        float hypoSev = hypo[0];
        float hypoMod = hypo[1];
        float hypoMild = hypo[2];
        float[] hyper = orderedHyperthermiaThresholds();
        float hyperMild = hyper[0];
        float hyperMod = hyper[1];
        float hyperSev = hyper[2];

        if (temp <= hypoSev) {
            refresh(player, new MobEffectInstance(MobEffects.SLOWNESS, duration, 1, false, true, true));
            refresh(player, new MobEffectInstance(MobEffects.MINING_FATIGUE, duration, 1, false, true, true));
            hurt(
                    player,
                    level,
                    level.damageSources().freeze(),
                    (float) RealismSurvivalConfig.FREEZE_DAMAGE.getAsDouble() * windowSeconds
            );
        } else if (temp < hypoMod) {
            refresh(player, new MobEffectInstance(MobEffects.SLOWNESS, duration, 0, false, true, true));
            refresh(player, new MobEffectInstance(MobEffects.MINING_FATIGUE, duration, 0, false, true, true));
        } else if (temp < hypoMild) {
            refresh(player, new MobEffectInstance(MobEffects.SLOWNESS, duration, 0, false, true, true));
        }

        if (temp >= hyperSev) {
            refresh(player, new MobEffectInstance(MobEffects.HUNGER, duration, 0, false, true, true));
            refresh(player, new MobEffectInstance(MobEffects.WEAKNESS, duration, 0, false, true, true));
            hurt(
                    player,
                    level,
                    level.damageSources().onFire(),
                    (float) RealismSurvivalConfig.HEAT_DAMAGE.getAsDouble() * windowSeconds
            );
        } else if (temp > hyperMod) {
            refresh(player, new MobEffectInstance(MobEffects.HUNGER, duration, 0, false, true, true));
            refresh(player, new MobEffectInstance(MobEffects.WEAKNESS, duration, 0, false, true, true));
        } else if (temp > hyperMild) {
            refresh(player, new MobEffectInstance(MobEffects.HUNGER, duration, 0, false, true, true));
        }
    }

    /** severe ≤ moderate ≤ mild (ascending °C). */
    static float[] orderedHypothermiaThresholds() {
        float a = (float) RealismSurvivalConfig.HYPOTHERMIA_SEVERE_THRESHOLD.getAsDouble();
        float b = (float) RealismSurvivalConfig.HYPOTHERMIA_MODERATE_THRESHOLD.getAsDouble();
        float c = (float) RealismSurvivalConfig.HYPOTHERMIA_THRESHOLD.getAsDouble();
        return sortThreeAscending(a, b, c);
    }

    /** mild ≤ moderate ≤ severe (ascending °C). */
    static float[] orderedHyperthermiaThresholds() {
        float a = (float) RealismSurvivalConfig.HYPERTHERMIA_THRESHOLD.getAsDouble();
        float b = (float) RealismSurvivalConfig.HYPERTHERMIA_MODERATE_THRESHOLD.getAsDouble();
        float c = (float) RealismSurvivalConfig.HYPERTHERMIA_SEVERE_THRESHOLD.getAsDouble();
        return sortThreeAscending(a, b, c);
    }

    private static float[] sortThreeAscending(float a, float b, float c) {
        float lo = Math.min(a, Math.min(b, c));
        float hi = Math.max(a, Math.max(b, c));
        float mid = a + b + c - lo - hi;
        return new float[] {lo, mid, hi};
    }

    private static void refresh(ServerPlayer player, MobEffectInstance effect) {
        player.addEffect(effect);
    }

    private static void hurt(
            ServerPlayer player,
            ServerLevel level,
            net.minecraft.world.damagesource.DamageSource source,
            float amount
    ) {
        if (amount > 0.0F) {
            player.hurtServer(level, source, amount);
        }
    }
}
