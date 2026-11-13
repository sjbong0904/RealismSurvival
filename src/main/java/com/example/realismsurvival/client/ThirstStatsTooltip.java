package com.example.realismsurvival.client;

import net.minecraft.world.inventory.tooltip.TooltipComponent;

/**
 * Thirst restore preview as HUD-style droplet icons. Hunger/saturation come from AppleSkin.
 *
 * @param thirstRestore thirst points on the 0–100 scale (may be negative); 0 = omit row
 */
public record ThirstStatsTooltip(float thirstRestore) implements TooltipComponent {

    public boolean hasRow() {
        return thirstRestore != 0.0F;
    }
}
