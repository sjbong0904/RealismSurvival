package com.example.realismsurvival.compat.shop;

import com.example.realismsurvival.compat.shop.SupplyPricePolicy.Rate;
import com.example.realismsurvival.compat.shop.SupplyPricePolicy.Reason;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/**
 * Server-side quote for everything on the counter. Computed fresh for display and again on confirm;
 * nothing the client sends is used as a price.
 */
public record SupplyCounterQuote(List<Slot> slots, long totalCoins, Map<String, Integer> soldByItem) {

    /** {@code unitCents} is display-only; the payout uses exact rate math, floored once. */
    public record Slot(int accepted, Reason reason, int unitCents, boolean offSpecialty) {

        public Slot(int accepted, Reason reason, int unitCents) {
            this(accepted, reason, unitCents, false);
        }
    }

    public static SupplyCounterQuote compute(
            ServerPlayer player, Container counter, Map<String, Rate> rates, SupplyCategory merchant) {
        List<Slot> slots = new ArrayList<>(counter.getContainerSize());
        Map<String, Integer> used = new HashMap<>();
        Map<String, Integer> sold = new LinkedHashMap<>();
        double total = 0.0;
        for (int i = 0; i < counter.getContainerSize(); i++) {
            ItemStack stack = counter.getItem(i);
            Reason refusal = SupplyPricePolicy.refusal(stack);
            if (refusal != Reason.OK) {
                slots.add(new Slot(0, refusal, 0));
                continue;
            }
            String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            Rate rate = SupplyPricePolicy.rateFor(stack, merchant, rates);
            if (rate == null || rate.price() <= 0) {
                slots.add(new Slot(0, Reason.NOT_BOUGHT, 0));
                continue;
            }
            boolean offSpecialty = SupplyPricePolicy.offSpecialty(stack, merchant);
            int unitCents = (int) Math.floor(rate.unitPrice() * 100.0);
            int already = used.computeIfAbsent(itemId, id -> SupplyCounterLedger.soldToday(player, id));
            int room = Math.max(0, rate.dailyUnits() - already);
            int accepted = Math.min(stack.getCount(), room);
            if (accepted <= 0) {
                slots.add(new Slot(0, Reason.LIMIT, unitCents, offSpecialty));
                continue;
            }
            used.put(itemId, already + accepted);
            sold.merge(itemId, accepted, Integer::sum);
            total += accepted * rate.unitPrice();
            slots.add(new Slot(
                    accepted,
                    accepted < stack.getCount() ? Reason.PARTIAL : Reason.OK,
                    unitCents,
                    offSpecialty));
        }
        return new SupplyCounterQuote(List.copyOf(slots), (long) Math.floor(total + 1.0E-9), Map.copyOf(sold));
    }
}
