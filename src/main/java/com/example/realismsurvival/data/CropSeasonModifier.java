package com.example.realismsurvival.data;

import com.example.realismsurvival.environment.Season;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;

/**
 * Data-pack entry: season-dependent crop growth correction (own calendar path).
 *
 * <p>Location: {@code data/<namespace>/realismsurvival/crop_season/<name>.json}
 *
 * <pre>{@code
 * {
 *   "seasons": ["spring", "autumn"],
 *   "crops": ["minecraft:wheat", "minecraft:beetroots"],
 *   "growth_multiplier": 1.25,
 *   "priority": 10
 * }
 * }</pre>
 */
public record CropSeasonModifier(
        List<Season> seasons,
        Optional<HolderSet<Block>> crops,
        float growthMultiplier,
        int priority
) implements Prioritized {

    public static final Codec<CropSeasonModifier> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Season.CODEC.listOf().fieldOf("seasons").forGetter(CropSeasonModifier::seasons),
            RegistryCodecs.homogeneousList(Registries.BLOCK).optionalFieldOf("crops").forGetter(CropSeasonModifier::crops),
            Codec.floatRange(0.0F, 10.0F).fieldOf("growth_multiplier").forGetter(CropSeasonModifier::growthMultiplier),
            Codec.INT.optionalFieldOf("priority", 0).forGetter(CropSeasonModifier::priority)
    ).apply(instance, CropSeasonModifier::new));

    public boolean matches(Season season, Holder<Block> crop) {
        return seasons.contains(season) && crops.map(set -> set.contains(crop)).orElse(true);
    }
}
