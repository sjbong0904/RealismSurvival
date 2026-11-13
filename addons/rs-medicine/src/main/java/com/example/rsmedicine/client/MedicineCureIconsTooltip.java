package com.example.rsmedicine.client;

import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

/** Row of mob-effect icons cleared by a medicine item. */
public record MedicineCureIconsTooltip(List<Holder<MobEffect>> effects) implements TooltipComponent {}
