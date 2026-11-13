package com.example.rsmedicine;

import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

final class MedicineCreativeTabs {

    private MedicineCreativeTabs() {}

    static void onBuildTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() != CreativeModeTabs.TOOLS_AND_UTILITIES
                && event.getTabKey() != CreativeModeTabs.FOOD_AND_DRINKS) {
            return;
        }
        event.accept(MedicineItems.BANDAGE);
        event.accept(MedicineItems.TRAUMA_KIT);
        event.accept(MedicineItems.MEDKIT);
        event.accept(MedicineItems.ANTIDOTE);
        event.accept(MedicineItems.BURN_SALVE);
        event.accept(MedicineItems.WARM_PACK);
        event.accept(MedicineItems.STIMULANT);
        event.accept(MedicineItems.EYE_DROPS);
        event.accept(MedicineItems.FIELD_KIT);
        event.accept(MedicineItems.CLINIC_KIT);
    }
}
