package com.example.realismsurvival.registry;

import com.example.realismsurvival.RealismSurvival;
import com.example.realismsurvival.api.ModBiomeTags;
import com.example.realismsurvival.api.water.WaterQuality;
import com.example.realismsurvival.water.WaterColors;
import net.minecraft.core.Registry;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Built-in {@link WaterQuality} registry entries. Registration order fixes QUALITY ids:
 * distilled=0, fresh=1, stagnant=2, salt=3.
 */
public final class ModWaterQualities {

    private ModWaterQualities() {}

    public static final DeferredRegister<WaterQuality> QUALITIES =
            DeferredRegister.create(WaterQuality.REGISTRY_KEY, RealismSurvival.MODID);

    /** Synced custom registry; created when {@link #register} runs {@code makeRegistry}. */
    public static final Registry<WaterQuality> REGISTRY = QUALITIES.makeRegistry(builder -> builder.sync(true)
            .defaultKey(RealismSurvival.id("fresh")));

    public static final DeferredHolder<WaterQuality, WaterQuality> DISTILLED = QUALITIES.register(
            "distilled",
            () -> WaterQuality.builder()
                    .bottleItem(ModItems.DISTILLED_WATER_BOTTLE)
                    .bucketItem(ModItems.DISTILLED_WATER_BUCKET)
                    .tintRgb(WaterColors.DISTILLED)
                    .canHydrateFarmland(true)
                    .contaminatesIrrigation(false)
                    .resolvePriority(-100)
                    .build()
    );

    public static final DeferredHolder<WaterQuality, WaterQuality> FRESH = QUALITIES.register(
            "fresh",
            () -> WaterQuality.builder()
                    .bottleItem(ModItems.FRESH_WATER_BOTTLE)
                    .bucketItem(ModItems.FRESH_WATER_BUCKET)
                    .tintRgb(WaterColors.FRESH)
                    .canHydrateFarmland(true)
                    .contaminatesIrrigation(false)
                    .resolvePriority(0)
                    // Mild: river water hydrates poorly vs distilled; slight nausea + peckish.
                    .drinkEffect(new MobEffectInstance(MobEffects.NAUSEA, 5 * 20, 0))
                    .drinkEffect(new MobEffectInstance(MobEffects.HUNGER, 8 * 20, 0))
                    .build()
    );

    public static final DeferredHolder<WaterQuality, WaterQuality> STAGNANT = QUALITIES.register(
            "stagnant",
            () -> WaterQuality.builder()
                    .bottleItem(ModItems.STAGNANT_WATER_BOTTLE)
                    .bucketItem(ModItems.STAGNANT_WATER_BUCKET)
                    .tintRgb(WaterColors.STAGNANT)
                    .canHydrateFarmland(true)
                    .contaminatesIrrigation(false)
                    .resolvePriority(10)
                    .biomeTag(ModBiomeTags.STAGNANT_WATER)
                    .matchSurfaceSlimeBiomes(true)
                    // Same thirst as fresh, but sickness makes distilled the real goal.
                    .drinkEffect(new MobEffectInstance(MobEffects.NAUSEA, 15 * 20, 0))
                    .drinkEffect(new MobEffectInstance(MobEffects.POISON, 6 * 20, 0))
                    .drinkEffect(new MobEffectInstance(MobEffects.HUNGER, 14 * 20, 0))
                    .build()
    );

    public static final DeferredHolder<WaterQuality, WaterQuality> SALT = QUALITIES.register(
            "salt",
            () -> WaterQuality.builder()
                    .bottleItem(ModItems.SALT_WATER_BOTTLE)
                    .bucketItem(ModItems.SALT_WATER_BUCKET)
                    .tintRgb(WaterColors.SALT)
                    .canHydrateFarmland(false)
                    .contaminatesIrrigation(true)
                    .resolvePriority(20)
                    .biomeTag(ModBiomeTags.SALT_WATER)
                    .matchOceanOrBeach(true)
                    .drinkEffect(new MobEffectInstance(MobEffects.SLOWNESS, 12 * 20, 0))
                    .drinkEffect(new MobEffectInstance(MobEffects.HUNGER, 15 * 20, 0))
                    .drinkEffect(new MobEffectInstance(MobEffects.NAUSEA, 8 * 20, 0))
                    .build()
    );

    public static void register(IEventBus modEventBus) {
        QUALITIES.register(modEventBus);
    }
}
