package com.example.realismsurvival.mixin.sodium;

import net.caffeinemc.mods.sodium.client.world.biome.LevelBiomeSlice;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Sodium chunk slices keep biomes here; the slice itself is not a {@code LevelReader}. */
@Mixin(targets = "net.caffeinemc.mods.sodium.client.world.LevelSlice", remap = false)
public interface LevelSliceBiomeAccess {

    @Accessor("biomeSlice")
    LevelBiomeSlice realismsurvival$biomeSlice();
}
