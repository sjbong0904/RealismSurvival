package com.example.realismsurvival.api;

import com.example.realismsurvival.RealismSurvival;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;

/** Datapack damage types owned by this mod (T-10). */
public final class ModDamageTypes {

    private ModDamageTypes() {}

    public static final ResourceKey<DamageType> DEHYDRATION =
            ResourceKey.create(Registries.DAMAGE_TYPE, RealismSurvival.id("dehydration"));
}
