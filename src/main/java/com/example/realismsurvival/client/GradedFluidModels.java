package com.example.realismsurvival.client;

import com.example.realismsurvival.registry.ModFluids;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;

/** Registers the shared graded-water fluid model + {@link WaterFluidTint}. */
public final class GradedFluidModels {

    private static final Material STILL =
            new Material(Identifier.withDefaultNamespace("block/water_still"));
    private static final Material FLOW =
            new Material(Identifier.withDefaultNamespace("block/water_flow"));
    private static final Material OVERLAY =
            new Material(Identifier.withDefaultNamespace("block/water_overlay"));

    private GradedFluidModels() {}

    public static void register(RegisterFluidModelsEvent event) {
        event.register(
                new FluidModel.Unbaked(STILL, FLOW, OVERLAY, WaterFluidTint.INSTANCE),
                ModFluids.GRADED_WATER,
                ModFluids.FLOWING_GRADED_WATER
        );
    }
}
