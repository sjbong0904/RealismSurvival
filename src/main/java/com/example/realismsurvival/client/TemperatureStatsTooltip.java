package com.example.realismsurvival.client;

import net.minecraft.world.inventory.tooltip.TooltipComponent;

/**
 * Body-temperature regulation preview as HUD-style thermometer icons (warm / cool tint).
 *
 * @param temperatureDelta °C pull toward normal on consume (negative = cool when hot)
 */
public record TemperatureStatsTooltip(float temperatureDelta) implements TooltipComponent {}
