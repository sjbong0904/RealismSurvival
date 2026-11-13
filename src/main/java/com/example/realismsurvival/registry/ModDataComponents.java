package com.example.realismsurvival.registry;

import com.example.realismsurvival.RealismSurvival;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModDataComponents {

    private ModDataComponents() {}

    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, RealismSurvival.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> BUCKET_FILL =
            COMPONENTS.registerComponentType(
                    "bucket_fill",
                    builder -> builder.persistent(Codec.intRange(1, 3)).networkSynchronized(ByteBufCodecs.VAR_INT)
            );

    /**
     * Water quality id (0–15) on {@link net.neoforged.neoforge.fluids.FluidStack} / fluid resources
     * so tanks and pipes can carry graded water without losing QUALITY.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> WATER_QUALITY =
            COMPONENTS.registerComponentType(
                    "water_quality",
                    builder -> builder.persistent(Codec.intRange(0, 15)).networkSynchronized(ByteBufCodecs.VAR_INT)
            );

    public static void register(IEventBus modEventBus) {
        COMPONENTS.register(modEventBus);
    }
}
