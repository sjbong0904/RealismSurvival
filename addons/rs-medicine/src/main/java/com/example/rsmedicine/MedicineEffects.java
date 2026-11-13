package com.example.rsmedicine;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Addon afflictions with custom {@code textures/mob_effect} icons.
 * Blindness / levitation stay on vanilla holders (engine-tied); cures still clear both.
 */
public final class MedicineEffects {

    private MedicineEffects() {}

    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, RsMedicine.MODID);

    public static final DeferredHolder<MobEffect, MobEffect> TOXIN = MOB_EFFECTS.register("toxin", ToxinEffect::new);
    public static final DeferredHolder<MobEffect, MobEffect> DECAY = MOB_EFFECTS.register("decay", DecayEffect::new);
    public static final DeferredHolder<MobEffect, MobEffect> FATIGUE = MOB_EFFECTS.register("fatigue", FatigueEffect::new);
    public static final DeferredHolder<MobEffect, MobEffect> FRAILTY = MOB_EFFECTS.register("frailty", FrailtyEffect::new);
    public static final DeferredHolder<MobEffect, MobEffect> CHILL = MOB_EFFECTS.register("chill", ChillEffect::new);
    public static final DeferredHolder<MobEffect, MobEffect> SHOCK = MOB_EFFECTS.register("shock", ShockEffect::new);
    public static final DeferredHolder<MobEffect, MobEffect> BURN = MOB_EFFECTS.register("burn", BurnEffect::new);

    public static void register(IEventBus modEventBus) {
        MOB_EFFECTS.register(modEventBus);
    }

    private static final class ToxinEffect extends MobEffect {
        ToxinEffect() {
            super(MobEffectCategory.HARMFUL, 0x4E9331);
        }

        @Override
        public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
            if (entity.getHealth() > 1.0F) {
                entity.hurtServer(level, entity.damageSources().magic(), 1.0F);
            }
            return true;
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
            int interval = 25 >> amplifier;
            return interval <= 0 || duration % interval == 0;
        }
    }

    private static final class DecayEffect extends MobEffect {
        DecayEffect() {
            super(MobEffectCategory.HARMFUL, 0x352A27);
        }

        @Override
        public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
            entity.hurtServer(level, entity.damageSources().wither(), 1.0F);
            return true;
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
            int interval = 40 >> amplifier;
            return interval <= 0 || duration % interval == 0;
        }
    }

    private static final class FatigueEffect extends MobEffect {
        FatigueEffect() {
            super(MobEffectCategory.HARMFUL, 0x587653);
        }

        @Override
        public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
            if (entity instanceof Player player) {
                player.causeFoodExhaustion(0.005F * (amplifier + 1));
            }
            return true;
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
            return true;
        }
    }

    private static final class FrailtyEffect extends MobEffect {
        FrailtyEffect() {
            super(MobEffectCategory.HARMFUL, 0x484D48);
            addAttributeModifier(
                    Attributes.ATTACK_DAMAGE,
                    Identifier.fromNamespaceAndPath(RsMedicine.MODID, "effect.frailty"),
                    -4.0,
                    AttributeModifier.Operation.ADD_VALUE
            );
        }
    }

    private static final class ChillEffect extends MobEffect {
        ChillEffect() {
            super(MobEffectCategory.HARMFUL, 0x8BA4C8);
            addAttributeModifier(
                    Attributes.MOVEMENT_SPEED,
                    Identifier.fromNamespaceAndPath(RsMedicine.MODID, "effect.chill"),
                    -0.15,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
            );
        }
    }

    private static final class ShockEffect extends MobEffect {
        ShockEffect() {
            super(MobEffectCategory.HARMFUL, 0xFFE566);
            addAttributeModifier(
                    Attributes.MOVEMENT_SPEED,
                    Identifier.fromNamespaceAndPath(RsMedicine.MODID, "effect.shock_slow"),
                    -0.15,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
            );
            addAttributeModifier(
                    Attributes.ATTACK_DAMAGE,
                    Identifier.fromNamespaceAndPath(RsMedicine.MODID, "effect.shock_weak"),
                    -4.0,
                    AttributeModifier.Operation.ADD_VALUE
            );
        }
    }

    private static final class BurnEffect extends MobEffect {
        BurnEffect() {
            super(MobEffectCategory.HARMFUL, 0xE25822);
        }
    }
}
