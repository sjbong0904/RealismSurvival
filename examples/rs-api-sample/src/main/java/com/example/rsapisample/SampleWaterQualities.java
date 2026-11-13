package com.example.rsapisample;

import com.example.realismsurvival.api.water.WaterQuality;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class SampleWaterQualities {

    private SampleWaterQualities() {}

    public static final TagKey<Biome> MINERAL_WATER_BIOMES = TagKey.create(
            net.minecraft.core.registries.Registries.BIOME,
            Identifier.fromNamespaceAndPath(RsApiSample.MODID, "mineral_water")
    );

    public static final DeferredRegister<WaterQuality> QUALITIES =
            DeferredRegister.create(WaterQuality.REGISTRY_KEY, RsApiSample.MODID);

    /**
     * Fifth grade (ids 0–3 are core). Milder than stagnant; biome tag empty by default
     * so worldgen stays fresh unless pack authors fill {@code #rsapisample:mineral_water}.
     */
    public static final DeferredHolder<WaterQuality, WaterQuality> MINERAL = QUALITIES.register(
            "mineral",
            () -> WaterQuality.builder()
                    .bottleItem(SampleItems.MINERAL_WATER_BOTTLE)
                    .bucketItem(SampleItems.MINERAL_WATER_BUCKET)
                    .tintRgb(0x5BA88A)
                    .canHydrateFarmland(true)
                    .contaminatesIrrigation(false)
                    .resolvePriority(5)
                    .biomeTag(MINERAL_WATER_BIOMES)
                    .drinkEffect(new MobEffectInstance(MobEffects.REGENERATION, 5 * 20, 0))
                    .build()
    );

    public static void register(IEventBus modEventBus) {
        QUALITIES.register(modEventBus);
    }
}
