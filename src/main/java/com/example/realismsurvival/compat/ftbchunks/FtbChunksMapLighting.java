package com.example.realismsurvival.compat.ftbchunks;

import com.example.realismsurvival.RealismSurvival;
import com.example.realismsurvival.compat.ModCompat;
import dev.ftb.mods.ftbchunks.client.map.MapManager;
import dev.ftb.mods.ftbchunks.client.map.MapMode;
import dev.ftb.mods.ftbchunks.config.FTBChunksClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * FTB Chunks map_mode is a static day/night/topo choice. RS switches {@code none} ↔ {@code night}
 * from overworld clock so the minimap follows dusk and dawn. Other modes (topo, blocks, lights)
 * are left alone. The toggle is not written to disk.
 */
@EventBusSubscriber(modid = RealismSurvival.MODID, value = Dist.CLIENT)
public final class FtbChunksMapLighting {

    /** RS clock: 0 = 06:00, 12000 = 18:00. */
    private static final long NIGHT_START = 12_000L;

    private static int ticks;
    private static Boolean lastNight;

    private FtbChunksMapLighting() {}

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        if (!ModCompat.loaded(ModCompat.FTB_CHUNKS)) {
            return;
        }
        if (++ticks % 20 != 0) {
            return;
        }
        sync();
    }

    private static void sync() {
        Level level = Minecraft.getInstance().level;
        if (level == null || !level.dimensionType().hasSkyLight()) {
            lastNight = null;
            return;
        }

        boolean night = Math.floorMod(level.getOverworldClockTime(), 24_000L) >= NIGHT_START;
        if (lastNight != null && lastNight == night) {
            return;
        }

        MapMode current = FTBChunksClientConfig.MAP_MODE.get();
        if (current != MapMode.NONE && current != MapMode.NIGHT) {
            lastNight = null;
            return;
        }

        MapMode want = night ? MapMode.NIGHT : MapMode.NONE;
        lastNight = night;
        if (current == want) {
            return;
        }

        FTBChunksClientConfig.MAP_MODE.set(want);
        MapManager.getInstance().ifPresent(manager -> manager.updateAllRegions(true));
    }
}
