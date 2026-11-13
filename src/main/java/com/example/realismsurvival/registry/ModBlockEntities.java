package com.example.realismsurvival.registry;

import com.example.realismsurvival.RealismSurvival;
import com.example.realismsurvival.blockentity.SupplyPostMarkerBlockEntity;
import com.example.realismsurvival.blockentity.WaterFilterBlockEntity;
import com.example.realismsurvival.blockentity.WaterStillBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {

    private ModBlockEntities() {}

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, RealismSurvival.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WaterFilterBlockEntity>> WATER_FILTER =
            BLOCK_ENTITIES.register(
                    "water_filter",
                    () -> new BlockEntityType<>(WaterFilterBlockEntity::new, ModBlocks.WATER_FILTER.get())
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WaterStillBlockEntity>> WATER_STILL =
            BLOCK_ENTITIES.register(
                    "water_still",
                    () -> new BlockEntityType<>(WaterStillBlockEntity::new, ModBlocks.WATER_STILL.get())
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SupplyPostMarkerBlockEntity>> SUPPLY_POST_MARKER =
            BLOCK_ENTITIES.register(
                    "supply_post_marker",
                    () -> new BlockEntityType<>(SupplyPostMarkerBlockEntity::new, ModBlocks.SUPPLY_POST_MARKER.get())
            );

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITIES.register(modEventBus);
    }
}
