package com.example.realismsurvival.registry;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper;

/**
 * Expose filter/still inventories on {@link Capabilities.Item#BLOCK} (hoppers / Create funnels)
 * and fluid tanks on {@link Capabilities.Fluid#BLOCK} (pipes / Create pumps when available).
 */
public final class ModCapabilities {

    private ModCapabilities() {}

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ModCapabilities::onRegisterCapabilities);
    }

    private static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.Item.BLOCK,
                ModBlockEntities.WATER_FILTER.get(),
                WorldlyContainerWrapper::new
        );
        event.registerBlockEntity(
                Capabilities.Item.BLOCK,
                ModBlockEntities.WATER_STILL.get(),
                WorldlyContainerWrapper::new
        );
        event.registerBlockEntity(
                Capabilities.Fluid.BLOCK,
                ModBlockEntities.WATER_FILTER.get(),
                (be, side) -> be.getFluidInput().handler()
        );
        event.registerBlockEntity(
                Capabilities.Fluid.BLOCK,
                ModBlockEntities.WATER_STILL.get(),
                (be, side) -> be.getFluidInput().handler()
        );
    }
}
