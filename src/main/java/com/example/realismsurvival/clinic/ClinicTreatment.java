package com.example.realismsurvival.clinic;

import com.example.realismsurvival.api.SurvivalApi;
import java.util.Locale;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/** Paid doctor services. Soft-compat with rs-medicine when present; otherwise vanilla effect clears. */
public enum ClinicTreatment {
    FIRST_AID(40, 8, 4.0F, false),
    TRAUMA(90, 4, 8.0F, false),
    STABILIZE(120, 3, 0.0F, true),
    FULL(280, 1, 8.0F, true);

    private final int cost;
    private final int dailyLimit;
    private final float heal;
    private final boolean clearStatuses;

    ClinicTreatment(int cost, int dailyLimit, float heal, boolean clearStatuses) {
        this.cost = cost;
        this.dailyLimit = dailyLimit;
        this.heal = heal;
        this.clearStatuses = clearStatuses;
    }

    public int cost() {
        return cost;
    }

    public int dailyLimit() {
        return dailyLimit;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public void apply(ServerPlayer player) {
        if (heal > 0.0F) {
            float amount = heal;
            SurvivalApi.allowUnscaledHeal(() -> player.heal(amount));
        }
        if (clearStatuses) {
            clearCommon(player);
            tryMedicineFieldKit(player);
        }
    }

    private static void clearCommon(LivingEntity entity) {
        remove(entity, MobEffects.POISON);
        remove(entity, MobEffects.WITHER);
        remove(entity, MobEffects.HUNGER);
        remove(entity, MobEffects.WEAKNESS);
        remove(entity, MobEffects.SLOWNESS);
        remove(entity, MobEffects.BLINDNESS);
        remove(entity, MobEffects.DARKNESS);
        remove(entity, MobEffects.NAUSEA);
        remove(entity, MobEffects.LEVITATION);
        entity.clearFire();
        entity.setTicksFrozen(0);
    }

    private static void remove(LivingEntity entity, Holder<MobEffect> effect) {
        entity.removeEffect(effect);
    }

    /** Best-effort: apply rs-medicine field-kit cures when the addon is loaded. */
    private static void tryMedicineFieldKit(LivingEntity entity) {
        try {
            Class<?> actionClass = Class.forName("com.example.rsmedicine.MedicineAction");
            Object action = actionClass.getMethod("fieldKit").invoke(null);
            actionClass.getMethod("applyCures", LivingEntity.class).invoke(action, entity);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Addon absent or API mismatch — vanilla clears already applied.
        }
    }
}
