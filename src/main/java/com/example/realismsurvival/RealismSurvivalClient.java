package com.example.realismsurvival;

import com.example.realismsurvival.client.ClientSurvivalData;
import com.example.realismsurvival.client.DistilledBrewingClient;
import com.example.realismsurvival.client.GradedFluidModels;
import com.example.realismsurvival.water.ClientBrewingBridge;
import com.example.realismsurvival.client.GradedIceItemTint;
import com.example.realismsurvival.client.GradedIceTint;
import com.example.realismsurvival.client.GradedWaterTint;
import com.example.realismsurvival.client.RealismSurvivalHUD;
import com.example.realismsurvival.compat.ftbchunks.FtbChunksInstruments;
import com.example.realismsurvival.client.ClinicScreen;
import com.example.realismsurvival.client.SupplyCounterScreen;
import com.example.realismsurvival.client.SupplyShopScreen;
import com.example.realismsurvival.client.TemperatureStatsClientTooltip;
import com.example.realismsurvival.client.TemperatureStatsTooltip;
import com.example.realismsurvival.client.ThirstStatsClientTooltip;
import com.example.realismsurvival.client.ThirstStatsTooltip;
import com.example.realismsurvival.client.WaterFilterScreen;
import com.example.realismsurvival.client.WaterStillScreen;
import com.example.realismsurvival.client.sodium.SodiumWaterTint;
import com.example.realismsurvival.registry.ModBlocks;
import java.util.List;
import com.example.realismsurvival.registry.ModMenus;
import com.example.realismsurvival.network.SyncSurvivalDataPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

/**
 * Client-only entry point. FML instantiates this class exclusively on the physical client
 * ({@code dist = Dist.CLIENT}), which is what makes it safe to reference {@code net.minecraft.client.*}
 * from here and from everything under {@code com.example.realismsurvival.client}.
 *
 * <p>{@code @EventBusSubscriber(value = Dist.CLIENT)} registers the static {@code @SubscribeEvent} methods
 * below; the bus (mod vs. game) is inferred from each event type.
 */
@Mod(value = RealismSurvival.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = RealismSurvival.MODID, value = Dist.CLIENT)
public final class RealismSurvivalClient {

    public RealismSurvivalClient(ModContainer container) {
        // Mods screen -> Realism Survival -> Config: auto-generated screen for RealismSurvivalConfig.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        ClientBrewingBridge.bind(DistilledBrewingClient::brewingOrNull);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        if (ModList.get().isLoaded("sodium")) {
            SodiumWaterTint.register();
        }
        event.enqueueWork(FtbChunksInstruments::registerIfPresent);
        RealismSurvival.LOGGER.info("[RealismSurvival] Client setup complete");
    }

    /** Mod-bus event, fired once while the HUD layer list is being assembled. */
    @SubscribeEvent
    static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        RealismSurvivalHUD.register(event);
    }

    /** Thirst / temperature icon rows on edible tooltips (hunger/saturation left to AppleSkin). */
    @SubscribeEvent
    static void onRegisterTooltipFactories(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(ThirstStatsTooltip.class, ThirstStatsClientTooltip::new);
        event.register(TemperatureStatsTooltip.class, TemperatureStatsClientTooltip::new);
    }

    /** Registers graded-water / graded-ice item tints. */
    @SubscribeEvent
    static void onRegisterItemTintSources(RegisterColorHandlersEvent.ItemTintSources event) {
        event.register(GradedWaterTint.ID, GradedWaterTint.MAP_CODEC);
        event.register(GradedIceItemTint.ID, GradedIceItemTint.MAP_CODEC);
    }

    @SubscribeEvent
    static void onRegisterBlockTintSources(RegisterColorHandlersEvent.BlockTintSources event) {
        event.register(List.of(GradedIceTint.INSTANCE), ModBlocks.GRADED_ICE.get());
    }

    /** World fluid meshes for distilled / fresh / stagnant / salt water. */
    @SubscribeEvent
    static void onRegisterFluidModels(RegisterFluidModelsEvent event) {
        GradedFluidModels.register(event);
    }

    /** Datapack → client sync (T-09). Handler kept here so dedicated servers never classload {@link ClientSurvivalData}. */
    @SubscribeEvent
    static void onRegisterClientPayloads(RegisterClientPayloadHandlersEvent event) {
        event.register(SyncSurvivalDataPayload.TYPE, (payload, context) -> ClientSurvivalData.apply(payload));
    }

    @SubscribeEvent
    static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.WATER_FILTER.get(), WaterFilterScreen::new);
        event.register(ModMenus.WATER_STILL.get(), WaterStillScreen::new);
        event.register(ModMenus.SUPPLY_COUNTER.get(), SupplyCounterScreen::new);
        event.register(ModMenus.SUPPLY_SHOP.get(), SupplyShopScreen::new);
        event.register(ModMenus.CLINIC.get(), ClinicScreen::new);
    }
}
