package com.example.realismsurvival.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;

/**
 * Data-pack entry: biome-dependent crop growth correction.
 *
 * <p>Location: {@code data/<namespace>/realismsurvival/crop_growth/<name>.json}
 *
 * <pre>{@code
 * {
 *   "biomes": "#minecraft:is_jungle",                   // where this modifier applies (HolderSet grammar: see BiomeThirstProfile)
 *   "crops": ["minecraft:wheat", "minecraft:carrots"],  // optional; omitted = every crop block
 *   "growth_multiplier": 1.35,                          // 1.0 = vanilla, 0.0 = never grows, 2.0 = twice as often
 *   "priority": 0
 * }
 * }</pre>
 *
 * <p>Consumption (spec §9.5): {@link com.example.realismsurvival.event.CropGrowthHandler} multiplies this
 * value with any matching {@link CropLightProfile}, then converts the product into GROW / DO_NOT_GROW.
 *
 * @param biomes           biomes where the modifier applies
 * @param crops            optional crop filter; empty means "all crops"
 * @param growthMultiplier random-tick growth probability multiplier
 * @param priority         conflict resolution, highest wins
 */
public record CropGrowthModifier(HolderSet<Biome> biomes, Optional<HolderSet<Block>> crops, float growthMultiplier, int priority)
        implements Prioritized {

    public static final Codec<CropGrowthModifier> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            RegistryCodecs.homogeneousList(Registries.BIOME).fieldOf("biomes").forGetter(CropGrowthModifier::biomes),
            RegistryCodecs.homogeneousList(Registries.BLOCK).optionalFieldOf("crops").forGetter(CropGrowthModifier::crops),
            Codec.floatRange(0.0F, 10.0F).fieldOf("growth_multiplier").forGetter(CropGrowthModifier::growthMultiplier),
            Codec.INT.optionalFieldOf("priority", 0).forGetter(CropGrowthModifier::priority)
    ).apply(instance, CropGrowthModifier::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CropGrowthModifier> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.holderSet(Registries.BIOME), CropGrowthModifier::biomes,
            ByteBufCodecs.optional(ByteBufCodecs.holderSet(Registries.BLOCK)), CropGrowthModifier::crops,
            ByteBufCodecs.FLOAT, CropGrowthModifier::growthMultiplier,
            ByteBufCodecs.VAR_INT, CropGrowthModifier::priority,
            CropGrowthModifier::new
    );

    public boolean matches(Holder<Biome> biome, Holder<Block> crop) {
        return biomes.contains(biome) && crops.map(set -> set.contains(crop)).orElse(true);
    }
}
