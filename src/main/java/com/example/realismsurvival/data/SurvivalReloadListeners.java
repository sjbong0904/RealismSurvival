package com.example.realismsurvival.data;

import com.example.realismsurvival.RealismSurvival;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;

/**
 * Registers one {@link SurvivalJsonLoader} per data type on every server resource reload
 * (world load and {@code /reload}).
 *
 * <p>{@link AddServerReloadListenersEvent} is a <b>game-bus</b> event fired on the logical server only,
 * so this class is inherently server-side and safe to keep in the common source set.
 * {@code @EventBusSubscriber} picks the correct bus automatically from the event type.
 */
@EventBusSubscriber(modid = RealismSurvival.MODID)
public final class SurvivalReloadListeners {

    private SurvivalReloadListeners() {}

    public static final String BIOME_THIRST = "biome_thirst";
    public static final String CROP_GROWTH = "crop_growth";
    public static final String CROP_LIGHT = "crop_light";
    public static final String CROP_SEASON = "crop_season";
    public static final String DRINK = "drink";
    public static final String INSULATION = "insulation";

    @SubscribeEvent
    static void onAddReloadListeners(AddServerReloadListenersEvent event) {
        var registries = event.getRegistryAccess();

        event.addListener(RealismSurvival.id(BIOME_THIRST),
                new SurvivalJsonLoader<>(registries, BiomeThirstProfile.CODEC, BIOME_THIRST, SurvivalDataManager::publishBiomeThirst));
        event.addListener(RealismSurvival.id(CROP_GROWTH),
                new SurvivalJsonLoader<>(registries, CropGrowthModifier.CODEC, CROP_GROWTH, SurvivalDataManager::publishCropGrowth));
        event.addListener(RealismSurvival.id(CROP_LIGHT),
                new SurvivalJsonLoader<>(registries, CropLightProfile.CODEC, CROP_LIGHT, SurvivalDataManager::publishCropLight));
        event.addListener(RealismSurvival.id(CROP_SEASON),
                new SurvivalJsonLoader<>(registries, CropSeasonModifier.CODEC, CROP_SEASON, SurvivalDataManager::publishCropSeason));
        event.addListener(RealismSurvival.id(DRINK),
                new SurvivalJsonLoader<>(registries, DrinkDefinition.CODEC, DRINK, SurvivalDataManager::publishDrinks));
        event.addListener(RealismSurvival.id(INSULATION),
                new SurvivalJsonLoader<>(registries, InsulationDefinition.CODEC, INSULATION, SurvivalDataManager::publishInsulation));
    }
}
