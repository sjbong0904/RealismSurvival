package com.example.rsapisample;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

/**
 * Example addon for Realism Survival — see {@code docs/ADDON_API.md}.
 * Not shipped in the core jar; loaded from the {@code addonExample} source set when enabled.
 */
@Mod(RsApiSample.MODID)
public final class RsApiSample {

    public static final String MODID = "rsapisample";
    public static final Logger LOGGER = LogUtils.getLogger();

    public RsApiSample(IEventBus modEventBus) {
        SampleItems.register(modEventBus);
        SampleWaterQualities.register(modEventBus);
        modEventBus.addListener(this::onCommonSetup);
        NeoForge.EVENT_BUS.register(SampleDrinkEvents.class);
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("[rs-api-sample] Loaded against Realism Survival API (mineral water grade).");
    }
}
