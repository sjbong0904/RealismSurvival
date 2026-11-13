package com.example.rsmedicine;

import com.example.rsmedicine.client.MedicineCureIconsClientTooltip;
import com.example.rsmedicine.client.MedicineCureIconsTooltip;
import com.example.rsmedicine.client.MedicineHealClientTooltip;
import com.example.rsmedicine.client.MedicineHealTooltip;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;

@Mod(value = RsMedicine.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = RsMedicine.MODID, value = Dist.CLIENT)
public final class RsMedicineClient {

    public RsMedicineClient() {}

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        RsMedicine.LOGGER.info("[rs-medicine] Client setup complete");
    }

    @SubscribeEvent
    static void onRegisterTooltipFactories(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(MedicineHealTooltip.class, MedicineHealClientTooltip::new);
        event.register(MedicineCureIconsTooltip.class, MedicineCureIconsClientTooltip::new);
    }
}
