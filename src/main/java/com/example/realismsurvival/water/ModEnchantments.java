package com.example.realismsurvival.water;

import com.example.realismsurvival.RealismSurvival;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * Glass-bottle temperature enchantments (spec §9.10). Definitions live in datapack JSON;
 * this class only holds keys and the drink-time temperature deltas.
 */
public final class ModEnchantments {

    private ModEnchantments() {}

    public static final ResourceKey<Enchantment> WARMING =
            ResourceKey.create(Registries.ENCHANTMENT, RealismSurvival.id("warming"));
    public static final ResourceKey<Enchantment> COOLING =
            ResourceKey.create(Registries.ENCHANTMENT, RealismSurvival.id("cooling"));

    /** °C pull toward normal when drinking from a warming-enchanted bottle. */
    public static final float WARMING_TEMPERATURE_DELTA = 0.75F;
    /** °C pull toward normal when drinking from a cooling-enchanted bottle. */
    public static final float COOLING_TEMPERATURE_DELTA = -0.75F;

    public static float drinkTemperatureDelta(ItemStack stack, HolderLookup.Provider registries) {
        HolderLookup.RegistryLookup<Enchantment> lookup = registries.lookupOrThrow(Registries.ENCHANTMENT);
        if (stack.getEnchantmentLevel(holder(lookup, WARMING)) > 0) {
            return WARMING_TEMPERATURE_DELTA;
        }
        if (stack.getEnchantmentLevel(holder(lookup, COOLING)) > 0) {
            return COOLING_TEMPERATURE_DELTA;
        }
        return 0.0F;
    }

    public static boolean hasWarming(ItemStack stack, HolderLookup.Provider registries) {
        return stack.getEnchantmentLevel(holder(registries.lookupOrThrow(Registries.ENCHANTMENT), WARMING)) > 0;
    }

    public static boolean hasCooling(ItemStack stack, HolderLookup.Provider registries) {
        return stack.getEnchantmentLevel(holder(registries.lookupOrThrow(Registries.ENCHANTMENT), COOLING)) > 0;
    }

    private static Holder<Enchantment> holder(HolderLookup.RegistryLookup<Enchantment> lookup, ResourceKey<Enchantment> key) {
        return lookup.getOrThrow(key);
    }
}
