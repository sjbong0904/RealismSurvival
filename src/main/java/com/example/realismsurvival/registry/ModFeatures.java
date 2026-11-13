package com.example.realismsurvival.registry;

import com.example.realismsurvival.RealismSurvival;
import com.example.realismsurvival.worldgen.SupplyPostClimatePlacement;
import com.example.realismsurvival.worldgen.SupplyPostFeature;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModFeatures {

    private ModFeatures() {}

    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(Registries.FEATURE, RealismSurvival.MODID);

    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> SUPPLY_POST =
            FEATURES.register("supply_post", () -> new SupplyPostFeature(NoneFeatureConfiguration.CODEC));

    public static final DeferredRegister<PlacementModifierType<?>> PLACEMENTS =
            DeferredRegister.create(Registries.PLACEMENT_MODIFIER_TYPE, RealismSurvival.MODID);

    public static final DeferredHolder<PlacementModifierType<?>, PlacementModifierType<SupplyPostClimatePlacement>>
            SUPPLY_POST_CLIMATE = PLACEMENTS.register(
                    "supply_post_climate",
                    () -> () -> SupplyPostClimatePlacement.CODEC
            );

    public static void register(IEventBus modEventBus) {
        FEATURES.register(modEventBus);
        PLACEMENTS.register(modEventBus);
    }
}
