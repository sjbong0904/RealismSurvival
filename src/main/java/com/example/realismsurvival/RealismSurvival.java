package com.example.realismsurvival;

import com.example.realismsurvival.compat.ModCompat;
import com.example.realismsurvival.component.SurvivalAttachments;
import com.example.realismsurvival.event.CauldronInteractionHandler;
import com.example.realismsurvival.event.WaterDispenseBehaviors;
import com.example.realismsurvival.gametest.SurvivalGameTests;
import com.example.realismsurvival.registry.ModBlockEntities;
import com.example.realismsurvival.registry.ModBlocks;
import com.example.realismsurvival.registry.ModCapabilities;
import com.example.realismsurvival.registry.ModDataComponents;
import com.example.realismsurvival.registry.ModFeatures;
import com.example.realismsurvival.registry.ModFluidTypes;
import com.example.realismsurvival.registry.ModFluids;
import com.example.realismsurvival.registry.ModItems;
import com.example.realismsurvival.registry.ModMenus;
import com.example.realismsurvival.registry.ModRecipeSerializers;
import com.example.realismsurvival.registry.ModVillagers;
import com.example.realismsurvival.registry.ModWaterQualities;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;

/**
 * Common entry point — loaded on <b>both</b> the client and the dedicated server.
 *
 * <p>Wiring rules (spec §10):
 * <ul>
 *   <li>Everything registered here must be side-agnostic: registries (attachments), configs, common setup.</li>
 *   <li>Client-only work (HUD layers, screens, key bindings) lives in {@link RealismSurvivalClient}, a second
 *       {@code @Mod} class with {@code dist = Dist.CLIENT}. FML never loads that class on a dedicated server,
 *       so no {@code net.minecraft.client.*} symbol can leak into server classloading.</li>
 *   <li>Game-bus listeners ({@code PlayerTickEvent}, {@code AddServerReloadListenersEvent}, ...) are discovered
 *       through {@code @EventBusSubscriber(modid = MODID)} on their classes; nothing needs to be registered here.</li>
 * </ul>
 */
@Mod(RealismSurvival.MODID)
public final class RealismSurvival {

    /** Must match {@code mod_id} in gradle.properties and the {@code [[mods]].modId} in neoforge.mods.toml. */
    public static final String MODID = "realismsurvival";
    public static final Logger LOGGER = LogUtils.getLogger();

    public RealismSurvival(IEventBus modEventBus, ModContainer modContainer) {
        // Registries -> mod bus. Attachment types are frozen during RegisterEvent, so this must happen in the constructor.
        SurvivalAttachments.register(modEventBus);
        ModWaterQualities.register(modEventBus);
        ModFluidTypes.register(modEventBus);
        ModFluids.register(modEventBus);
        ModDataComponents.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModFeatures.register(modEventBus);
        ModCapabilities.register(modEventBus);
        ModMenus.register(modEventBus);
        ModVillagers.register(modEventBus);
        ModRecipeSerializers.register(modEventBus);
        SurvivalGameTests.register(modEventBus);

        // Lifecycle.
        modEventBus.addListener(this::onCommonSetup);
        modEventBus.addListener(CauldronInteractionHandler::register);
        modEventBus.addListener(this::onConfigLoad);
        modEventBus.addListener(this::onConfigReload);

        // Server config: lives in <world>/serverconfig and is synced to clients on join.
        modContainer.registerConfig(ModConfig.Type.SERVER, RealismSurvivalConfig.SPEC);
    }

    private void onConfigLoad(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == RealismSurvivalConfig.SPEC) {
            RealismSurvivalConfig.logValidationWarnings();
        }
    }

    private void onConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == RealismSurvivalConfig.SPEC) {
            RealismSurvivalConfig.logValidationWarnings();
        }
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(WaterDispenseBehaviors::register);
        event.enqueueWork(ModCompat::logDetected);
        LOGGER.info("[RealismSurvival] Common setup complete (attachment: {})", SurvivalAttachments.SURVIVAL_DATA.getId());
    }

    /** {@code realismsurvival:<path>} helper used everywhere an {@link Identifier} is needed. */
    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }
}
