package com.example.realismsurvival.mixin;

import com.example.realismsurvival.client.WaterFluidTint;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.block.FluidStateModelSet;
import net.minecraft.client.resources.model.sprite.MaterialBaker;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Vanilla water (including waterlogged seagrass/kelp) cannot be re-registered via
 * {@link net.neoforged.neoforge.client.event.RegisterFluidModelsEvent} ({@code putIfAbsent}).
 * Swap its tint to {@link WaterFluidTint} after bake so ocean plants match salt water and seams blend.
 */
@Mixin(FluidStateModelSet.class)
public abstract class FluidStateModelSetMixin {

    @Inject(method = "bake", at = @At("RETURN"), cancellable = true)
    private static void realismsurvival$blendVanillaWaterTint(
            MaterialBaker materials,
            CallbackInfoReturnable<Map<Fluid, FluidModel>> cir
    ) {
        Map<Fluid, FluidModel> map = new HashMap<>(cir.getReturnValue());
        FluidModel water = map.get(Fluids.WATER);
        if (water == null) {
            return;
        }
        FluidModel blended = new FluidModel(
                water.layer(),
                water.stillMaterial(),
                water.flowingMaterial(),
                water.overlayMaterial(),
                WaterFluidTint.INSTANCE,
                water.customRenderer()
        );
        map.put(Fluids.WATER, blended);
        map.put(Fluids.FLOWING_WATER, blended);
        cir.setReturnValue(Map.copyOf(map));
    }
}
