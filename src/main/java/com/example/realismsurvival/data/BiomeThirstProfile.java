package com.example.realismsurvival.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.biome.Biome;

/**
 * Data-pack entry: how fast thirst drains inside a set of biomes.
 *
 * <p>Location: {@code data/<namespace>/realismsurvival/biome_thirst/<name>.json}
 *
 * <pre>{@code
 * {
 *   "biomes": "#minecraft:is_nether",   // HolderSet, see grammar below
 *   "thirst_per_second": 0.25,          // points drained per real second while idle
 *   "sprint_multiplier": 2.0,           // applied while sprinting (default 1.5)
 *   "priority": 10                      // higher wins when several profiles match (default 0)
 * }
 * }</pre>
 *
 * <p><b>HolderSet grammar</b> (vanilla + NeoForge, applies to every {@code HolderSet} field in this mod):
 * <ul>
 *   <li>{@code "#namespace:tag"} — a single tag reference (string).</li>
 *   <li>{@code ["ns:id", "ns:id"]} — a list of direct ids. <b>Tags are NOT allowed inside this list.</b></li>
 *   <li>{@code {"type": "neoforge:or", "values": [ ["ns:id"], "#ns:tag", ... ]}} — union of any of the above
 *       (also {@code neoforge:and}, {@code neoforge:not}, {@code neoforge:any}).</li>
 * </ul>
 *
 * @param biomes           biomes this profile applies to
 * @param thirstPerSecond  base drain, in thirst points per second (20 ticks)
 * @param sprintMultiplier multiplier applied while the player is sprinting
 * @param priority         conflict resolution: the matching profile with the highest priority wins
 */
public record BiomeThirstProfile(HolderSet<Biome> biomes, float thirstPerSecond, float sprintMultiplier, int priority)
        implements Prioritized {

    public static final float DEFAULT_SPRINT_MULTIPLIER = 1.5F;

    public static final Codec<BiomeThirstProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            RegistryCodecs.homogeneousList(Registries.BIOME).fieldOf("biomes").forGetter(BiomeThirstProfile::biomes),
            Codec.floatRange(0.0F, 100.0F).fieldOf("thirst_per_second").forGetter(BiomeThirstProfile::thirstPerSecond),
            Codec.floatRange(0.0F, 10.0F).optionalFieldOf("sprint_multiplier", DEFAULT_SPRINT_MULTIPLIER).forGetter(BiomeThirstProfile::sprintMultiplier),
            Codec.INT.optionalFieldOf("priority", 0).forGetter(BiomeThirstProfile::priority)
    ).apply(instance, BiomeThirstProfile::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, BiomeThirstProfile> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.holderSet(Registries.BIOME), BiomeThirstProfile::biomes,
            ByteBufCodecs.FLOAT, BiomeThirstProfile::thirstPerSecond,
            ByteBufCodecs.FLOAT, BiomeThirstProfile::sprintMultiplier,
            ByteBufCodecs.VAR_INT, BiomeThirstProfile::priority,
            BiomeThirstProfile::new
    );

    public boolean matches(Holder<Biome> biome) {
        return biomes.contains(biome);
    }

    /** Drain for one evaluation window of {@code seconds} length. */
    public float drainFor(float seconds, boolean sprinting) {
        return thirstPerSecond * seconds * (sprinting ? sprintMultiplier : 1.0F);
    }
}
