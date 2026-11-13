package com.example.realismsurvival.compat.shop;

import com.example.realismsurvival.RealismSurvival;
import com.example.realismsurvival.environment.ClimateZone;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.CommandEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.lang.reflect.Method;

/**
 * Opens the RS chest shop hub from Easy NPC interactions (entity tag {@code rs_shop}).
 * Buy/sell are chosen inside that chest GUI.
 */
@EventBusSubscriber(modid = RealismSurvival.MODID)
public final class SupplyShopAccess {

    private static final double NEARBY_NPC_RANGE = 6.0;

    private static Method openShop;

    private SupplyShopAccess() {}

    /**
     * Supply shops are entered through their NPCs; block QShop's direct open command for these shop IDs,
     * including when nested under {@code /execute ... run}. Gamemasters keep the command for support.
     */
    @SubscribeEvent
    public static void onCommand(CommandEvent event) {
        String command = event.getParseResults().getReader().getString().strip();
        if (command.startsWith("/")) {
            command = command.substring(1).stripLeading();
        }
        if (!opensSupplyShop(command.split("\\s+"))) {
            return;
        }
        var source = event.getParseResults().getContext().getSource();
        if (Commands.LEVEL_GAMEMASTERS.check(source.permissions())) {
            return;
        }
        event.setCanceled(true);
        ServerPlayer player = source.getPlayer();
        if (player != null) {
            player.sendSystemMessage(Component.translatable("message.realismsurvival.shop.use_merchant"), true);
        }
    }

    private static boolean opensSupplyShop(String[] parts) {
        for (int i = 0; i + 2 < parts.length; i++) {
            if ("qshop".equals(parts[i])
                    && "open".equals(parts[i + 1])
                    && ("supply_post".equals(parts[i + 2]) || parts[i + 2].startsWith("supply_post_"))) {
                return true;
            }
        }
        return false;
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer serverPlayer) || player.level().isClientSide()) {
            return;
        }
        if (!isSupplyMerchant(event.getTarget())) {
            return;
        }
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
        SupplyShopTrade.openHub(serverPlayer, event.getTarget());
    }

    public static boolean openSupplyShop(ServerPlayer player) {
        return openSupplyShop(player, nearestMerchant(player));
    }

    public static boolean openSupplyShop(ServerPlayer player, Entity merchant) {
        if (!ShopMods.shopOpenReady()) {
            player.sendSystemMessage(Component.translatable("message.realismsurvival.shop.missing_qshop"), true);
            return false;
        }
        SgQShopCurrencyBridge.pushSgIntoQShop(player);
        try {
            if (openShop == null) {
                Class<?> manager = Class.forName("com.qshop.shop.ShopManager");
                openShop = manager.getMethod("openShop", ServerPlayer.class, String.class);
            }
            BlockPos at = merchant == null ? player.blockPosition() : merchant.blockPosition();
            ClimateZone climate = ClimateZone.of(player.level().getBiome(at));
            SupplyCategory category = merchant == null ? SupplyCategory.FOOD : SupplyCategory.fromEntity(merchant);
            openShop.invoke(null, player, SeasonalSupplyShop.shopId(climate, category));
            return true;
        } catch (ReflectiveOperationException ex) {
            RealismSurvival.LOGGER.warn("[RealismSurvival] Failed to open supply shop: {}", ex.toString());
            player.sendSystemMessage(Component.translatable("message.realismsurvival.shop.open_failed"), true);
            return false;
        }
    }

    /** Creative / OP always; otherwise a tagged supply NPC must be nearby. */
    public static boolean nearSupplyMerchant(ServerPlayer player) {
        if (player.isCreative() || Commands.LEVEL_GAMEMASTERS.check(player.permissions())) {
            return true;
        }
        var box = player.getBoundingBox().inflate(NEARBY_NPC_RANGE);
        for (Entity entity : player.level().getEntities(player, box, SupplyShopAccess::isSupplyMerchant)) {
            if (entity.distanceToSqr(player) <= NEARBY_NPC_RANGE * NEARBY_NPC_RANGE) {
                return true;
            }
        }
        return false;
    }

    private static Entity nearestMerchant(ServerPlayer player) {
        Entity nearest = null;
        double best = NEARBY_NPC_RANGE * NEARBY_NPC_RANGE;
        AABB box = player.getBoundingBox().inflate(NEARBY_NPC_RANGE);
        for (Entity entity : player.level().getEntities(player, box, SupplyShopAccess::isSupplyMerchant)) {
            double distance = entity.distanceToSqr(player);
            if (distance <= best) {
                best = distance;
                nearest = entity;
            }
        }
        return nearest;
    }

    public static boolean isSupplyMerchant(Entity entity) {
        if (entity == null || !entity.entityTags().contains(ShopMods.NPC_SHOP_TAG)) {
            return false;
        }
        Identifier typeId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return typeId != null && ShopMods.EASY_NPC.equals(typeId.getNamespace());
    }
}
