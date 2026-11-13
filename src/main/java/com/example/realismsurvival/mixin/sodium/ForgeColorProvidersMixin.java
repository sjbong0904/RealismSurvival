package com.example.realismsurvival.mixin.sodium;

import com.example.realismsurvival.client.WaterFluidTint;
import com.example.realismsurvival.client.sodium.SodiumWaterFluidColorProvider;
import net.caffeinemc.mods.sodium.client.model.color.ColorProvider;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.client.fluid.FluidTintSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Swap Sodium's flat Forge fluid tint adapter for biome-style vertex blending on our water tint.
 */
@Mixin(targets = "net.caffeinemc.mods.sodium.neoforge.render.ForgeColorProviders", remap = false)
public abstract class ForgeColorProvidersMixin {

    @Inject(method = "adapt", at = @At("HEAD"), cancellable = true, remap = false)
    private static void realismsurvival$blendedWaterTint(
            FluidTintSource handler,
            CallbackInfoReturnable<ColorProvider<FluidState>> cir
    ) {
        if (handler == WaterFluidTint.INSTANCE) {
            cir.setReturnValue(SodiumWaterFluidColorProvider.INSTANCE);
        }
    }
}
