package com.example.realismsurvival.event;

import com.example.realismsurvival.RealismSurvival;
import com.example.realismsurvival.RealismSurvivalConfig;
import com.example.realismsurvival.data.CropGrowthModifier;
import com.example.realismsurvival.data.CropLightProfile;
import com.example.realismsurvival.data.CropSeasonModifier;
import com.example.realismsurvival.data.SurvivalDataManager;
import com.example.realismsurvival.environment.Season;
import com.example.realismsurvival.environment.SeasonCalendar;
import com.example.realismsurvival.environment.SeasonClimate;
import com.example.realismsurvival.water.WaterHydration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.block.CropGrowEvent;

/**
 * Applies data-pack crop rules on {@link CropGrowEvent.Pre}:
 * <ol>
 *   <li>Biome {@link CropGrowthModifier}</li>
 *   <li>Light {@link CropLightProfile}</li>
 *   <li>Season {@link CropSeasonModifier} (skipped only when this level's season came from Serene Seasons)</li>
 *   <li>Optional summer rain moisture bonus from {@link SeasonClimate}</li>
 *   <li>Coastal salt+fresh mix ({@link WaterHydration#cropGrowthMultiplier})</li>
 * </ol>
 *
 * <p>Factors multiply, then the product is clamped to {@code cropGrowthMultiplierMax}.
 */
@EventBusSubscriber(modid = RealismSurvival.MODID)
public final class CropGrowthHandler {

    private CropGrowthHandler() {}

    @SubscribeEvent
    static void onCropGrowPre(CropGrowEvent.Pre event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        BlockPos pos = event.getPos();
        Holder<Block> crop = event.getState().getBlock().builtInRegistryHolder();
        Holder<Biome> biome = level.getBiome(pos);

        float biomeMult = SurvivalDataManager.cropGrowth(biome, crop)
                .map(CropGrowthModifier::growthMultiplier)
                .orElse(1.0F);
        float lightMult = SurvivalDataManager.cropLight(crop)
                .map(profile -> profile.multiplierAt(level, pos))
                .orElse(1.0F);

        float seasonMult = 1.0F;
        float moisture = 1.0F;
        // SS owns crop seasonality only when its lookup succeeded for this level — avoid double penalties.
        SeasonCalendar.CalendarDate date = SeasonCalendar.from(level);
        if (!date.fromSereneSeasons()) {
            Season season = date.season();
            seasonMult = SurvivalDataManager.cropSeason(season, crop)
                    .map(CropSeasonModifier::growthMultiplier)
                    .orElse(1.0F);
            SeasonClimate.Profile profile = SeasonClimate.resolve(biome, season);
            if (profile.cropMoistureBonus() > 0.0F && level.isRainingAt(pos)) {
                moisture = 1.0F + profile.cropMoistureBonus();
            }
        }

        float saltCoastMult = WaterHydration.cropGrowthMultiplier(level, pos);
        float combined = biomeMult * lightMult * seasonMult * moisture * saltCoastMult;
        float max = (float) RealismSurvivalConfig.CROP_GROWTH_MULTIPLIER_MAX.getAsDouble();
        applyMultiplier(event, level, Math.min(combined, max));
    }

    /**
     * Maps a combined growth multiplier to the NeoForge crop-grow result.
     *
     * <ul>
     *   <li>{@code ≤ 0} → never grow</li>
     *   <li>{@code (0, 1)} → randomly deny (e.g. 0.5 ≈ half as often)</li>
     *   <li>{@code 1} → vanilla checks</li>
     *   <li>{@code > 1} → chance to force grow ({@code mult - 1}, e.g. 1.35 → 35%)</li>
     * </ul>
     */
    static void applyMultiplier(CropGrowEvent.Pre event, ServerLevel level, float multiplier) {
        if (multiplier <= 0.0F) {
            event.setResult(CropGrowEvent.Pre.Result.DO_NOT_GROW);
            return;
        }
        if (multiplier < 1.0F) {
            if (level.getRandom().nextFloat() > multiplier) {
                event.setResult(CropGrowEvent.Pre.Result.DO_NOT_GROW);
            }
            return;
        }
        if (multiplier > 1.0F && level.getRandom().nextFloat() < (multiplier - 1.0F)) {
            event.setResult(CropGrowEvent.Pre.Result.GROW);
        }
    }
}
