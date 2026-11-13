package com.example.realismsurvival.registry;

import com.example.realismsurvival.RealismSurvival;
import com.example.realismsurvival.world.inventory.ClinicMenu;
import com.example.realismsurvival.world.inventory.SupplyCounterMenu;
import com.example.realismsurvival.world.inventory.SupplyShopMenu;
import com.example.realismsurvival.world.inventory.WaterFilterMenu;
import com.example.realismsurvival.world.inventory.WaterStillMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {

    private ModMenus() {}

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, RealismSurvival.MODID);

    /** Furnace-style: client ctor uses empty container; slots/data sync from server. No extra open-menu buffer. */
    public static final DeferredHolder<MenuType<?>, MenuType<WaterFilterMenu>> WATER_FILTER =
            MENUS.register("water_filter", () -> new MenuType<>(WaterFilterMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<WaterStillMenu>> WATER_STILL =
            MENUS.register("water_still", () -> new MenuType<>(WaterStillMenu::new, FeatureFlags.DEFAULT_FLAGS));

    /** Supply sell counter; client ctor gets empty slots and the quote arrives through data slots. */
    public static final DeferredHolder<MenuType<?>, MenuType<SupplyCounterMenu>> SUPPLY_COUNTER =
            MENUS.register("supply_counter", () -> new MenuType<>(SupplyCounterMenu::new, FeatureFlags.DEFAULT_FLAGS));

    /** Chest hub + buy catalog. Display items sync through slots. */
    public static final DeferredHolder<MenuType<?>, MenuType<SupplyShopMenu>> SUPPLY_SHOP =
            MENUS.register("supply_shop", () -> new MenuType<>(SupplyShopMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<ClinicMenu>> CLINIC =
            MENUS.register("clinic", () -> new MenuType<>(ClinicMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static void register(IEventBus modEventBus) {
        MENUS.register(modEventBus);
    }
}
