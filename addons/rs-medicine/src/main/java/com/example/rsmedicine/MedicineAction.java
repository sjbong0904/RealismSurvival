package com.example.rsmedicine;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/**
 * What a medicine item does on finish-use. Heal uses {@link com.example.realismsurvival.api.SurvivalApi#allowUnscaledHeal}.
 */
public record MedicineAction(
        float healAmount,
        boolean clearPoison,
        boolean clearWither,
        boolean clearHunger,
        boolean clearWeakness,
        boolean clearBlindness,
        boolean clearDarkness,
        boolean clearNausea,
        boolean clearLevitation,
        boolean clearSlowness,
        boolean clearFire,
        boolean clearFreeze
) {
    public static MedicineAction heal(float amount) {
        return new MedicineAction(amount, false, false, false, false, false, false, false, false, false, false, false);
    }

    public static MedicineAction antidote() {
        return new MedicineAction(0.0F, true, false, false, false, false, false, false, false, false, false, false);
    }

    public static MedicineAction burnSalve() {
        return new MedicineAction(0.0F, false, false, false, false, false, false, false, false, false, true, false);
    }

    public static MedicineAction warmPack() {
        return new MedicineAction(0.0F, false, false, false, false, false, false, false, false, true, false, true);
    }

    public static MedicineAction stimulant() {
        return new MedicineAction(0.0F, false, false, false, true, false, false, false, false, true, false, false);
    }

    public static MedicineAction eyeDrops() {
        return new MedicineAction(0.0F, false, false, false, false, true, true, true, true, false, false, false);
    }

    public static MedicineAction fieldKit() {
        return new MedicineAction(0.0F, true, true, true, true, true, true, true, true, true, true, true);
    }

    public static MedicineAction clinicKit(float heal) {
        return new MedicineAction(heal, true, true, true, true, true, true, true, true, true, true, true);
    }

    public void applyCures(LivingEntity entity) {
        if (clearPoison) {
            entity.removeEffect(MedicineEffects.TOXIN);
            entity.removeEffect(MobEffects.POISON);
        }
        if (clearWither) {
            entity.removeEffect(MedicineEffects.DECAY);
            entity.removeEffect(MobEffects.WITHER);
        }
        if (clearHunger) {
            entity.removeEffect(MedicineEffects.FATIGUE);
            entity.removeEffect(MobEffects.HUNGER);
        }
        if (clearWeakness) {
            entity.removeEffect(MedicineEffects.FRAILTY);
            entity.removeEffect(MedicineEffects.SHOCK);
            entity.removeEffect(MobEffects.WEAKNESS);
        }
        if (clearBlindness) {
            entity.removeEffect(MobEffects.BLINDNESS);
        }
        if (clearDarkness) {
            entity.removeEffect(MobEffects.DARKNESS);
        }
        if (clearNausea) {
            entity.removeEffect(MobEffects.NAUSEA);
        }
        if (clearLevitation) {
            entity.removeEffect(MobEffects.LEVITATION);
        }
        if (clearSlowness) {
            entity.removeEffect(MedicineEffects.CHILL);
            entity.removeEffect(MedicineEffects.SHOCK);
            entity.removeEffect(MobEffects.SLOWNESS);
        }
        if (clearFire) {
            entity.clearFire();
            entity.removeEffect(MedicineEffects.BURN);
        }
        if (clearFreeze) {
            entity.setTicksFrozen(0);
            entity.removeEffect(MedicineEffects.CHILL);
        }
    }

    public boolean heals() {
        return healAmount > 0.0F;
    }

    public boolean curesAnything() {
        return clearPoison || clearWither || clearHunger || clearWeakness || clearBlindness
                || clearDarkness || clearNausea || clearLevitation || clearSlowness || clearFire || clearFreeze;
    }

    /** Fire / freeze are not mob-effect icons — if either is cleared, tooltips fall back to a name list. */
    public boolean allCuresHaveIcons() {
        return curesAnything() && !clearFire && !clearFreeze;
    }

    /** Unique mob effects cleared by this action (for icon tooltips). */
    public List<Holder<MobEffect>> cureEffectIcons() {
        Set<Holder<MobEffect>> out = new LinkedHashSet<>();
        if (clearPoison) {
            out.add(MobEffects.POISON);
            out.add(MedicineEffects.TOXIN);
        }
        if (clearWither) {
            out.add(MobEffects.WITHER);
            out.add(MedicineEffects.DECAY);
        }
        if (clearHunger) {
            out.add(MobEffects.HUNGER);
            out.add(MedicineEffects.FATIGUE);
        }
        if (clearWeakness) {
            out.add(MobEffects.WEAKNESS);
            out.add(MedicineEffects.FRAILTY);
            out.add(MedicineEffects.SHOCK);
        }
        if (clearBlindness) {
            out.add(MobEffects.BLINDNESS);
        }
        if (clearDarkness) {
            out.add(MobEffects.DARKNESS);
        }
        if (clearNausea) {
            out.add(MobEffects.NAUSEA);
        }
        if (clearLevitation) {
            out.add(MobEffects.LEVITATION);
        }
        if (clearSlowness) {
            out.add(MobEffects.SLOWNESS);
            out.add(MedicineEffects.CHILL);
            out.add(MedicineEffects.SHOCK);
        }
        if (clearFire) {
            out.add(MedicineEffects.BURN);
        }
        if (clearFreeze) {
            out.add(MedicineEffects.CHILL);
        }
        return List.copyOf(out);
    }

    /** Display names for the text cure list (includes fire / frostbite when relevant). */
    public List<Component> cureDisplayNames() {
        List<Component> names = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (Holder<MobEffect> effect : cureEffectIcons()) {
            Component name = effect.value().getDisplayName();
            if (seen.add(name.getString())) {
                names.add(name);
            }
        }
        if (clearFire && seen.add("__fire__")) {
            names.add(Component.translatable("tooltip.rsmedicine.cure.fire"));
        }
        if (clearFreeze && seen.add("__freeze__")) {
            names.add(Component.translatable("tooltip.rsmedicine.cure.freeze"));
        }
        return names;
    }
}
