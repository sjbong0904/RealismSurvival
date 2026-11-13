package com.example.realismsurvival.api.water;

import com.example.realismsurvival.RealismSurvival;
import com.example.realismsurvival.registry.ModFluids;
import com.example.realismsurvival.water.WaterQualityStates;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.jspecify.annotations.Nullable;

/**
 * Registry entry describing one drinkable / placeable water grade (spec §9.9).
 *
 * <p>All grades share {@link ModFluids#GRADED_WATER}; world identity is
 * {@link WaterQualityStates#QUALITY} on the fluid/block state. Addons register a
 * {@link WaterQuality} plus bucket/bottle items — not a new fluid pair.
 */
public final class WaterQuality {

    public static final ResourceKey<Registry<WaterQuality>> REGISTRY_KEY =
            ResourceKey.createRegistryKey(RealismSurvival.id("water_quality"));

    private final Supplier<? extends Item> bottleItem;
    private final Supplier<? extends Item> bucketItem;
    private final int tintRgb;
    private final boolean canHydrateFarmland;
    private final boolean contaminatesIrrigation;
    private final int resolvePriority;
    private final @Nullable TagKey<Biome> biomeTag;
    private final boolean matchOceanOrBeach;
    private final boolean matchSurfaceSlimeBiomes;
    private final List<MobEffectInstance> drinkEffects;

    private WaterQuality(Builder builder) {
        this.bottleItem = Objects.requireNonNull(builder.bottleItem, "bottleItem");
        this.bucketItem = Objects.requireNonNull(builder.bucketItem, "bucketItem");
        this.tintRgb = builder.tintRgb;
        this.canHydrateFarmland = builder.canHydrateFarmland;
        this.contaminatesIrrigation = builder.contaminatesIrrigation;
        this.resolvePriority = builder.resolvePriority;
        this.biomeTag = builder.biomeTag;
        this.matchOceanOrBeach = builder.matchOceanOrBeach;
        this.matchSurfaceSlimeBiomes = builder.matchSurfaceSlimeBiomes;
        this.drinkEffects = List.copyOf(builder.drinkEffects);
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Shared source fluid for every grade. */
    public FlowingFluid sourceFluid() {
        return ModFluids.GRADED_WATER.get();
    }

    /** Shared flowing fluid for every grade. */
    public FlowingFluid flowingFluid() {
        return ModFluids.FLOWING_GRADED_WATER.get();
    }

    public Supplier<? extends FlowingFluid> sourceFluidSupplier() {
        return ModFluids.GRADED_WATER;
    }

    public Supplier<? extends FlowingFluid> flowingFluidSupplier() {
        return ModFluids.FLOWING_GRADED_WATER;
    }

    public Item bottleItem() {
        return bottleItem.get();
    }

    public Item bucketItem() {
        return bucketItem.get();
    }

    public ItemStack createBottle() {
        return new ItemStack(bottleItem());
    }

    public ItemStack createBucket() {
        return com.example.realismsurvival.water.BucketFill.createBucket(this, com.example.realismsurvival.water.BucketFill.MAX);
    }

    public int tintRgb() {
        return tintRgb;
    }

    public boolean canHydrateFarmland() {
        return canHydrateFarmland;
    }

    public boolean contaminatesIrrigation() {
        return contaminatesIrrigation;
    }

    public int resolvePriority() {
        return resolvePriority;
    }

    public @Nullable TagKey<Biome> biomeTag() {
        return biomeTag;
    }

    public boolean matchOceanOrBeach() {
        return matchOceanOrBeach;
    }

    public boolean matchSurfaceSlimeBiomes() {
        return matchSurfaceSlimeBiomes;
    }

    public List<MobEffectInstance> drinkEffects() {
        return drinkEffects;
    }

    public void applyDrinkEffects(ServerPlayer player) {
        applyDrinkEffects(player, 1.0F);
    }

    public void applyDrinkEffects(ServerPlayer player, float multiplier) {
        float m = Math.max(0.0F, multiplier);
        for (MobEffectInstance effect : drinkEffects) {
            int duration = effect.getDuration();
            if (duration > 0) {
                duration = Math.max(1, Math.round(duration * m));
            }
            player.addEffect(new MobEffectInstance(
                    effect.getEffect(),
                    duration,
                    effect.getAmplifier(),
                    effect.isAmbient(),
                    effect.isVisible(),
                    effect.showIcon()
            ));
        }
    }

    /** Whether {@code biome} matches this grade's tag / vanilla fallbacks (no event). */
    public boolean matchesBiome(Holder<Biome> biome) {
        if (biomeTag != null && biome.is(biomeTag)) {
            return true;
        }
        if (matchOceanOrBeach
                && (biome.is(net.minecraft.tags.BiomeTags.IS_OCEAN)
                        || biome.is(net.minecraft.tags.BiomeTags.IS_DEEP_OCEAN)
                        || biome.is(net.minecraft.tags.BiomeTags.IS_BEACH))) {
            return true;
        }
        return matchSurfaceSlimeBiomes
                && biome.is(net.minecraft.tags.BiomeTags.ALLOWS_SURFACE_SLIME_SPAWNS);
    }

    /** @see WaterApi#resolveFromBiome(Holder) */
    public static WaterQuality fromBiome(Holder<Biome> biome) {
        return WaterApi.resolveFromBiome(biome);
    }

    /**
     * {@code null} unless {@code fluid} is the shared graded water (use
     * {@link #fromFluidState(FluidState)} for the grade).
     */
    public static @Nullable WaterQuality fromFluid(Fluid fluid) {
        return WaterApi.fromFluid(fluid);
    }

    public static @Nullable WaterQuality fromFluidState(FluidState state) {
        return WaterApi.fromFluidState(state);
    }

    public static @Nullable WaterQuality fromBottleItem(Item item) {
        return WaterApi.fromBottleItem(item);
    }

    public static @Nullable WaterQuality fromBucketItem(Item item) {
        return WaterApi.fromBucketItem(item);
    }

    public static final class Builder {
        private @Nullable Supplier<? extends Item> bottleItem;
        private @Nullable Supplier<? extends Item> bucketItem;
        private int tintRgb = 0x385DC6;
        private boolean canHydrateFarmland;
        private boolean contaminatesIrrigation;
        private int resolvePriority;
        private @Nullable TagKey<Biome> biomeTag;
        private boolean matchOceanOrBeach;
        private boolean matchSurfaceSlimeBiomes;
        private final List<MobEffectInstance> drinkEffects = new ArrayList<>();

        /** No-op: all grades share {@link ModFluids#GRADED_WATER}. Kept for call-site compatibility. */
        public Builder sourceFluid(Supplier<? extends FlowingFluid> sourceFluid) {
            return this;
        }

        /** No-op: all grades share {@link ModFluids#FLOWING_GRADED_WATER}. */
        public Builder flowingFluid(Supplier<? extends FlowingFluid> flowingFluid) {
            return this;
        }

        public Builder bottleItem(Supplier<? extends Item> bottleItem) {
            this.bottleItem = bottleItem;
            return this;
        }

        public Builder bucketItem(Supplier<? extends Item> bucketItem) {
            this.bucketItem = bucketItem;
            return this;
        }

        public Builder tintRgb(int tintRgb) {
            this.tintRgb = tintRgb & 0xFFFFFF;
            return this;
        }

        public Builder canHydrateFarmland(boolean canHydrateFarmland) {
            this.canHydrateFarmland = canHydrateFarmland;
            return this;
        }

        public Builder contaminatesIrrigation(boolean contaminatesIrrigation) {
            this.contaminatesIrrigation = contaminatesIrrigation;
            return this;
        }

        /** Higher wins when multiple grades match a biome. */
        public Builder resolvePriority(int resolvePriority) {
            this.resolvePriority = resolvePriority;
            return this;
        }

        public Builder biomeTag(@Nullable TagKey<Biome> biomeTag) {
            this.biomeTag = biomeTag;
            return this;
        }

        public Builder matchOceanOrBeach(boolean matchOceanOrBeach) {
            this.matchOceanOrBeach = matchOceanOrBeach;
            return this;
        }

        public Builder matchSurfaceSlimeBiomes(boolean matchSurfaceSlimeBiomes) {
            this.matchSurfaceSlimeBiomes = matchSurfaceSlimeBiomes;
            return this;
        }

        public Builder drinkEffect(MobEffectInstance effect) {
            this.drinkEffects.add(effect);
            return this;
        }

        public Builder drinkEffects(List<MobEffectInstance> effects) {
            this.drinkEffects.addAll(effects);
            return this;
        }

        public WaterQuality build() {
            return new WaterQuality(this);
        }
    }
}
