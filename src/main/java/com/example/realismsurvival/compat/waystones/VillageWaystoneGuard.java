package com.example.realismsurvival.compat.waystones;

import com.example.realismsurvival.RealismSurvival;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.PistonEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

/**
 * Village waystones are public anchors. Players must not break them and carry the block home.
 * Player-placed and wilderness waystones stay breakable.
 */
@EventBusSubscriber(modid = RealismSurvival.MODID)
public final class VillageWaystoneGuard {

    private VillageWaystoneGuard() {}

    @SubscribeEvent
    public static void onBreak(BreakBlockEvent event) {
        if (!protects(event.getLevel(), event.getPos(), event.getState())) {
            return;
        }
        event.setCanceled(true);
        event.setNotifyClient(true);
        if (event.getPlayer() instanceof ServerPlayer player) {
            player.sendSystemMessage(Component.translatable("message.realismsurvival.waystone.village_unbreakable"), true);
        }
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        Level level = event.getLevel();
        event.getAffectedBlocks().removeIf(pos -> protects(level, pos, level.getBlockState(pos)));
    }

    @SubscribeEvent
    public static void onPiston(PistonEvent.Pre event) {
        if (!(event.getLevel() instanceof Level level)) {
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
            if (protects(level, pos, level.getBlockState(pos))) {
                return true;
            }
        }
        return false;
    }

    /** This block, or the other half of the same waystone, was generated in a village. */
    private static boolean protects(BlockGetter level, BlockPos pos, BlockState state) {
        if (isVillageWaystone(state)) {
            return true;
        }
        if (!isWaystone(state)) {
            return false;
        }
        BlockState above = level.getBlockState(pos.above());
        BlockState below = level.getBlockState(pos.below());
        return (above.getBlock() == state.getBlock() && isVillageWaystone(above))
                || (below.getBlock() == state.getBlock() && isVillageWaystone(below));
    }

    private static boolean isWaystone(BlockState state) {
        var id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return id != null && "waystones".equals(id.getNamespace())
                && state.getBlock().getStateDefinition().getProperty("origin") != null;
    }

    private static boolean isVillageWaystone(BlockState state) {
        if (!isWaystone(state)) {
            return false;
        }
        Property<?> origin = state.getBlock().getStateDefinition().getProperty("origin");
        Comparable<?> value = state.getValue(origin);
        if (value instanceof StringRepresentable named) {
            return "village".equals(named.getSerializedName());
        }
        return "village".equals(String.valueOf(value));
    }
}
