package com.example.realismsurvival.compat.shop;

import com.example.realismsurvival.RealismSurvival;
import java.util.Map;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Which stacks a supply merchant will buy, and at what unit rate.
 *
 * <p>Listed supply items use the seasonal catalog + {@link SupplyStock} weights (specialist about
 * two thirds of the sale price; other merchants 85% of that). Unlisted stacks still sell, at a low
 * scrap rate with the same off-specialty discount. Unique stacks (renamed, enchanted, custom data),
 * damaged gear, and {@code #realismsurvival:supply_unsellable} are refused so quest rewards and
 * command items cannot be laundered into coins.
 */
public final class SupplyPricePolicy {

    public static final TagKey<Item> UNSELLABLE =
            TagKey.create(Registries.ITEM, RealismSurvival.id("supply_unsellable"));

    /** Ordinals are sent to the client; append only. */
    public enum Reason {
        EMPTY,
        OK,
        PARTIAL,
        NOT_BOUGHT,
        UNIQUE,
        DAMAGED,
        LIMIT
    }

    /** One buyback catalog line: {@code price} coins for {@code count} items, {@code playerLimit} times per day. */
    public record Rate(int count, int price, int playerLimit) {

        public static Rate of(SupplyStock.Line line) {
            return new Rate(Math.max(1, line.count()), Math.max(0, line.price()), Math.max(1, line.playerLimit()));
        }

        public double unitPrice() {
            return price / (double) count;
        }

        public int dailyUnits() {
            return count * playerLimit;
        }
    }

    private SupplyPricePolicy() {}

    /** Reason a listed item is still refused, or {@link Reason#OK}. */
    public static Reason refusal(ItemStack stack) {
        if (stack.isEmpty()) {
            return Reason.EMPTY;
        }
        if (stack.is(UNSELLABLE)) {
            return Reason.NOT_BOUGHT;
        }
        if (stack.has(DataComponents.CUSTOM_NAME)
                || stack.has(DataComponents.CUSTOM_DATA)
                || stack.isEnchanted()
                || stack.has(DataComponents.STORED_ENCHANTMENTS)) {
            return Reason.UNIQUE;
        }
        if (stack.isDamaged()) {
            return Reason.DAMAGED;
        }
        return Reason.OK;
    }

    /**
     * Catalog rate when listed; otherwise a scrap line. Off-specialty merchants always pay less
     * than a specialist would for the same stack.
     */
    public static Rate rateFor(ItemStack stack, SupplyCategory merchant, Map<String, Rate> listed) {
        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        Rate listedRate = listed.get(itemId);
        if (listedRate != null && listedRate.price() > 0) {
            return listedRate;
        }
        int specialty = scrapSpecialty(stack);
        int price = SupplyStock.weightedBuyback(specialty, SupplyStock.categoryOf(stack), merchant);
        return new Rate(1, price, 64);
    }

    static boolean offSpecialty(ItemStack stack, SupplyCategory merchant) {
        return SupplyStock.categoryOf(stack) != merchant;
    }

    private static int scrapSpecialty(ItemStack stack) {
        int max = stack.getMaxStackSize();
        if (max <= 1) {
            return 4;
        }
        if (max <= 16) {
            return 2;
        }
        return 1;
    }
}
