package com.example.realismsurvival.compat.shop;

import com.example.realismsurvival.environment.ClimateZone;
import com.example.realismsurvival.environment.Season;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Specialty sale lists, and buyback prices for every supply item.
 * A specialist pays about two thirds of that item's sale price. Any other merchant pays about 85% of that.
 * Every buyback stays under the sale price in the same climate and season.
 */
public final class SupplyStock {

    public record Line(boolean buy, String item, int count, int price, int playerLimit) {}

    private static final Map<String, SupplyCategory> CATEGORY = new LinkedHashMap<>();

    static {
        food("minecraft:bread");
        food("minecraft:wheat");
        food("minecraft:wheat_seeds");
        food("minecraft:beetroot");
        food("minecraft:beetroot_seeds");
        food("minecraft:bone_meal");
        food("minecraft:apple");
        food("minecraft:baked_potato");
        food("minecraft:potato");
        food("minecraft:carrot");
        food("minecraft:sweet_berries");
        food("minecraft:melon_seeds");
        food("minecraft:melon_slice");
        food("minecraft:pumpkin");
        food("minecraft:pumpkin_seeds");
        food("minecraft:pumpkin_pie");
        food("minecraft:hay_block");
        food("minecraft:cactus");
        food("minecraft:cocoa_beans");
        food("minecraft:sugar_cane");
        food("minecraft:cod");
        food("minecraft:salmon");
        food("minecraft:cooked_beef");
        food("minecraft:rabbit_stew");
        food("farmersdelight:cabbage");
        food("farmersdelight:cabbage_seeds");
        food("farmersdelight:onion");
        food("farmersdelight:rice");
        food("farmersdelight:tomato");
        food("farmersdelight:tomato_seeds");
        food("minecraft:leather");
        food("minecraft:rabbit_hide");
        food("minecraft:white_wool");
        food("minecraft:bone");
        food("minecraft:bamboo");
        food("minecraft:golden_apple");
        food("minecraft:enchanted_golden_apple");

        weapons("minecraft:string");
        weapons("minecraft:arrow");
        weapons("minecraft:bow");
        weapons("minecraft:shield");
        weapons("minecraft:stone_sword");
        weapons("minecraft:iron_sword");
        weapons("minecraft:leather_helmet");
        weapons("minecraft:leather_chestplate");
        weapons("minecraft:leather_leggings");
        weapons("minecraft:leather_boots");
        weapons("minecraft:iron_helmet");
        weapons("minecraft:iron_chestplate");
        weapons("minecraft:iron_leggings");
        weapons("minecraft:iron_boots");
        weapons("minecraft:golden_sword");
        weapons("minecraft:golden_helmet");
        weapons("minecraft:golden_chestplate");
        weapons("minecraft:golden_leggings");
        weapons("minecraft:golden_boots");
        weapons("minecraft:diamond_sword");
        weapons("minecraft:diamond_helmet");
        weapons("minecraft:diamond_chestplate");
        weapons("minecraft:diamond_leggings");
        weapons("minecraft:diamond_boots");
        weapons("minecraft:netherite_sword");
        weapons("minecraft:netherite_helmet");
        weapons("minecraft:netherite_chestplate");
        weapons("minecraft:netherite_leggings");
        weapons("minecraft:netherite_boots");
        weapons("minecraft:torch");
        weapons("minecraft:campfire");

        potions("realismsurvival:salt");
        potions("realismsurvival:filter_cartridge");
        potions("realismsurvival:fresh_water_bottle");
        potions("realismsurvival:distilled_water_bottle");
        potions("minecraft:rotten_flesh");
        potions("minecraft:glass_bottle");
        potions("minecraft:gunpowder");
        potions("minecraft:glowstone_dust");
        potions("minecraft:spider_eye");
        potions("minecraft:nether_wart");
        potions("minecraft:blaze_powder");
        potions("minecraft:magma_cream");
        potions("minecraft:golden_carrot");
        potions("minecraft:fermented_spider_eye");
        potions("minecraft:phantom_membrane");
        potions("minecraft:ghast_tear");
        potions("minecraft:glistering_melon_slice");
        potions("minecraft:ice");
        potions("minecraft:dragon_breath");

        minerals("minecraft:coal");
        minerals("minecraft:charcoal");
        minerals("minecraft:raw_copper");
        minerals("minecraft:raw_iron");
        minerals("minecraft:raw_gold");
        minerals("minecraft:copper_ingot");
        minerals("minecraft:diamond");
        minerals("minecraft:netherite_scrap");
    }

    private SupplyStock() {}

    @Nullable
    public static SupplyCategory categoryOf(String item) {
        return CATEGORY.get(item);
    }

    /**
     * Mapped supply items first; otherwise a coarse tag guess so off-specialty discounts still apply
     * to unlisted stacks. Returns {@code null} when the item has no shop category.
     */
    @Nullable
    public static SupplyCategory categoryOf(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        SupplyCategory mapped = categoryOf(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        if (mapped != null) {
            return mapped;
        }
        if (stack.has(DataComponents.FOOD)
                || stack.is(ItemTags.MEAT)
                || stack.is(ItemTags.FISHES)
                || stack.is(ItemTags.VILLAGER_PLANTABLE_SEEDS)
                || stack.is(ItemTags.WOOL)
                || stack.is(ItemTags.SAPLINGS)
                || stack.is(ItemTags.FLOWERS)) {
            return SupplyCategory.FOOD;
        }
        if (stack.is(ItemTags.SWORDS)
                || stack.is(ItemTags.AXES)
                || stack.is(ItemTags.PICKAXES)
                || stack.is(ItemTags.SHOVELS)
                || stack.is(ItemTags.HOES)
                || stack.is(ItemTags.SPEARS)
                || stack.is(ItemTags.ARROWS)
                || stack.is(ItemTags.HEAD_ARMOR)
                || stack.is(ItemTags.CHEST_ARMOR)
                || stack.is(ItemTags.LEG_ARMOR)
                || stack.is(ItemTags.FOOT_ARMOR)
                || stack.is(ItemTags.WEAPON_ENCHANTABLE)
                || stack.is(ItemTags.BOW_ENCHANTABLE)
                || stack.is(ItemTags.CROSSBOW_ENCHANTABLE)) {
            return SupplyCategory.WEAPONS;
        }
        if (stack.has(DataComponents.POTION_CONTENTS) || stack.is(ItemTags.BREWING_FUEL)) {
            return SupplyCategory.POTIONS;
        }
        if (stack.is(ItemTags.COALS)
                || stack.is(ItemTags.BEACON_PAYMENT_ITEMS)
                || stack.is(ItemTags.TRIM_MATERIALS)
                || stack.is(ItemTags.METAL_NUGGETS)
                || stack.is(ItemTags.STONE_CRAFTING_MATERIALS)
                || stack.is(ItemTags.COAL_ORES)
                || stack.is(ItemTags.COPPER_ORES)
                || stack.is(ItemTags.IRON_ORES)
                || stack.is(ItemTags.GOLD_ORES)
                || stack.is(ItemTags.REDSTONE_ORES)
                || stack.is(ItemTags.LAPIS_ORES)
                || stack.is(ItemTags.EMERALD_ORES)
                || stack.is(ItemTags.DIAMOND_ORES)) {
            return SupplyCategory.MINERALS;
        }
        return null;
    }

    /** Specialist keeps {@code specialty}; any other (or uncategorized) merchant pays 85% of that. */
    public static int weightedBuyback(int specialty, @Nullable SupplyCategory itemCategory, SupplyCategory merchant) {
        int price = Math.max(1, specialty);
        if (itemCategory == merchant) {
            return price;
        }
        int reduced = price * 85 / 100;
        if (reduced >= price) {
            reduced = price > 1 ? price - 1 : price;
        }
        return Math.max(1, reduced);
    }

    /**
     * Gear, brewing ingredients, and ore the seasonal catalog does not already list.
     * Prices move with climate and season. Higher bands appear only after {@code era}.
     * Gold ingots are not stock: they are crafting metal, and the wallet is coins.
     */
    public static List<Line> extraSales(ClimateZone zone, Season season, SupplyEra era) {
        ClimateZone climate = zone == ClimateZone.OCEAN ? ClimateZone.TEMPERATE : zone;
        List<Line> lines = new ArrayList<>();
        int weaponSeason = seasonPercent(season, 100, 90, 110, 125);
        int potionSeason = seasonPercent(season, 100, 120, 105, 95);
        int mineralSeason = seasonPercent(season, 100, 90, 110, 130);
        int foodSeason = seasonPercent(season, 100, 90, 110, 130);

        offer(lines, era, SupplyEra.PLAIN, "minecraft:arrow", 16, scaled(32, climate, weaponSeason), 4);
        offer(lines, era, SupplyEra.PLAIN, "minecraft:bow", 1, scaled(96, climate, weaponSeason), 1);
        offer(lines, era, SupplyEra.PLAIN, "minecraft:shield", 1, scaled(140, climate, weaponSeason), 1);
        offer(lines, era, SupplyEra.PLAIN, "minecraft:stone_sword", 1, scaled(48, climate, weaponSeason), 2);
        offer(lines, era, SupplyEra.PLAIN, "minecraft:leather_helmet", 1, scaled(160, climate, weaponSeason), 1);
        offer(lines, era, SupplyEra.PLAIN, "minecraft:leather_chestplate", 1, scaled(280, climate, weaponSeason), 1);
        offer(lines, era, SupplyEra.PLAIN, "minecraft:leather_leggings", 1, scaled(240, climate, weaponSeason), 1);
        offer(lines, era, SupplyEra.PLAIN, "minecraft:leather_boots", 1, scaled(140, climate, weaponSeason), 1);
        offer(lines, era, SupplyEra.IRON, "minecraft:iron_sword", 1, scaled(480, climate, weaponSeason), 1);
        offer(lines, era, SupplyEra.IRON, "minecraft:iron_helmet", 1, scaled(720, climate, weaponSeason), 1);
        offer(lines, era, SupplyEra.IRON, "minecraft:iron_chestplate", 1, scaled(1200, climate, weaponSeason), 1);
        offer(lines, era, SupplyEra.IRON, "minecraft:iron_leggings", 1, scaled(1000, climate, weaponSeason), 1);
        offer(lines, era, SupplyEra.IRON, "minecraft:iron_boots", 1, scaled(560, climate, weaponSeason), 1);
        offer(lines, era, SupplyEra.GOLD, "minecraft:golden_sword", 1, scaled(900, climate, weaponSeason), 1);
        offer(lines, era, SupplyEra.GOLD, "minecraft:golden_helmet", 1, scaled(1100, climate, weaponSeason), 1);
        offer(lines, era, SupplyEra.GOLD, "minecraft:golden_chestplate", 1, scaled(1800, climate, weaponSeason), 1);
        offer(lines, era, SupplyEra.GOLD, "minecraft:golden_leggings", 1, scaled(1500, climate, weaponSeason), 1);
        offer(lines, era, SupplyEra.GOLD, "minecraft:golden_boots", 1, scaled(860, climate, weaponSeason), 1);
        offer(lines, era, SupplyEra.DIAMOND, "minecraft:diamond_sword", 1, scaled(2200, climate, weaponSeason), 1);
        offer(lines, era, SupplyEra.DIAMOND, "minecraft:diamond_helmet", 1, scaled(2800, climate, weaponSeason), 1);
        offer(lines, era, SupplyEra.DIAMOND, "minecraft:diamond_chestplate", 1, scaled(4600, climate, weaponSeason), 1);
        offer(lines, era, SupplyEra.DIAMOND, "minecraft:diamond_leggings", 1, scaled(3800, climate, weaponSeason), 1);
        offer(lines, era, SupplyEra.DIAMOND, "minecraft:diamond_boots", 1, scaled(2200, climate, weaponSeason), 1);
        offer(lines, era, SupplyEra.NETHERITE, "minecraft:netherite_sword", 1, scaled(5200, climate, weaponSeason), 1);
        offer(lines, era, SupplyEra.NETHERITE, "minecraft:netherite_helmet", 1, scaled(6400, climate, weaponSeason), 1);
        offer(lines, era, SupplyEra.NETHERITE, "minecraft:netherite_chestplate", 1, scaled(9800, climate, weaponSeason), 1);
        offer(lines, era, SupplyEra.NETHERITE, "minecraft:netherite_leggings", 1, scaled(8200, climate, weaponSeason), 1);
        offer(lines, era, SupplyEra.NETHERITE, "minecraft:netherite_boots", 1, scaled(4800, climate, weaponSeason), 1);

        offer(lines, era, SupplyEra.IRON, "minecraft:cooked_beef", 4, scaled(480, climate, foodSeason), 2);
        offer(lines, era, SupplyEra.GOLD, "minecraft:golden_apple", 1, scaled(960, climate, foodSeason), 1);
        offer(lines, era, SupplyEra.NETHERITE, "minecraft:enchanted_golden_apple", 1, scaled(4800, climate, foodSeason), 1);

        offer(lines, era, SupplyEra.PLAIN, "minecraft:glass_bottle", 8, scaled(16, climate, potionSeason), 4);
        offer(lines, era, SupplyEra.PLAIN, "minecraft:spider_eye", 4, scaled(36, climate, potionSeason), 3);
        offer(lines, era, SupplyEra.IRON, "minecraft:gunpowder", 8, scaled(48, climate, potionSeason), 3);
        offer(lines, era, SupplyEra.IRON, "minecraft:glowstone_dust", 8, scaled(64, climate, potionSeason), 2);
        if (season == Season.SUMMER || season == Season.AUTUMN) {
            offer(lines, era, SupplyEra.IRON, "minecraft:fermented_spider_eye", 2, scaled(90, climate, potionSeason), 2);
        }
        offer(lines, era, SupplyEra.GOLD, "minecraft:nether_wart", 4, scaled(120, climate, potionSeason), 2);
        offer(lines, era, SupplyEra.GOLD, "minecraft:golden_carrot", 2, scaled(180, climate, potionSeason), 1);
        if (season == Season.SPRING || season == Season.SUMMER) {
            offer(lines, era, SupplyEra.GOLD, "minecraft:glistering_melon_slice", 2, scaled(80, climate, potionSeason), 2);
        }
        offer(lines, era, SupplyEra.DIAMOND, "minecraft:blaze_powder", 2, scaled(160, climate, potionSeason), 1);
        offer(lines, era, SupplyEra.DIAMOND, "minecraft:magma_cream", 2, scaled(140, climate, potionSeason), 1);
        if (season == Season.AUTUMN || season == Season.WINTER) {
            offer(lines, era, SupplyEra.DIAMOND, "minecraft:phantom_membrane", 1, scaled(200, climate, potionSeason), 1);
        }
        if (season == Season.WINTER) {
            offer(lines, era, SupplyEra.DIAMOND, "minecraft:ghast_tear", 1, scaled(420, climate, potionSeason), 1);
        }
        offer(lines, era, SupplyEra.NETHERITE, "minecraft:dragon_breath", 1, scaled(2400, climate, potionSeason), 1);

        offer(lines, era, SupplyEra.PLAIN, "minecraft:raw_copper", 8, scaled(96, climate, mineralSeason), 2);
        offer(lines, era, SupplyEra.PLAIN, "minecraft:copper_ingot", 4, scaled(140, climate, mineralSeason), 2);
        offer(lines, era, SupplyEra.IRON, "minecraft:raw_iron", 4, scaled(220, climate, mineralSeason), 1);
        offer(lines, era, SupplyEra.GOLD, "minecraft:raw_gold", 2, scaled(360, climate, mineralSeason), 1);
        offer(lines, era, SupplyEra.DIAMOND, "minecraft:diamond", 1, scaled(1600, climate, mineralSeason), 1);
        offer(lines, era, SupplyEra.NETHERITE, "minecraft:netherite_scrap", 1, scaled(4000, climate, mineralSeason), 1);
        return lines;
    }

    /**
     * Sales for {@code category} only, plus a buyback line for every supply item.
     * Every merchant buys the full list. Specialist buyback is two thirds of the sale stack;
     * other merchants pay 85% of that. Unlisted stacks are priced later as scrap.
     */
    public static List<Line> forMerchant(
            List<Line> catalog, ClimateZone zone, Season season, SupplyCategory category, SupplyEra era) {
        Map<String, Line> sales = new LinkedHashMap<>();
        Map<String, Line> catalogBuys = new LinkedHashMap<>();
        for (Line line : catalog) {
            if (line.buy()) {
                sales.putIfAbsent(line.item(), line);
            } else {
                catalogBuys.putIfAbsent(line.item(), line);
            }
        }
        for (Line extra : extraSales(zone, season, era)) {
            sales.putIfAbsent(extra.item(), extra);
        }
        Map<String, Line> buybackSales = new LinkedHashMap<>(sales);
        for (Line extra : extraSales(zone, season, SupplyEra.NETHERITE)) {
            buybackSales.putIfAbsent(extra.item(), extra);
        }

        List<Line> out = new ArrayList<>();
        for (Line sale : sales.values()) {
            if (categoryOf(sale.item()) == category) {
                out.add(sale);
            }
        }

        Set<String> items = new LinkedHashSet<>();
        items.addAll(buybackSales.keySet());
        items.addAll(catalogBuys.keySet());
        for (String item : items) {
            Line sale = buybackSales.get(item);
            Line catalogBuy = catalogBuys.get(item);
            int count;
            int limit;
            int specialty;
            if (sale != null && sale.price() >= 2) {
                count = sale.count();
                limit = sale.playerLimit();
                specialty = sale.price() * 2 / 3;
                if (specialty >= sale.price()) {
                    specialty = sale.price() - 1;
                }
                specialty = Math.max(1, specialty);
            } else if (catalogBuy != null) {
                count = catalogBuy.count();
                limit = catalogBuy.playerLimit();
                specialty = Math.max(1, catalogBuy.price());
            } else {
                continue;
            }
            int price = weightedBuyback(specialty, categoryOf(item), category);
            if (sale != null && sale.count() > 0 && (long) price * sale.count() >= (long) sale.price() * count) {
                price = Math.max(1, (int) (((long) sale.price() * count) / sale.count()) - 1);
            }
            out.add(new Line(false, item, count, Math.max(1, price), Math.max(1, limit)));
        }
        return out;
    }

    private static void food(String item) {
        CATEGORY.put(item, SupplyCategory.FOOD);
    }

    private static void weapons(String item) {
        CATEGORY.put(item, SupplyCategory.WEAPONS);
    }

    private static void potions(String item) {
        CATEGORY.put(item, SupplyCategory.POTIONS);
    }

    private static void minerals(String item) {
        CATEGORY.put(item, SupplyCategory.MINERALS);
    }

    private static void offer(List<Line> lines, SupplyEra era, SupplyEra min, String item, int count, int price, int limit) {
        if (era.atLeast(min)) {
            lines.add(sale(item, count, price, limit));
        }
    }

    private static Line sale(String item, int count, int price, int limit) {
        return new Line(true, item, count, Math.max(2, price), limit);
    }

    private static int scaled(int base, ClimateZone zone, int seasonPercent) {
        int climate = switch (zone) {
            case ARID -> 125;
            case COLD -> 115;
            case TROPICAL -> 90;
            default -> 100;
        };
        return Math.max(2, base * climate * seasonPercent / 10_000);
    }

    private static int seasonPercent(Season season, int spring, int summer, int autumn, int winter) {
        return switch (season) {
            case SUMMER -> summer;
            case AUTUMN -> autumn;
            case WINTER -> winter;
            default -> spring;
        };
    }
}
