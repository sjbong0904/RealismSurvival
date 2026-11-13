package com.example.realismsurvival.compat.waystones;

import com.example.realismsurvival.RealismSurvival;
import com.example.realismsurvival.RealismSurvivalConfig;
import com.example.realismsurvival.compat.ModCompat;
import com.example.realismsurvival.compat.shop.SgQShopCurrencyBridge;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

/**
 * Replaces Waystones XP costs with inventory gold for waystone / warp-stone travel.
 * Return / warp / blank scrolls stay free (consumable already).
 */
@EventBusSubscriber(modid = RealismSurvival.MODID)
public final class WaystoneGoldToll {

    private static final Map<UUID, Integer> PENDING = new ConcurrentHashMap<>();
    private static boolean hooked;

    private WaystoneGoldToll() {}

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        ensureHooked();
    }

    private static void ensureHooked() {
        if (hooked || !ModCompat.loaded(ModCompat.WAYSTONES)) {
            return;
        }
        try {
            Class<?> before = Class.forName("net.blay09.mods.waystones.api.event.WaystoneTeleportEvent$Before");
            Class<?> after = Class.forName("net.blay09.mods.waystones.api.event.WaystoneTeleportEvent$After");
            register(before, WaystoneGoldToll::onBeforeRaw);
            register(after, WaystoneGoldToll::onAfterRaw);
            hooked = true;
            RealismSurvival.LOGGER.info("[RealismSurvival] Waystone gold toll hooked");
        } catch (ReflectiveOperationException | RuntimeException ex) {
            RealismSurvival.LOGGER.warn("[RealismSurvival] Waystone gold toll hook failed: {}", ex.toString());
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void register(Class<?> eventClass, java.util.function.Consumer<Object> handler)
            throws ReflectiveOperationException {
        Field eventField = eventClass.getField("EVENT");
        Object mapper = eventField.get(null);
        Method register = mapper.getClass().getMethod("register", Object.class);
        register.invoke(mapper, handler);
    }

    private static void onBeforeRaw(Object raw) {
        if (!RealismSurvivalConfig.WAYSTONE_GOLD_TOLL_ENABLED.getAsBoolean()) {
            return;
        }
        try {
            Object context = raw.getClass().getMethod("getContext").invoke(raw);
            Entity entity = (Entity) context.getClass().getMethod("getEntity").invoke(context);
            if (!(entity instanceof ServerPlayer player)) {
                return;
            }
            ItemStack warpItem = (ItemStack) context.getClass().getMethod("getWarpItem").invoke(context);
            if (isFreeScroll(warpItem)) {
                PENDING.remove(player.getUUID());
                return;
            }
            if (!SgQShopCurrencyBridge.walletAvailable()) {
                return;
            }
            int cost = computeCost(context, player);
            if (cost <= 0) {
                PENDING.remove(player.getUUID());
                return;
            }
            if (!SgQShopCurrencyBridge.hasBalance(player, cost)) {
                raw.getClass().getMethod("setCanceled", boolean.class).invoke(raw, true);
                player.sendSystemMessage(
                        Component.translatable("message.realismsurvival.waystone.toll_insufficient", cost),
                        true);
                PENDING.remove(player.getUUID());
                return;
            }
            PENDING.put(player.getUUID(), cost);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            RealismSurvival.LOGGER.warn("[RealismSurvival] Waystone toll Before failed: {}", ex.toString());
        }
    }

    private static void onAfterRaw(Object raw) {
        if (!RealismSurvivalConfig.WAYSTONE_GOLD_TOLL_ENABLED.getAsBoolean()) {
            PENDING.clear();
            return;
        }
        try {
            Object context = raw.getClass().getMethod("getContext").invoke(raw);
            Entity entity = (Entity) context.getClass().getMethod("getEntity").invoke(context);
            if (!(entity instanceof ServerPlayer player)) {
                return;
            }
            Integer cost = PENDING.remove(player.getUUID());
            if (cost == null || cost <= 0) {
                return;
            }
            if (!SgQShopCurrencyBridge.withdraw(player, cost)) {
                player.sendSystemMessage(
                        Component.translatable("message.realismsurvival.waystone.toll_charge_failed", cost),
                        true);
                return;
            }
            player.sendSystemMessage(
                    Component.translatable("message.realismsurvival.waystone.toll_charged", cost),
                    true);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            RealismSurvival.LOGGER.warn("[RealismSurvival] Waystone toll After failed: {}", ex.toString());
        }
    }

    private static boolean isFreeScroll(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!"waystones".equals(id.getNamespace())) {
            return false;
        }
        String path = id.getPath();
        return path.contains("scroll");
    }

    private static int computeCost(Object context, ServerPlayer player) throws ReflectiveOperationException {
        Object target = context.getClass().getMethod("getTargetWaystone").invoke(context);
        BlockPos to = (BlockPos) target.getClass().getMethod("getPos").invoke(target);
        BlockPos from = player.blockPosition();
        Object fromOpt = context.getClass().getMethod("getFromWaystone").invoke(context);
        if (fromOpt instanceof java.util.Optional<?> opt && opt.isPresent()) {
            Object fromStone = opt.get();
            from = (BlockPos) fromStone.getClass().getMethod("getPos").invoke(fromStone);
        }
        double dist = Math.sqrt(from.distToCenterSqr(to.getX() + 0.5, to.getY() + 0.5, to.getZ() + 0.5));
        int per100 = RealismSurvivalConfig.WAYSTONE_GOLD_TOLL_PER_100_BLOCKS.getAsInt();
        int distanceFee = per100 <= 0 ? 0 : (int) Math.ceil(dist / 100.0) * per100;
        int total = RealismSurvivalConfig.WAYSTONE_GOLD_TOLL_BASE.getAsInt() + distanceFee;
        boolean dimensional = Boolean.TRUE.equals(context.getClass().getMethod("isDimensionalTeleport").invoke(context));
        if (dimensional) {
            total = Math.max(total, RealismSurvivalConfig.WAYSTONE_GOLD_TOLL_MAX.getAsInt());
        }
        return Math.min(total, RealismSurvivalConfig.WAYSTONE_GOLD_TOLL_MAX.getAsInt());
    }
}
