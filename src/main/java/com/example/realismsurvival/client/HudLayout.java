package com.example.realismsurvival.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

/**
 * Shared HUD geometry constants and visibility gate (spec §8).
 * Temporary placeholder sprites use these same numbers so custom art can drop in later.
 */
public final class HudLayout {

    private HudLayout() {}

    public static final int ICON_COUNT = 10;
    public static final int ICON_SIZE = 9;
    public static final int ICON_STRIDE = 8;
    public static final int ROW_HEIGHT = 10;
    /** Half of the hotbar width (182 / 2). */
    public static final int STATUS_BAR_HALF_WIDTH = 91;

    /**
     * Same visibility rules as vanilla survival layers: GUI visible, can take damage, camera is a player.
     */
    public static Player cameraPlayer(Minecraft minecraft) {
        if (minecraft.options.hideGui || minecraft.gameMode == null || !minecraft.gameMode.canHurtPlayer()) {
            return null;
        }
        return minecraft.getCameraEntity() instanceof Player player ? player : null;
    }
}
