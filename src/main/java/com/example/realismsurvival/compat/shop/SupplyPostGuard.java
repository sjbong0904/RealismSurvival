package com.example.realismsurvival.compat.shop;

import com.example.realismsurvival.RealismSurvival;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDestroyBlockEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.PistonEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

/** Generated supply stalls stay intact. Player-placed waystones and other builds are untouched. */
@EventBusSubscriber(modid = RealismSurvival.MODID)
public final class SupplyPostGuard {

    private SupplyPostGuard() {}

    @SubscribeEvent
    public static void onBreak(BreakBlockEvent event) {
        if (!SupplyPostSite.contains(event.getLevel(), event.getPos())) {
            return;
        }
        event.setCanceled(true);
        event.setNotifyClient(true);
        tell(event.getPlayer());
    }

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (!SupplyPostSite.contains(event.getLevel(), event.getPos())) {
            return;
        }
        event.setCanceled(true);
        if (event.getEntity() instanceof Player player) {
            tell(player);
        }
    }

    @SubscribeEvent
    public static void onTool(BlockEvent.BlockToolModificationEvent event) {
        if (event.isSimulated() || !SupplyPostSite.contains(event.getLevel(), event.getPos())) {
            return;
        }
        event.setCanceled(true);
        tell(event.getPlayer());
    }

    @SubscribeEvent
    public static void onLivingDestroy(LivingDestroyBlockEvent event) {
        if (SupplyPostSite.contains(event.getEntity().level(), event.getPos())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        Level level = event.getLevel();
        event.getAffectedBlocks().removeIf(pos -> SupplyPostSite.contains(level, pos));
    }

    @SubscribeEvent
    public static void onPiston(PistonEvent.Pre event) {
        if (!(event.getLevel() instanceof BlockGetter level)) {
            return;
        }
        PistonStructureResolver helper = event.getStructureHelper();
        if (helper == null || !helper.resolve()) {
            return;
        }
        if (touches(level, helper.getToPush()) || touches(level, helper.getToDestroy())) {
            event.setCanceled(true);
        }
    }

    private static boolean touches(BlockGetter level, List<BlockPos> positions) {
        for (BlockPos pos : positions) {
            if (SupplyPostSite.contains(level, pos)) {
                return true;
            }
        }
        return false;
    }

    private static void tell(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(Component.translatable("message.realismsurvival.supply_post.protected"), true);
        }
    }
}
