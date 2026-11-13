package com.example.realismsurvival.api;

import com.example.realismsurvival.RealismSurvival;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;

/** Public fluid tags for drink / world-sip integration (spec §15.3). */
public final class ModFluidTags {

    private ModFluidTags() {}

    /** Any fluid the core may treat as drinkable water-like (grades + vanilla water). */
    public static final TagKey<Fluid> DRINKABLE_FLUIDS = TagKey.create(Registries.FLUID, RealismSurvival.id("drinkable_fluids"));

    public static final TagKey<Fluid> DISTILLED_WATER = TagKey.create(Registries.FLUID, RealismSurvival.id("distilled_water"));
    public static final TagKey<Fluid> FRESH_WATER = TagKey.create(Registries.FLUID, RealismSurvival.id("fresh_water"));
    public static final TagKey<Fluid> STAGNANT_WATER = TagKey.create(Registries.FLUID, RealismSurvival.id("stagnant_water"));
    public static final TagKey<Fluid> SALT_WATER = TagKey.create(Registries.FLUID, RealismSurvival.id("salt_water"));

    /**
     * Fluids that count as a submerging atmosphere for {@code BreathCalculateEvent}
     * (beyond vanilla water/lava). Gas / oil / acid addons add entries here.
     */
    public static final TagKey<Fluid> SUBMERGING_FLUIDS = TagKey.create(Registries.FLUID, RealismSurvival.id("submerging_fluids"));

    /** Fluids that are breathable when submerged (addon gills / water-breathing media). */
    public static final TagKey<Fluid> BREATHABLE_FLUIDS = TagKey.create(Registries.FLUID, RealismSurvival.id("breathable_fluids"));
}
