package com.example.realismsurvival.client;

import com.example.realismsurvival.data.BiomeThirstProfile;
import com.example.realismsurvival.data.CropGrowthModifier;
import com.example.realismsurvival.data.CropLightProfile;
import com.example.realismsurvival.data.DrinkDefinition;
import com.example.realismsurvival.data.InsulationDefinition;
import com.example.realismsurvival.data.Prioritized;
import com.example.realismsurvival.network.SyncSurvivalDataPayload;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;

/**
 * Client-side mirror of server datapack tables (T-09 / spec §7.6).
 *
 * <p>Never read {@link com.example.realismsurvival.data.SurvivalDataManager} from client code —
 * on a remote connection those maps are empty. This store is filled by {@link SyncSurvivalDataPayload}.
 */
public final class ClientSurvivalData {

    private ClientSurvivalData() {}

    private static volatile Map<Identifier, BiomeThirstProfile> biomeThirst = Map.of();
    private static volatile Map<Identifier, CropGrowthModifier> cropGrowth = Map.of();
    private static volatile Map<Identifier, CropLightProfile> cropLight = Map.of();
    private static volatile Map<Identifier, DrinkDefinition> drinks = Map.of();
    private static volatile Map<Identifier, InsulationDefinition> insulation = Map.of();

    public static void apply(SyncSurvivalDataPayload payload) {
        biomeThirst = Map.copyOf(payload.biomeThirst());
        cropGrowth = Map.copyOf(payload.cropGrowth());
        cropLight = Map.copyOf(payload.cropLight());
        drinks = Map.copyOf(payload.drinks());
        insulation = Map.copyOf(payload.insulation());
    }

    public static void clear() {
        biomeThirst = Map.of();
        cropGrowth = Map.of();
        cropLight = Map.of();
        drinks = Map.of();
        insulation = Map.of();
    }

    public static Optional<BiomeThirstProfile> biomeThirst(Holder<Biome> biome) {
        return pickBest(biomeThirst, profile -> profile.matches(biome));
    }

    public static Optional<CropGrowthModifier> cropGrowth(Holder<Biome> biome, Holder<Block> crop) {
        return pickBest(cropGrowth, modifier -> modifier.matches(biome, crop));
    }

    public static Optional<CropLightProfile> cropLight(Holder<Block> crop) {
        return pickBest(cropLight, profile -> profile.matches(crop));
    }

    public static Optional<DrinkDefinition> drink(Holder<Item> item) {
        return pickBest(drinks, drink -> drink.matches(item));
    }

    public static Optional<InsulationDefinition> insulation(Holder<Item> item) {
        return pickBest(insulation, def -> def.matches(item));
    }

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
