package com.example.realismsurvival.event;

import com.example.realismsurvival.RealismSurvival;
import com.example.realismsurvival.RealismSurvivalConfig;
import com.example.realismsurvival.component.SurvivalAttachments;
import com.example.realismsurvival.component.SurvivalData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;

/**
 * Slows <em>natural</em> (food/saturation) health regeneration without touching penalty damage
 * or item/potion heals (spec §9.7).
 *
 * <p>Identification rules (no mixin into {@code FoodData}):
 * <ul>
 *   <li>{@link MobEffects#REGENERATION} → leave alone (potions, golden apples, future regen items).</li>
 *   <li>Heal amount ≥ 4 → leave alone ({@code Instant Health} and most dedicated heal items).</li>
 *   <li>{@link #allowNextHeal()} thread-local → leave alone (call from future custom heal items).</li>
 *   <li>Everything else on a survival player → treat as food regen: gate on thirst/temp, then multiply.</li>
 * </ul>
 */
@EventBusSubscriber(modid = RealismSurvival.MODID)
public final class NaturalRegenHandler {

    private NaturalRegenHandler() {}

    /**
     * Instant Health I heals {@code 4 << amplifier}. Anything at or above this is treated as
     * intentional item/potion healing, not food tick regen (which is ≤ 1.0).
     */
    private static final float ITEM_HEAL_AMOUNT_FLOOR = 4.0F;

    private static final ThreadLocal<Boolean> ALLOW_NEXT_HEAL = ThreadLocal.withInitial(() -> false);

    /**
     * Wrap a custom heal so this handler does not scale it. For future recovery items:
     * {@code NaturalRegenHandler.allowNextHeal(() -> player.heal(amount));}
     */
    public static void allowNextHeal(Runnable healAction) {
        ALLOW_NEXT_HEAL.set(true);
        try {
            healAction.run();
        } finally {
            ALLOW_NEXT_HEAL.set(false);
        }
    }

    @SubscribeEvent
    static void onLivingHeal(LivingHealEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!RealismSurvivalConfig.SLOW_NATURAL_REGEN.getAsBoolean()) {
            return;
        }
        if (RealismSurvivalConfig.CREATIVE_BYPASS.getAsBoolean() && (player.isCreative() || player.isSpectator())) {
            return;
        }
        if (ALLOW_NEXT_HEAL.get()) {
            return;
        }
        if (player.hasEffect(MobEffects.REGENERATION)) {
            return;
        }
        if (event.getAmount() >= ITEM_HEAL_AMOUNT_FLOOR) {
            return;
        }

        SurvivalData data = SurvivalAttachments.get(player);
        if (!canNaturallyRegenerate(data)) {
            event.setCanceled(true);
            return;
        }

        float multiplier = (float) RealismSurvivalConfig.NATURAL_REGEN_MULTIPLIER.getAsDouble();
        if (multiplier <= 0.0F) {
            event.setCanceled(true);
            return;
        }
        if (multiplier < 1.0F) {
            event.setAmount(event.getAmount() * multiplier);
        }
    }

    /**
     * Natural regen only while hydrated enough and not in a temperature penalty band.
     * Disabled survival systems are skipped so leftover attachment values cannot block regen.
     * {@code slowNaturalRegen} itself remains independent of those toggles.
     * Hunger gating stays in vanilla {@code FoodData}.
     */
    private static boolean canNaturallyRegenerate(SurvivalData data) {
        if (RealismSurvivalConfig.THIRST_ENABLED.getAsBoolean()
                && data.thirst() < RealismSurvivalConfig.NATURAL_REGEN_MIN_THIRST.getAsDouble()) {
            return false;
        }
        if (RealismSurvivalConfig.TEMPERATURE_ENABLED.getAsBoolean()) {
            float temp = data.temperature();
            if (temp < RealismSurvivalConfig.HYPOTHERMIA_THRESHOLD.getAsDouble()) {
                return false;
            }
            if (temp > RealismSurvivalConfig.HYPERTHERMIA_THRESHOLD.getAsDouble()) {
                return false;
            }
        }
        return true;
    }
}
