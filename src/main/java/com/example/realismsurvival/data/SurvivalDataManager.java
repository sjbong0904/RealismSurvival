package com.example.realismsurvival.data;

import com.example.realismsurvival.api.EquipmentAccess;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import com.example.realismsurvival.environment.Season;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;

/**
 * Server-side, read-mostly store for every data-pack driven balancing table.
 *
 * <p>Threading model: the maps are replaced wholesale (never mutated) from the server thread in
 * {@link SurvivalJsonLoader#apply}. Fields are {@code volatile} so any thread observing the reference
 * sees a fully constructed immutable map. Lookups are O(n) over a handful of entries; cache in the caller
 * if a hot loop ever needs it.
 *
 * <p><b>Side rule:</b> only the logical server populates this store. On a client connected to a remote
 * server all maps are empty — client code must use {@code ClientSurvivalData} filled by
 * {@code SyncSurvivalDataPayload} (T-09 / spec §7.6).
 *
 * <p>Conflict resolution for every lookup: filter by {@code matches}, then pick the highest
 * {@link Prioritized#priority()}; ties are broken by entry id so results are deterministic.
 */
public final class SurvivalDataManager {

    private SurvivalDataManager() {}

    private static volatile Map<Identifier, BiomeThirstProfile> biomeThirst = Map.of();
    private static volatile Map<Identifier, CropGrowthModifier> cropGrowth = Map.of();
    private static volatile Map<Identifier, CropLightProfile> cropLight = Map.of();
    private static volatile Map<Identifier, CropSeasonModifier> cropSeason = Map.of();
    private static volatile Map<Identifier, DrinkDefinition> drinks = Map.of();
    private static volatile Map<Identifier, InsulationDefinition> insulation = Map.of();

    // ---- Publishers (called by loaders) -------------------------------------------------------------------------------

    static void publishBiomeThirst(Map<Identifier, BiomeThirstProfile> entries) {
        biomeThirst = entries;
    }

    static void publishCropGrowth(Map<Identifier, CropGrowthModifier> entries) {
        cropGrowth = entries;
    }

    static void publishCropLight(Map<Identifier, CropLightProfile> entries) {
        cropLight = entries;
    }

    static void publishCropSeason(Map<Identifier, CropSeasonModifier> entries) {
        cropSeason = entries;
    }

    static void publishDrinks(Map<Identifier, DrinkDefinition> entries) {
        drinks = entries;
    }

    static void publishInsulation(Map<Identifier, InsulationDefinition> entries) {
        insulation = entries;
    }

    // ---- Queries --------------------------------------------------------------------------------------------------------

    /** Best matching thirst profile for a biome, or empty if no data-pack entry covers it. */
    public static Optional<BiomeThirstProfile> biomeThirst(Holder<Biome> biome) {
        return pickBest(biomeThirst, profile -> profile.matches(biome));
    }

    /** Best matching crop growth modifier for a (biome, crop block) pair. */
    public static Optional<CropGrowthModifier> cropGrowth(Holder<Biome> biome, Holder<Block> crop) {
        return pickBest(cropGrowth, modifier -> modifier.matches(biome, crop));
    }

    /** Best matching light-preference profile for a crop block. */
    public static Optional<CropLightProfile> cropLight(Holder<Block> crop) {
        return pickBest(cropLight, profile -> profile.matches(crop));
    }

    /** Best matching season growth modifier for a (season, crop) pair. */
    public static Optional<CropSeasonModifier> cropSeason(Season season, Holder<Block> crop) {
        return pickBest(cropSeason, modifier -> modifier.matches(season, crop));
    }

    /** Best matching drink definition for an item. */
    public static Optional<DrinkDefinition> drink(Holder<Item> item) {
        return pickBest(drinks, drink -> drink.matches(item));
    }

    /** Direct lookup by datapack entry id (e.g. {@code realismsurvival:raw_water}). */
    public static Optional<DrinkDefinition> drinkById(Identifier id) {
        return Optional.ofNullable(drinks.get(id));
    }

    /** Best matching insulation entry for a single item. */
    public static Optional<InsulationDefinition> insulation(Holder<Item> item) {
        return pickBest(insulation, def -> def.matches(item));
    }

    /**
     * Total insulation {@code [0, 1]} from equipped items (armor + Curios +
     * {@link com.example.realismsurvival.api.event.CollectEquippedItemsEvent}).
     * Each matching stack contributes the best datapack entry's {@code insulation} value.
     */
    public static float wornInsulation(LivingEntity entity) {
        float total = 0.0F;
        for (ItemStack stack : EquipmentAccess.collectEquipped(entity)) {
            total += insulation(stack.typeHolder()).map(InsulationDefinition::insulation).orElse(0.0F);
        }
        return Mth.clamp(total, 0.0F, 1.0F);
    }

    // ---- Raw views (for commands / debugging) -------------------------------------------------------------------------

    public static Map<Identifier, BiomeThirstProfile> allBiomeThirst() {
        return biomeThirst;
    }

    public static Map<Identifier, CropGrowthModifier> allCropGrowth() {
        return cropGrowth;
    }

    public static Map<Identifier, CropLightProfile> allCropLight() {
        return cropLight;
    }

    public static Map<Identifier, CropSeasonModifier> allCropSeason() {
        return cropSeason;
    }

    public static Map<Identifier, DrinkDefinition> allDrinks() {
        return drinks;
    }

    public static Map<Identifier, InsulationDefinition> allInsulation() {
        return insulation;
    }

    // ---- Internals ------------------------------------------------------------------------------------------------------

    private static <T extends Prioritized> Optional<T> pickBest(Map<Identifier, T> entries, java.util.function.Predicate<T> filter) {
        Comparator<Map.Entry<Identifier, T>> order = Comparator
                .<Map.Entry<Identifier, T>>comparingInt(entry -> entry.getValue().priority()).reversed()
                .thenComparing(Map.Entry::getKey);
        return entries.entrySet().stream()
                .filter(entry -> filter.test(entry.getValue()))
                .min(order)
                .map(Map.Entry::getValue);
    }
}
