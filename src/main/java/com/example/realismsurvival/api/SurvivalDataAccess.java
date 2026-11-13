package com.example.realismsurvival.api;

import com.example.realismsurvival.data.BiomeThirstProfile;
import com.example.realismsurvival.data.DrinkDefinition;
import com.example.realismsurvival.data.InsulationDefinition;
import com.example.realismsurvival.data.SurvivalDataManager;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;

/**
 * Thin re-export of datapack-driven survival tables for addons.
 * Server-populated; clients should use synced client copies where applicable.
 */
public final class SurvivalDataAccess {

    private SurvivalDataAccess() {}

    public static Optional<BiomeThirstProfile> biomeThirst(Holder<Biome> biome) {
        return SurvivalDataManager.biomeThirst(biome);
    }

    public static Optional<DrinkDefinition> drink(Holder<Item> item) {
        return SurvivalDataManager.drink(item);
    }

    public static Optional<DrinkDefinition> drinkById(Identifier id) {
        return SurvivalDataManager.drinkById(id);
    }

    public static Optional<InsulationDefinition> insulation(Holder<Item> item) {
        return SurvivalDataManager.insulation(item);
    }

    public static float wornInsulation(LivingEntity entity) {
        return SurvivalDataManager.wornInsulation(entity);
    }
}
