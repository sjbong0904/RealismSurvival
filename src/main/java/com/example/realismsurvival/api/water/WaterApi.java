package com.example.realismsurvival.api.water;

import com.example.realismsurvival.api.ModFluidTags;
import com.example.realismsurvival.api.event.ConvertVanillaWaterEvent;
import com.example.realismsurvival.api.event.ResolveWaterQualityEvent;
import com.example.realismsurvival.registry.ModFluids;
import com.example.realismsurvival.registry.ModWaterQualities;
import com.example.realismsurvival.water.GradedIceBucketItem;
import com.example.realismsurvival.water.GradedWaterBottleItem;
import com.example.realismsurvival.water.GradedWaterBucketItem;
import com.example.realismsurvival.water.VanillaWaterConverter;
import com.example.realismsurvival.water.WaterQualityStates;
import java.util.Comparator;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.common.NeoForge;
import org.jspecify.annotations.Nullable;

/**
 * Public water helpers for addons: quality resolution, lookups, and vanilla-water conversion.
 */
public final class WaterApi {

    private WaterApi() {}

    public static Registry<WaterQuality> registry() {
        return Objects.requireNonNull(ModWaterQualities.REGISTRY, "WaterQuality registry not created yet");
    }

    /**
     * Biome → quality using registered matchers (priority, then registry id), then
     * {@link ResolveWaterQualityEvent}. Defaults to {@link ModWaterQualities#FRESH} when nothing matches.
     */
    public static WaterQuality resolveFromBiome(Holder<Biome> biome) {
        WaterQuality resolved = registry().stream()
                .filter(quality -> quality.matchesBiome(biome))
                .max(Comparator
                        .comparingInt(WaterQuality::resolvePriority)
                        .thenComparing(quality -> String.valueOf(registry().getKey(quality))))
                .orElseGet(ModWaterQualities.FRESH::get);
        ResolveWaterQualityEvent event = new ResolveWaterQualityEvent(biome, resolved);
        NeoForge.EVENT_BUS.post(event);
        return event.getQuality();
    }

    /** True when {@code fluid} is the shared graded-water still or flowing fluid. */
    public static boolean isGradedWaterFluid(Fluid fluid) {
        return fluid == ModFluids.GRADED_WATER.get() || fluid == ModFluids.FLOWING_GRADED_WATER.get();
    }

    /**
     * Non-null only for the shared graded fluid type (not a specific grade). Prefer
     * {@link #fromFluidState(FluidState)}.
     */
    public static @Nullable WaterQuality fromFluid(Fluid fluid) {
        if (isGradedWaterFluid(fluid)) {
            return ModWaterQualities.FRESH.get();
        }
        return null;
    }

    /** Grade from {@link WaterQualityStates#QUALITY}, or {@code null} if not graded water. */
    public static @Nullable WaterQuality fromFluidState(FluidState state) {
        if (state.isEmpty() || !isGradedWaterFluid(state.getType())) {
            return null;
        }
        return WaterQualityStates.byId(WaterQualityStates.qualityId(state));
    }

    /**
     * Graded quality at {@code pos}, or biome quality for vanilla {@code #minecraft:water} sources.
     */
    public static WaterQuality qualityAt(LevelReader level, BlockPos pos, FluidState state) {
        WaterQuality graded = fromFluidState(state);
        if (graded != null) {
            return graded;
        }
        if (state.is(FluidTags.WATER)) {
            return resolveFromBiome(level.getBiome(pos));
        }
        return ModWaterQualities.FRESH.get();
    }

    public static boolean isQuality(FluidState state, WaterQuality quality) {
        WaterQuality found = fromFluidState(state);
        return found != null && found == quality;
    }

    /**
     * Resolves a bottle item via registry identity, then {@link GradedWaterBottleItem} fallback.
     */
    public static @Nullable WaterQuality fromBottleItem(Item item) {
        for (WaterQuality quality : registry()) {
            if (quality.bottleItem() == item) {
                return quality;
            }
        }
        if (item instanceof GradedWaterBottleItem graded) {
            return graded.quality();
        }
        return null;
    }

    /**
     * Resolves a bucket item via registry identity, then {@link GradedWaterBucketItem} fallback.
     */
    public static @Nullable WaterQuality fromBucketItem(Item item) {
        for (WaterQuality quality : registry()) {
            if (quality.bucketItem() == item) {
                return quality;
            }
        }
        if (item instanceof GradedWaterBucketItem graded) {
            return graded.quality();
        }
        return null;
    }

    /** Ice-bucket item → quality, or {@code null}. */
    public static @Nullable WaterQuality fromIceBucketItem(Item item) {
        if (item instanceof GradedIceBucketItem graded) {
            return graded.quality();
        }
        return null;
    }

    /** Freshwater ice bucket stack for {@code quality}, or {@code null} if salt / unknown. */
    public static @Nullable ItemStack createIceBucket(WaterQuality quality) {
        Item item = iceBucketItem(quality);
        return item != null ? new ItemStack(item) : null;
    }

    public static @Nullable Item iceBucketItem(WaterQuality quality) {
        if (quality == ModWaterQualities.DISTILLED.get()) {
            return com.example.realismsurvival.registry.ModItems.DISTILLED_ICE_BUCKET.get();
        }
        if (quality == ModWaterQualities.FRESH.get()) {
            return com.example.realismsurvival.registry.ModItems.FRESH_ICE_BUCKET.get();
        }
        if (quality == ModWaterQualities.STAGNANT.get()) {
            return com.example.realismsurvival.registry.ModItems.STAGNANT_ICE_BUCKET.get();
        }
        return null;
    }

    /** True when the item is linked as a bucket on any registered {@link WaterQuality}. */
    public static boolean isGradedWaterBucket(Item item) {
        return fromBucketItem(item) != null;
    }

    public static boolean isGradedIceBucket(Item item) {
        return fromIceBucketItem(item) != null;
    }

    /** True when the item is linked as a bottle on any registered {@link WaterQuality}. */
    public static boolean isGradedWaterBottle(Item item) {
        return fromBottleItem(item) != null;
    }

    /**
     * Source fluid the player may drink from or scoop: graded water, drinkable tag, or vanilla water.
     */
    public static boolean isInteractableWaterSource(FluidState state) {
        if (!state.isSource()) {
            return false;
        }
        if (isGradedWaterFluid(state.getType())) {
            return true;
        }
        return state.is(ModFluidTags.DRINKABLE_FLUIDS) || state.is(FluidTags.WATER);
    }

    /**
     * Posts {@link ConvertVanillaWaterEvent}; if not canceled returns the (possibly overridden) quality.
     *
     * @return {@code null} when conversion should be skipped
     */
    public static @Nullable WaterQuality prepareConversion(
            BlockPos pos,
            Holder<Biome> biome,
            FluidState vanillaFluid,
            WaterQuality defaultQuality
    ) {
        ConvertVanillaWaterEvent event = new ConvertVanillaWaterEvent(pos, biome, vanillaFluid, defaultQuality);
        NeoForge.EVENT_BUS.post(event);
        if (event.isCanceled()) {
            return null;
        }
        return event.getQuality();
    }

    public static boolean tryConvert(ServerLevel level, BlockPos pos) {
        return VanillaWaterConverter.tryConvert(level, pos);
    }

    public static int convertChunkAccess(ChunkAccess chunk) {
        return VanillaWaterConverter.convertChunkAccess(chunk);
    }

    public static int convertChunk(ServerLevel level, LevelChunk chunk) {
        return VanillaWaterConverter.convertChunk(level, chunk);
    }
}
