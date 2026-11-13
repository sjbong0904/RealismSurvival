package com.example.rsmedicine;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.golem.SnowGolem;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Endermite;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.monster.skeleton.Stray;
import net.minecraft.world.entity.monster.skeleton.WitherSkeleton;
import net.minecraft.world.entity.monster.spider.CaveSpider;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.entity.monster.zombie.Drowned;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/**
 * On player hit: short afflictions with HP-inverse chance ≤ pMax (≤ 10%).
 * Uses {@link MedicineEffects} icons where possible; blindness/levitation stay vanilla (engine-tied).
 */
final class AfflictionHandler {

    private AfflictionHandler() {}

    @SubscribeEvent
    static void onDamagePost(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (event.getHealthDamage() <= 0.0F && event.getInflictedDamage() <= 0.0F) {
            return;
        }
        if (player.isCreative() || player.isSpectator()) {
            return;
        }

        DamageSource source = event.getSource();
        Entity direct = source.getDirectEntity();
        Entity attacker = source.getEntity();
        RandomSource random = player.getRandom();

        if (tryLightning(player, source, random)) {
            return;
        }

        LivingEntity livingAttacker = resolveLiving(attacker, direct);
        if (livingAttacker != null) {
            tryFromAttacker(player, livingAttacker, random);
        }
    }

    private static LivingEntity resolveLiving(Entity attacker, Entity direct) {
        if (attacker instanceof LivingEntity living) {
            return living;
        }
        if (direct instanceof LivingEntity living) {
            return living;
        }
        return null;
    }

    private static boolean tryLightning(ServerPlayer player, DamageSource source, RandomSource random) {
        if (!source.is(DamageTypes.LIGHTNING_BOLT)) {
            return false;
        }
        return rollAndApply(player, 0.10F, MedicineEffects.SHOCK, random);
    }

    private static void tryFromAttacker(ServerPlayer player, LivingEntity attacker, RandomSource random) {
        if (attacker instanceof CaveSpider || attacker instanceof Spider
                || attacker instanceof Silverfish || attacker instanceof Endermite) {
            rollAndApply(player, 0.08F, MedicineEffects.TOXIN, random);
            return;
        }
        if (attacker instanceof WitherBoss || attacker instanceof WitherSkeleton) {
            rollAndApply(player, 0.04F, MedicineEffects.DECAY, random);
            return;
        }
        if (attacker instanceof Husk || attacker instanceof Drowned || attacker instanceof Zombie) {
            Holder<MobEffect> effect = random.nextBoolean() ? MedicineEffects.FATIGUE : MedicineEffects.FRAILTY;
            rollAndApply(player, 0.07F, effect, random);
            return;
        }
        if (attacker instanceof Phantom) {
            rollAndApply(player, 0.06F, MobEffects.BLINDNESS, random);
            return;
        }
        if (attacker instanceof Shulker) {
            rollAndApply(player, 0.05F, MobEffects.LEVITATION, random);
            return;
        }
        if (attacker instanceof Blaze || attacker instanceof MagmaCube) {
            rollFire(player, 0.08F, random);
            return;
        }
        if (attacker instanceof Stray || attacker instanceof SnowGolem) {
            if (random.nextBoolean()) {
                rollFreeze(player, 0.07F, random);
            } else {
                rollAndApply(player, 0.07F, MedicineEffects.CHILL, random);
            }
            return;
        }
        if (attacker instanceof Guardian) {
            rollAndApply(player, 0.08F, MedicineEffects.SHOCK, random);
        }
    }

    private static float chance(ServerPlayer player, float pMax) {
        float max = player.getMaxHealth();
        if (max <= 0.0F) {
            return 0.0F;
        }
        float ratio = Math.clamp(player.getHealth() / max, 0.0F, 1.0F);
        return Math.min(0.10F, pMax) * (1.0F - ratio);
    }

    private static boolean rollAndApply(
            ServerPlayer player, float pMax, Holder<MobEffect> effect, RandomSource random
    ) {
        if (player.hasEffect(effect)) {
            return false;
        }
        if (random.nextFloat() >= chance(player, pMax)) {
            return false;
        }
        int ticks = MedicineTiming.randomDurationTicks(random);
        player.addEffect(new MobEffectInstance(effect, ticks, 0));
        return true;
    }

    private static void rollFire(ServerPlayer player, float pMax, RandomSource random) {
        if (player.getRemainingFireTicks() > 0 || player.hasEffect(MedicineEffects.BURN)) {
            return;
        }
        if (random.nextFloat() >= chance(player, pMax)) {
            return;
        }
        int ticks = MedicineTiming.randomDurationTicks(random);
        player.igniteForTicks(ticks);
        player.addEffect(new MobEffectInstance(MedicineEffects.BURN, ticks, 0));
    }

    private static void rollFreeze(ServerPlayer player, float pMax, RandomSource random) {
        if (player.getTicksFrozen() > 0 || player.hasEffect(MedicineEffects.CHILL)) {
            return;
        }
        if (random.nextFloat() >= chance(player, pMax)) {
            return;
        }
        int ticks = MedicineTiming.randomDurationTicks(random);
        player.setTicksFrozen(ticks);
        player.addEffect(new MobEffectInstance(MedicineEffects.CHILL, ticks, 0));
    }
}
