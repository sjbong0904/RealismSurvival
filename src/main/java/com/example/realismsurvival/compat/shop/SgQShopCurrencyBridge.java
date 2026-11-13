package com.example.realismsurvival.compat.shop;

import com.example.realismsurvival.RealismSurvival;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Keeps Q Shop {@code coins} equal to the SG Economy balance shown under the inventory.
 *
 * <p>All foreign types are accessed by reflection so this class loads when Q Shop / SG Economy
 * are absent (soft-compat).
 */
@EventBusSubscriber(modid = RealismSurvival.MODID)
public final class SgQShopCurrencyBridge {

    private static final ThreadLocal<Boolean> SYNCING = ThreadLocal.withInitial(() -> false);
    private static final AtomicLong SYNC_CHECKS = new AtomicLong();
    private static final AtomicLong SYNC_WRITES = new AtomicLong();
    private static final int SYNC_INTERVAL_TICKS = 40;

    private static boolean hooksRegistered;
    private static Method sgGetBalance;
    private static Method sgDeposit;
    private static Method sgWithdraw;
    private static Method qsGetBalance;
    private static Method qsSet;
    private static Method currencyService;
    private static Method eventGetPlayer;
    private static Method eventGetCurrency;
    private static Method eventGetDelta;
    private static Method eventGetSource;

    private SgQShopCurrencyBridge() {}

    static void ensureHooks() {
        if (hooksRegistered || !ShopMods.bridgeReady()) {
            return;
        }
        try {
            Class<?> sgApi = Class.forName("net.sirgrantd.sg_economy.api.SGEconomyApi");
            sgGetBalance = sgApi.getMethod("getBalance", Entity.class);
            sgDeposit = sgApi.getMethod("depositBalance", Entity.class, double.class);
            sgWithdraw = sgApi.getMethod("withdrawBalance", Entity.class, double.class);

            Class<?> addon = Class.forName("com.qshop.api.QShopAddonApi");
            currencyService = addon.getMethod("currency");
            Object service = currencyService.invoke(null);
            Class<?> serviceClass = service.getClass();
            qsGetBalance = serviceClass.getMethod("getBalance", ServerPlayer.class, String.class);
            qsSet = serviceClass.getMethod(
                    "set",
                    ServerPlayer.class,
                    String.class,
                    double.class,
                    Identifier.class,
                    net.minecraft.core.BlockPos.class,
                    boolean.class);

            Class<?> changed = Class.forName("com.qshop.api.CurrencyChangedEvent");
            eventGetPlayer = changed.getMethod("getPlayer");
            eventGetCurrency = changed.getMethod("getCurrency");
            eventGetDelta = changed.getMethod("getDelta");
            eventGetSource = changed.getMethod("getSource");

            @SuppressWarnings({"unchecked", "rawtypes"})
            Class eventClass = changed;
            NeoForge.EVENT_BUS.addListener(
                    EventPriority.NORMAL,
                    false,
                    eventClass,
                    SgQShopCurrencyBridge::onQShopCurrencyChangedRaw);

            hooksRegistered = true;
            RealismSurvival.LOGGER.info("[RealismSurvival] Q Shop ↔ SG Economy currency bridge armed");
        } catch (ReflectiveOperationException | RuntimeException ex) {
            RealismSurvival.LOGGER.warn("[RealismSurvival] Currency bridge failed to arm: {}", ex.toString());
        }
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        ensureHooks();
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        ensureHooks();
        if (!(event.getEntity() instanceof ServerPlayer player) || !hooksRegistered) {
            return;
        }
        pushSgIntoQShop(player);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !hooksRegistered) {
            return;
        }
        if (player.tickCount % SYNC_INTERVAL_TICKS != 0) {
            return;
        }
        pushSgIntoQShop(player);
    }

    private static void onQShopCurrencyChangedRaw(Event raw) {
        if (!hooksRegistered || Boolean.TRUE.equals(SYNCING.get())) {
            return;
        }
        try {
            if (!ShopMods.LINKED_CURRENCY.equals(eventGetCurrency.invoke(raw))) {
                return;
            }
            Object source = eventGetSource.invoke(raw);
            if (ShopMods.SOURCE_SG_BRIDGE.equals(source)) {
                return;
            }
            ServerPlayer player = (ServerPlayer) eventGetPlayer.invoke(raw);
            if (player == null) {
                return;
            }
            double delta = (Double) eventGetDelta.invoke(raw);
            if (delta == 0.0) {
                return;
            }
            SYNCING.set(true);
            try {
                if (delta > 0.0) {
                    sgDeposit.invoke(null, player, delta);
                } else {
                    sgWithdraw.invoke(null, player, -delta);
                }
            } finally {
                SYNCING.set(false);
            }
        } catch (ReflectiveOperationException ex) {
            RealismSurvival.LOGGER.warn("[RealismSurvival] Currency mirror failed: {}", ex.toString());
        }
    }

    /** Since server start: balance comparisons and actual SG → Q Shop writes (measurement, 7.5). */
    public static long syncChecks() {
        return SYNC_CHECKS.get();
    }

    public static long syncWrites() {
        return SYNC_WRITES.get();
    }

    /** A wallet exists to pay sell-counter proceeds into. */
    public static boolean walletAvailable() {
        return ShopMods.sgEconomy() || ShopMods.qshop();
    }

    /** Current wallet balance, or {@code 0} when no economy mod is present. */
    public static double getBalance(ServerPlayer player) {
        try {
            ensureHooks();
            if (ShopMods.sgEconomy()) {
                Method get = sgGetBalance != null
                        ? sgGetBalance
                        : Class.forName("net.sirgrantd.sg_economy.api.SGEconomyApi")
                                .getMethod("getBalance", Entity.class);
                return ((Number) get.invoke(null, player)).doubleValue();
            }
            if (ShopMods.qshop()) {
                Object service = Class.forName("com.qshop.api.QShopAddonApi").getMethod("currency").invoke(null);
                return ((Number) service.getClass()
                        .getMethod("getBalance", ServerPlayer.class, String.class)
                        .invoke(service, player, ShopMods.LINKED_CURRENCY)).doubleValue();
            }
        } catch (ReflectiveOperationException | RuntimeException ex) {
            RealismSurvival.LOGGER.warn("[RealismSurvival] Balance read failed: {}", ex.toString());
        }
        return 0.0;
    }

    public static boolean hasBalance(ServerPlayer player, double amount) {
        return amount <= 0.0 || getBalance(player) + 1.0e-6 >= amount;
    }

    /**
     * Removes {@code amount} coins. SG Economy is the balance of record when present.
     * Returns false if the wallet is missing or the balance is too low.
     */
    public static boolean withdraw(ServerPlayer player, double amount) {
        if (amount <= 0.0) {
            return true;
        }
        try {
            ensureHooks();
            if (ShopMods.sgEconomy()) {
                Method withdraw = sgWithdraw != null
                        ? sgWithdraw
                        : Class.forName("net.sirgrantd.sg_economy.api.SGEconomyApi")
                                .getMethod("withdrawBalance", Entity.class, double.class);
                Object result = withdraw.invoke(null, player, amount);
                if (Boolean.FALSE.equals(result)) {
                    return false;
                }
                pushSgIntoQShop(player);
                return true;
            }
            if (ShopMods.qshop()) {
                Object service = Class.forName("com.qshop.api.QShopAddonApi").getMethod("currency").invoke(null);
                Class<?> serviceClass = service.getClass();
                double balance = ((Number) serviceClass
                        .getMethod("getBalance", ServerPlayer.class, String.class)
                        .invoke(service, player, ShopMods.LINKED_CURRENCY)).doubleValue();
                if (balance + 1.0e-6 < amount) {
                    return false;
                }
                serviceClass
                        .getMethod(
                                "set",
                                ServerPlayer.class,
                                String.class,
                                double.class,
                                Identifier.class,
                                net.minecraft.core.BlockPos.class,
                                boolean.class)
                        .invoke(
                                service,
                                player,
                                ShopMods.LINKED_CURRENCY,
                                balance - amount,
                                ShopMods.SOURCE_SELL_COUNTER,
                                null,
                                false);
                return true;
            }
        } catch (ReflectiveOperationException | RuntimeException ex) {
            RealismSurvival.LOGGER.warn("[RealismSurvival] Withdraw failed: {}", ex.toString());
        }
        return false;
    }

    /**
     * Pays {@code amount} coins. SG Economy is the balance of record when present (then mirrored into
     * Q Shop); otherwise Q Shop {@code coins} is credited directly. Returns false if nothing was paid.
     */
    public static boolean deposit(ServerPlayer player, double amount) {
        if (amount <= 0.0) {
            return false;
        }
        try {
            if (ShopMods.sgEconomy()) {
                Method deposit = sgDeposit != null
                        ? sgDeposit
                        : Class.forName("net.sirgrantd.sg_economy.api.SGEconomyApi")
                                .getMethod("depositBalance", Entity.class, double.class);
                Object result = deposit.invoke(null, player, amount);
                if (Boolean.FALSE.equals(result)) {
                    return false;
                }
                pushSgIntoQShop(player);
                return true;
            }
            if (ShopMods.qshop()) {
                Object service = Class.forName("com.qshop.api.QShopAddonApi").getMethod("currency").invoke(null);
                Class<?> serviceClass = service.getClass();
                double balance = ((Number) serviceClass
                        .getMethod("getBalance", ServerPlayer.class, String.class)
                        .invoke(service, player, ShopMods.LINKED_CURRENCY)).doubleValue();
                serviceClass
                        .getMethod(
                                "set",
                                ServerPlayer.class,
                                String.class,
                                double.class,
                                Identifier.class,
                                net.minecraft.core.BlockPos.class,
                                boolean.class)
                        .invoke(service, player, ShopMods.LINKED_CURRENCY, balance + amount, ShopMods.SOURCE_SELL_COUNTER, null, false);
                return true;
            }
        } catch (ReflectiveOperationException | RuntimeException ex) {
            RealismSurvival.LOGGER.warn("[RealismSurvival] Sell counter payout failed: {}", ex.toString());
        }
        return false;
    }

    public static void pushSgIntoQShop(ServerPlayer player) {
        ensureHooks();
        if (!hooksRegistered || Boolean.TRUE.equals(SYNCING.get())) {
            return;
        }
        try {
            SYNC_CHECKS.incrementAndGet();
            double sg = ((Number) sgGetBalance.invoke(null, player)).doubleValue();
            Object service = currencyService.invoke(null);
            double qs = ((Number) qsGetBalance.invoke(service, player, ShopMods.LINKED_CURRENCY)).doubleValue();
            if (Math.abs(sg - qs) < 0.0001) {
                return;
            }
            SYNC_WRITES.incrementAndGet();
            SYNCING.set(true);
            try {
                qsSet.invoke(service, player, ShopMods.LINKED_CURRENCY, sg, ShopMods.SOURCE_SG_BRIDGE, null, false);
            } finally {
                SYNCING.set(false);
            }
        } catch (ReflectiveOperationException ex) {
            RealismSurvival.LOGGER.warn("[RealismSurvival] SG→QShop sync failed: {}", ex.toString());
        }
    }
}
