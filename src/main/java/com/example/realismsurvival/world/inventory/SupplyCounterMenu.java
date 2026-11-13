package com.example.realismsurvival.world.inventory;

import com.example.realismsurvival.compat.shop.SgQShopCurrencyBridge;
import com.example.realismsurvival.compat.shop.SupplyCategory;
import com.example.realismsurvival.compat.shop.SupplyCounterQuote;
import com.example.realismsurvival.compat.shop.SupplyCounterTrade;
import com.example.realismsurvival.compat.shop.SupplyPricePolicy.Reason;
import com.example.realismsurvival.compat.shop.SupplyShopTrade;
import com.example.realismsurvival.environment.ClimateZone;
import com.example.realismsurvival.registry.ModMenus;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

/**
 * Sell counter on a 54-slot chest. Inner 3×3 slots are real and temporary; the rest are display icons
 * (back, gold quote, confirm). Leftovers return to the player on close.
 */
public class SupplyCounterMenu extends AbstractContainerMenu {

    public static final int CHEST_SLOTS = 54;
    public static final int COUNTER_SLOTS = 9;
    public static final int BUTTON_CONFIRM = 0;
    public static final int BUTTON_BACK = 1;
    public static final int NAV_BACK = 45;
    public static final int NAV_GOLD = 49;
    public static final int NAV_CONFIRM = 53;
    /** Centered 3×3 on rows 1–3, columns 3–5 of a double chest. */
    public static final int[] SELL_SLOTS = {12, 13, 14, 21, 22, 23, 30, 31, 32};
    private static final int PLAYER_SLOTS_END = CHEST_SLOTS + 36;
    private static final double MAX_MERCHANT_DISTANCE_SQR = 8.0 * 8.0;

    // Data slots travel as shorts: per counter slot {accepted, reason, unitCents lo, unitCents hi, offSpecialty}, then total lo/hi.
    private static final int PER_SLOT = 5;
    private static final int TOTAL_INDEX = COUNTER_SLOTS * PER_SLOT;
    private static final int DATA_COUNT = TOTAL_INDEX + 2;

    private final SimpleContainer chest;
    private final SimpleContainer counter;
    private final int[] data = new int[DATA_COUNT];
    private final @Nullable ServerPlayer owner;
    private final @Nullable Entity merchant;
    private final ClimateZone climate;
    private final SupplyCategory category;

    public SupplyCounterMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null, null, ClimateZone.TEMPERATE, SupplyCategory.FOOD);
    }

    public SupplyCounterMenu(
            int containerId,
            Inventory playerInventory,
            @Nullable ServerPlayer owner,
            @Nullable Entity merchant,
            ClimateZone climate,
            SupplyCategory category) {
        super(ModMenus.SUPPLY_COUNTER.get(), containerId);
        this.owner = owner;
        this.merchant = merchant;
        this.climate = climate;
        this.category = category;
        this.chest = new SimpleContainer(CHEST_SLOTS);
        this.counter = new SimpleContainer(COUNTER_SLOTS) {
            @Override
            public void setChanged() {
                super.setChanged();
                slotsChanged(this);
            }
        };
        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 9; col++) {
                int index = col + row * 9;
                int x = 8 + col * 18;
                int y = 18 + row * 18;
                int sell = sellIndex(index);
                if (sell >= 0) {
                    addSlot(new Slot(counter, sell, x, y));
                } else {
                    addSlot(new DisplaySlot(chest, index, x, y));
                }
            }
        }
        addStandardInventorySlots(playerInventory, 8, 139);
        for (int i = 0; i < DATA_COUNT; i++) {
            addDataSlot(DataSlot.shared(data, i));
        }
        if (owner != null) {
            fillDecor();
        }
    }

    public int sellIndex(int menuSlot) {
        for (int i = 0; i < SELL_SLOTS.length; i++) {
            if (SELL_SLOTS[i] == menuSlot) {
                return i;
            }
        }
        return -1;
    }

    public Slot counterSlot(int index) {
        return slots.get(SELL_SLOTS[index]);
    }

    private void fillDecor() {
        for (int i = 0; i < CHEST_SLOTS; i++) {
            if (sellIndex(i) < 0) {
                chest.setItem(i, DisplaySlot.pane());
            }
        }
        chest.setItem(
                NAV_BACK,
                DisplaySlot.named(
                        new ItemStack(Items.ARROW),
                        Component.translatable("gui.realismsurvival.shop.back").withStyle(ChatFormatting.YELLOW),
                        List.of()));
        updateQuoteItems();
    }

    private void updateQuoteItems() {
        int gold = owner == null ? 0 : (int) Math.floor(SgQShopCurrencyBridge.getBalance(owner));
        int sold = 0;
        int refused = 0;
        for (int i = 0; i < COUNTER_SLOTS; i++) {
            sold += accepted(i);
            Reason reason = reason(i);
            if (counter.getItem(i).isEmpty()) {
                continue;
            }
            if (reason != Reason.OK && reason != Reason.PARTIAL) {
                refused++;
            }
        }
        chest.setItem(
                NAV_GOLD,
                DisplaySlot.named(
                        new ItemStack(Items.GOLD_NUGGET),
                        Component.translatable("gui.realismsurvival.shop.balance", gold).withStyle(ChatFormatting.GOLD),
                        List.of(
                                Component.translatable("gui.realismsurvival.supply_counter.total", totalCoins())
                                        .withStyle(ChatFormatting.GREEN),
                                Component.translatable("gui.realismsurvival.supply_counter.counts", sold, refused)
                                        .withStyle(ChatFormatting.GRAY),
                                Component.translatable("gui.realismsurvival.shop.sell_place")
                                        .withStyle(ChatFormatting.DARK_GRAY))));
        chest.setItem(
                NAV_CONFIRM,
                DisplaySlot.named(
                        new ItemStack(Items.EMERALD),
                        Component.translatable("gui.realismsurvival.supply_counter.confirm")
                                .withStyle(ChatFormatting.GREEN),
                        List.of(Component.translatable("gui.realismsurvival.shop.click_sell")
                                .withStyle(ChatFormatting.YELLOW))));
        chest.setChanged();
        broadcastChanges();
    }

    @Override
    public void slotsChanged(net.minecraft.world.Container container) {
        super.slotsChanged(container);
        if (owner != null && container == counter) {
            writeQuote(SupplyCounterTrade.quote(owner, counter, climate, category));
        }
    }

    private void writeQuote(SupplyCounterQuote quote) {
        for (int i = 0; i < COUNTER_SLOTS; i++) {
            SupplyCounterQuote.Slot slot = quote.slots().get(i);
            int base = i * PER_SLOT;
            data[base] = slot.accepted();
            data[base + 1] = slot.reason().ordinal();
            data[base + 2] = slot.unitCents() & 0xFFFF;
            data[base + 3] = (slot.unitCents() >>> 16) & 0xFFFF;
            data[base + 4] = slot.offSpecialty() ? 1 : 0;
        }
        int total = (int) Math.min(Integer.MAX_VALUE, quote.totalCoins());
        data[TOTAL_INDEX] = total & 0xFFFF;
        data[TOTAL_INDEX + 1] = (total >>> 16) & 0xFFFF;
        updateQuoteItems();
    }

    public int accepted(int slot) {
        return data[slot * PER_SLOT];
    }

    public Reason reason(int slot) {
        Reason[] values = Reason.values();
        int ordinal = data[slot * PER_SLOT + 1];
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : Reason.EMPTY;
    }

    public int unitCents(int slot) {
        int base = slot * PER_SLOT;
        return (data[base + 2] & 0xFFFF) | ((data[base + 3] & 0xFFFF) << 16);
    }

    public int totalCoins() {
        return (data[TOTAL_INDEX] & 0xFFFF) | ((data[TOTAL_INDEX + 1] & 0xFFFF) << 16);
    }

    public boolean offSpecialty(int slot) {
        return data[slot * PER_SLOT + 4] != 0;
    }

    @Override
    public void clicked(int slotIndex, int button, ContainerInput input, Player player) {
        if (slotIndex >= 0 && slotIndex < CHEST_SLOTS && sellIndex(slotIndex) < 0) {
            if (owner != null && player == owner && !player.level().isClientSide()) {
                if (slotIndex == NAV_BACK) {
                    SupplyShopTrade.openHub(owner, merchant);
                } else if (slotIndex == NAV_CONFIRM) {
                    SupplyCounterTrade.confirm(owner, counter, climate, category);
                    writeQuote(SupplyCounterTrade.quote(owner, counter, climate, category));
                }
            }
            return;
        }
        super.clicked(slotIndex, button, input, player);
    }

    @Override
    public boolean canDragTo(Slot slot) {
        return sellIndex(slot.index) >= 0 || slot.index >= CHEST_SLOTS;
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (owner == null || player != owner) {
            return false;
        }
        if (buttonId == BUTTON_BACK) {
            SupplyShopTrade.openHub(owner, merchant);
            return true;
        }
        if (buttonId != BUTTON_CONFIRM) {
            return false;
        }
        SupplyCounterTrade.confirm(owner, counter, climate, category);
        writeQuote(SupplyCounterTrade.quote(owner, counter, climate, category));
        return true;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (owner != null) {
            clearContainer(player, counter);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        if (merchant == null) {
            return owner == null;
        }
        return merchant.isAlive() && player.distanceToSqr(merchant) <= MAX_MERCHANT_DISTANCE_SQR;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (sellIndex(index) >= 0) {
            if (!moveItemStackTo(stack, CHEST_SLOTS, PLAYER_SLOTS_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (index >= CHEST_SLOTS) {
            boolean moved = false;
            for (int sellSlot : SELL_SLOTS) {
                if (stack.isEmpty()) {
                    break;
                }
                if (moveItemStackTo(stack, sellSlot, sellSlot + 1, false)) {
                    moved = true;
                }
            }
            if (!moved) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return original;
    }
}
