package com.example.rsmedicine;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

/**
 * Pack medicine addon — HP recovery, short vanilla afflictions, cures.
 * See {@code docs/MODPACK.md} «rs-medicine».
 */
@Mod(RsMedicine.MODID)
public final class RsMedicine {

    public static final String MODID = "rsmedicine";
    public static final Logger LOGGER = LogUtils.getLogger();

    public RsMedicine(IEventBus modEventBus) {
        MedicineEffects.register(modEventBus);
        MedicineItems.register(modEventBus);
        modEventBus.addListener(this::onCommonSetup);
        modEventBus.addListener(MedicineCreativeTabs::onBuildTabs);
        NeoForge.EVENT_BUS.register(AfflictionHandler.class);
        NeoForge.EVENT_BUS.register(MedicineBonusHandler.class);
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("[rs-medicine] Loaded (heal items, affliction, cures).");
    }
}
