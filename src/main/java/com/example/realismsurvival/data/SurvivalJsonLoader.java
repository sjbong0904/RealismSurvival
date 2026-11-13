package com.example.realismsurvival.data;

import com.example.realismsurvival.RealismSurvival;
import com.mojang.serialization.Codec;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

/**
 * Generic data-pack JSON loader built on vanilla's {@link SimpleJsonResourceReloadListener}.
 *
 * <p>One instance is created per data type and per reload (see {@link SurvivalReloadListeners}).
 * The {@code HolderLookup.Provider} constructor variant is used on purpose:
 * <ul>
 *   <li>It builds a {@code RegistryOps} so codecs can resolve {@code HolderSet} fields written as
 *       {@code "minecraft:desert"} or {@code "#minecraft:is_badlands"} against the live registries.</li>
 *   <li>It derives the scan directory from a pseudo registry key via {@code FileToIdConverter.registry(key)},
 *       which for a modded namespace yields {@code <modid>/<path>} — i.e.
 *       {@code data/<any_namespace>/realismsurvival/<path>/*.json}.</li>
 *   <li>NeoForge's {@code neoforge:conditions} are honoured automatically (patched into {@code scanDirectory}).</li>
 * </ul>
 *
 * <p>Parsing happens off-thread in {@code prepare}; {@link #apply} runs on the server thread, where the
 * fully parsed, immutable map is handed to the {@link SurvivalDataManager} in a single atomic publish.
 *
 * @param <T> entry type, see {@link BiomeThirstProfile}, {@link CropGrowthModifier}, {@link DrinkDefinition}
 */
public final class SurvivalJsonLoader<T> extends SimpleJsonResourceReloadListener<T> {

    private final String typeName;
    private final Consumer<Map<Identifier, T>> publisher;

    /**
     * @param registries registry access for the current reload ({@code AddServerReloadListenersEvent#getRegistryAccess()})
     * @param codec      entry codec
     * @param typeName   folder name under {@code realismsurvival/}, e.g. {@code "biome_thirst"}
     * @param publisher  receives the immutable parsed map once per reload
     */
    public SurvivalJsonLoader(HolderLookup.Provider registries, Codec<T> codec, String typeName, Consumer<Map<Identifier, T>> publisher) {
        super(registries, codec, pseudoRegistryKey(typeName));
        this.typeName = typeName;
        this.publisher = publisher;
    }

    /** Pseudo key: only used to compute the {@code realismsurvival/<typeName>} directory; nothing is registered. */
    private static <T> ResourceKey<Registry<T>> pseudoRegistryKey(String typeName) {
        return ResourceKey.createRegistryKey(RealismSurvival.id(typeName));
    }

    @Override
    protected void apply(Map<Identifier, T> entries, ResourceManager resourceManager, ProfilerFiller profiler) {
        publisher.accept(Map.copyOf(entries));
        RealismSurvival.LOGGER.info("[RealismSurvival] Loaded {} '{}' data entries", entries.size(), typeName);
    }

    @Override
    public String getName() {
        return "RealismSurvival/" + typeName;
    }
}
