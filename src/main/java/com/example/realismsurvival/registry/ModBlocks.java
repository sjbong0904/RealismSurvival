package com.example.realismsurvival.registry;

import com.example.realismsurvival.RealismSurvival;
import com.example.realismsurvival.block.ClinicTableBlock;
import com.example.realismsurvival.block.SupplyPostMarkerBlock;
import com.example.realismsurvival.block.WaterFilterBlock;
import com.example.realismsurvival.block.WaterStillBlock;
import com.example.realismsurvival.water.GradedIceBlock;
import com.example.realismsurvival.water.GradedLiquidBlock;
import com.example.realismsurvival.water.WaterQualityStates;
import java.util.function.Supplier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Canonical graded-water liquid block, plus legacy block ids (same fluid + default QUALITY) so old
 * chunks load; {@link com.example.realismsurvival.water.VanillaWaterConverter} rewrites them.
 */
public final class ModBlocks {

    private ModBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(RealismSurvival.MODID);

    /** Canonical block; default QUALITY = fresh (registry id 1 with built-in registration order). */
    public static final DeferredBlock<LiquidBlock> GRADED_WATER =
            registerWaterBlock("graded_water", ModFluids.GRADED_WATER, 1);

    /** Legacy ids — default QUALITY matches built-in registry order (distilled, fresh, stagnant, salt). */
    public static final DeferredBlock<LiquidBlock> DISTILLED_WATER =
            registerWaterBlock("distilled_water", ModFluids.GRADED_WATER, 0);
    public static final DeferredBlock<LiquidBlock> FRESH_WATER =
            registerWaterBlock("fresh_water", ModFluids.GRADED_WATER, 1);
    public static final DeferredBlock<LiquidBlock> STAGNANT_WATER =
            registerWaterBlock("stagnant_water", ModFluids.GRADED_WATER, 2);
    public static final DeferredBlock<LiquidBlock> SALT_WATER =
            registerWaterBlock("salt_water", ModFluids.GRADED_WATER, 3);

    public static final DeferredBlock<WaterFilterBlock> WATER_FILTER = BLOCKS.registerBlock(
            "water_filter",
            WaterFilterBlock::new,
            () -> BlockBehaviour.Properties.of().strength(0.5F)
    );

    public static final DeferredBlock<WaterStillBlock> WATER_STILL = BLOCKS.registerBlock(
            "water_still",
            WaterStillBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.FURNACE)
    );

    /** Compact salt storage from distillation byproduct. */
    public static final DeferredBlock<Block> SALT_BLOCK = BLOCKS.registerBlock(
            "salt_block",
            Block::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.SAND).strength(0.75F)
    );
    /** Shared ice for all water grades; QUALITY matches the fluid that froze / biome at convert. */
    public static final DeferredBlock<GradedIceBlock> GRADED_ICE = BLOCKS.registerBlock(
            "graded_ice",
            GradedIceBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.ICE)
    );

    /** Worldgen stall anchor; spawns the tagged Easy NPC supply merchant. */
    public static final DeferredBlock<SupplyPostMarkerBlock> SUPPLY_POST_MARKER = BLOCKS.registerBlock(
            "supply_post_marker",
            SupplyPostMarkerBlock::new,
            () -> BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(1.5F)
                    .sound(SoundType.WOOD)
    );

    /** Doctor villager workstation. */
    public static final DeferredBlock<ClinicTableBlock> CLINIC_TABLE = BLOCKS.registerBlock(
            "clinic_table",
            ClinicTableBlock::new,
            () -> BlockBehaviour.Properties.of()
                    .mapColor(MapColor.QUARTZ)
                    .strength(1.5F)
                    .sound(SoundType.WOOD)
                    .noOcclusion()
    );

    private static DeferredBlock<LiquidBlock> registerWaterBlock(
            String name,
            Supplier<? extends FlowingFluid> fluid,
            int defaultQualityId
    ) {
        int qualityId = WaterQualityStates.clampId(defaultQualityId);
        return BLOCKS.registerBlock(
                name,
                props -> new GradedLiquidBlock(fluid.get(), props, qualityId),
                () -> BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).noLootTable()
        );
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }
}
