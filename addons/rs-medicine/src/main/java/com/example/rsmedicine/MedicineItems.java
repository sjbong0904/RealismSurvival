package com.example.rsmedicine;

import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class MedicineItems {

    private MedicineItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(RsMedicine.MODID);

    public static final DeferredItem<Item> BANDAGE = ITEMS.registerItem(
            "bandage",
            props -> new MedicineItem(MedicineAction.heal(4.0F), "tooltip.rsmedicine.bandage", props),
            MedicineItems::stackable
    );

    public static final DeferredItem<Item> TRAUMA_KIT = ITEMS.registerItem(
            "trauma_kit",
            props -> new MedicineItem(MedicineAction.heal(8.0F), "tooltip.rsmedicine.trauma_kit", props),
            MedicineItems::stackable
    );

    public static final DeferredItem<Item> MEDKIT = ITEMS.registerItem(
            "medkit",
            props -> new MedicineItem(MedicineAction.heal(12.0F), "tooltip.rsmedicine.medkit", props),
            MedicineItems::stackable
    );

    public static final DeferredItem<Item> ANTIDOTE = ITEMS.registerItem(
            "antidote",
            props -> new MedicineItem(MedicineAction.antidote(), "tooltip.rsmedicine.antidote", props),
            MedicineItems::stackable
    );

    public static final DeferredItem<Item> BURN_SALVE = ITEMS.registerItem(
            "burn_salve",
            props -> new MedicineItem(MedicineAction.burnSalve(), "tooltip.rsmedicine.burn_salve", props),
            MedicineItems::stackable
    );

    public static final DeferredItem<Item> WARM_PACK = ITEMS.registerItem(
            "warm_pack",
            props -> new MedicineItem(MedicineAction.warmPack(), "tooltip.rsmedicine.warm_pack", props),
            MedicineItems::stackable
    );

    public static final DeferredItem<Item> STIMULANT = ITEMS.registerItem(
            "stimulant",
            props -> new MedicineItem(MedicineAction.stimulant(), "tooltip.rsmedicine.stimulant", props),
            MedicineItems::stackable
    );

    public static final DeferredItem<Item> EYE_DROPS = ITEMS.registerItem(
            "eye_drops",
            props -> new MedicineItem(MedicineAction.eyeDrops(), "tooltip.rsmedicine.eye_drops", props),
            MedicineItems::stackable
    );

    public static final DeferredItem<Item> FIELD_KIT = ITEMS.registerItem(
            "field_kit",
            props -> new MedicineItem(MedicineAction.fieldKit(), "tooltip.rsmedicine.field_kit", props),
            MedicineItems::stackable
    );

    public static final DeferredItem<Item> CLINIC_KIT = ITEMS.registerItem(
            "clinic_kit",
            props -> new MedicineItem(MedicineAction.clinicKit(10.0F), "tooltip.rsmedicine.clinic_kit", props),
            MedicineItems::stackable
    );

    private static Item.Properties stackable() {
        return new Item.Properties().stacksTo(16);
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
