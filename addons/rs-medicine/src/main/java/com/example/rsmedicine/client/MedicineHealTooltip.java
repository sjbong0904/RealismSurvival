package com.example.rsmedicine.client;

import net.minecraft.world.inventory.tooltip.TooltipComponent;

/** Heal amount in half-hearts (same units as {@link net.minecraft.world.entity.LivingEntity#heal}). */
public record MedicineHealTooltip(float healAmount) implements TooltipComponent {}
