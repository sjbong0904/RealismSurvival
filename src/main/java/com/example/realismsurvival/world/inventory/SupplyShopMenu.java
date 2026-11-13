package com.example.realismsurvival.world.inventory;

import com.example.realismsurvival.compat.shop.SeasonalSupplyShop;
import com.example.realismsurvival.compat.shop.SeasonalSupplyShop.BuyOffer;
import com.example.realismsurvival.compat.shop.SgQShopCurrencyBridge;
import com.example.realismsurvival.compat.shop.SupplyBuyLedger;
import com.example.realismsurvival.compat.shop.SupplyCategory;
import com.example.realismsurvival.compat.shop.SupplyCounterTrade;
import com.example.realismsurvival.compat.shop.SupplyShopTrade;
import com.example.realismsurvival.environment.ClimateZone;
import com.example.realismsurvival.registry.ModMenus;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

/**
 * Skript-style chest shop. Hub (buy/sell icons) and the buy catalog share this 54-slot chest;
 * the sell counter stays a separate menu with the same chest chrome.
 */
public class SupplyShopMenu extends AbstractContainerMenu {

    public static final int CHEST_SLOTS = 54;
    public static final int HUB_BUY = 20;
    public static final int HUB_SELL = 24;
    public static final int NAV_PREV = 45;
    public static final int NAV_BACK = 48;
    public static final int NAV_GOLD = 49;
    public static final int NAV_NEXT = 53;
    private static final int PAGE_SIZE = 28;
    private static final int[] CATALOG_SLOTS = catalogSlots();
    private static final double MAX_MERCHANT_DISTANCE_SQR = 8.0 * 8.0;

    private final SimpleContainer chest;
    private final @Nullable ServerPlayer owner;
    private final @Nullable Entity merchant;
    private final ClimateZone climate;
    private final SupplyCategory category;
    private final boolean buyMode;
    private int page;

    public SupplyShopMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null, null, ClimateZone.TEMPERATE, SupplyCategory.FOOD, false, 0);
    }

    public static SupplyShopMenu hub(
            int id,
            Inventory inventory,
            ServerPlayer owner,
            Entity merchant,
            ClimateZone climate,
            SupplyCategory category) {
        return new SupplyShopMenu(id, inventory, owner, merchant, climate, category, false, 0);
    }

    public static SupplyShopMenu buy(
            int id,
            Inventory inventory,
            ServerPlayer owner,
            Entity merchant,
            ClimateZone climate,
            SupplyCategory category,
            int page) {
        return new SupplyShopMenu(id, inventory, owner, merchant, climate, category, true, page);
    }

    private SupplyShopMenu(
            int containerId,
            Inventory playerInventory,
            @Nullable ServerPlayer owner,
            @Nullable Entity merchant,
            ClimateZone climate,
            SupplyCategory category,
            boolean buyMode,
            int page) {
        super(ModMenus.SUPPLY_SHOP.get(), containerId);
        this.owner = owner;
        this.merchant = merchant;
        this.climate = climate;
        this.category = category;
        this.buyMode = buyMode;
        this.page = Math.max(0, page);
        this.chest = new SimpleContainer(CHEST_SLOTS);
        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new DisplaySlot(chest, col + row * 9, 8 + col * 18, 18 + row * 18));
            }
        }
        addStandardInventorySlots(playerInventory, 8, 139);
        if (owner != null) {
            rebuild();
        }
    }

    private void rebuild() {
        for (int i = 0; i < CHEST_SLOTS; i++) {
            chest.setItem(i, pane());
        }
        if (buyMode) {
            fillBuy();
        } else {
            fillHub();
        }
        chest.setChanged();
        broadcastChanges();
    }

    private void fillHub() {
        chest.setItem(
                HUB_BUY,
                DisplaySlot.named(
                        new ItemStack(Items.EMERALD),
                        Component.translatable("gui.realismsurvival.shop.buy").withStyle(ChatFormatting.GREEN),
                        List.of(
                                Component.translatable("gui.realismsurvival.shop.buy_hint")
                                        .withStyle(ChatFormatting.GRAY))));
        if (SupplyCounterTrade.available()) {
            chest.setItem(
                    HUB_SELL,
                    DisplaySlot.named(
                            new ItemStack(Items.GOLD_INGOT),
                            Component.translatable("gui.realismsurvival.shop.sell").withStyle(ChatFormatting.GOLD),
                            List.of(
                                    Component.translatable("gui.realismsurvival.shop.sell_hint")
                                            .withStyle(ChatFormatting.GRAY))));
        }
        chest.setItem(NAV_GOLD, goldStack());
    }

    private void fillBuy() {
        List<BuyOffer> offers = buyOffers();
        int pages = Math.max(1, (offers.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        page = Math.min(page, pages - 1);
        int start = page * PAGE_SIZE;
        for (int i = 0; i < PAGE_SIZE; i++) {
            int offerIndex = start + i;
            int slot = CATALOG_SLOTS[i];
            if (offerIndex >= offers.size()) {
                chest.setItem(slot, ItemStack.EMPTY);
                continue;
            }
            chest.setItem(slot, offerStack(offers.get(offerIndex)));
        }
        chest.setItem(
                NAV_BACK,
                DisplaySlot.named(
                        new ItemStack(Items.ARROW),
                        Component.translatable("gui.realismsurvival.shop.back").withStyle(ChatFormatting.YELLOW),
                        List.of()));
        if (page > 0) {
            chest.setItem(
                    NAV_PREV,
                    DisplaySlot.named(
                            new ItemStack(Items.SPECTRAL_ARROW),
                            Component.translatable("gui.realismsurvival.shop.prev").withStyle(ChatFormatting.AQUA),
                            List.of()));
        }
        if (page < pages - 1) {
            chest.setItem(
                    NAV_NEXT,
                    DisplaySlot.named(
                            new ItemStack(Items.MAP),
                            Component.translatable("gui.realismsurvival.shop.next").withStyle(ChatFormatting.AQUA),
                            List.of()));
        }
        chest.setItem(NAV_GOLD, goldStack());
    }

    private ItemStack offerStack(BuyOffer offer) {
        Item item = BuiltInRegistries.ITEM.getValue(offer.itemId());
        if (item == null || item == Items.AIR) {
            return pane();
        }
        ItemStack stack = new ItemStack(item, offer.count());
        int bought = owner == null ? 0 : SupplyBuyLedger.boughtToday(owner, offer.itemId().toString());
        int left = Math.max(0, offer.dailyUnits() - bought);
        List<Component> lore = new ArrayList<>();
        lore.add(Component.translatable("gui.realismsurvival.shop.price", offer.price()).withStyle(ChatFormatting.GOLD));
        lore.add(Component.translatable("gui.realismsurvival.shop.stock", left, offer.dailyUnits())
                .withStyle(left > 0 ? ChatFormatting.GRAY : ChatFormatting.RED));
        lore.add(Component.translatable("gui.realismsurvival.shop.click_buy").withStyle(ChatFormatting.YELLOW));
        lore.add(Component.translatable("gui.realismsurvival.shop.shift_buy").withStyle(ChatFormatting.DARK_GRAY));
        return DisplaySlot.named(stack, stack.getHoverName(), lore);
    }

    private ItemStack goldStack() {
        int gold = owner == null ? 0 : (int) Math.floor(SgQShopCurrencyBridge.getBalance(owner));
        return DisplaySlot.named(
                new ItemStack(Items.GOLD_NUGGET),
                Component.translatable("gui.realismsurvival.shop.balance", gold).withStyle(ChatFormatting.GOLD),
                List.of(Component.translatable("gui.realismsurvival.shop.merchant", category.label())
                        .withStyle(ChatFormatting.GRAY)));
    }

    private static ItemStack pane() {
        return DisplaySlot.pane();
    }

    @Override
    public void clicked(int slotIndex, int button, ContainerInput input, Player player) {
        if (slotIndex >= 0 && slotIndex < CHEST_SLOTS) {
            if (owner != null && player == owner && !player.level().isClientSide()) {
                handleShopClick(slotIndex, input);
            }
            return;
        }
        super.clicked(slotIndex, button, input, player);
    }

    @Override
    public boolean canDragTo(Slot slot) {
        return slot.index >= CHEST_SLOTS;
    }

    private void handleShopClick(int slotIndex, ContainerInput input) {
        if (!buyMode) {
            if (slotIndex == HUB_BUY) {
                SupplyShopTrade.openBuy(owner, merchant);
            } else if (slotIndex == HUB_SELL && SupplyCounterTrade.available()) {
                SupplyCounterTrade.open(owner, merchant);
            }
            return;
        }
        if (slotIndex == NAV_BACK) {
            SupplyShopTrade.openHub(owner, merchant);
            return;
        }
        if (slotIndex == NAV_PREV && page > 0) {
            SupplyShopTrade.openBuyPage(owner, merchant, climate, category, page - 1);
            return;
        }
        if (slotIndex == NAV_NEXT) {
            SupplyShopTrade.openBuyPage(owner, merchant, climate, category, page + 1);
            return;
        }
        int catalogIndex = catalogIndex(slotIndex);
        if (catalogIndex < 0) {
            return;
        }
        List<BuyOffer> offers = buyOffers();
        int offerIndex = page * PAGE_SIZE + catalogIndex;
        if (offerIndex >= offers.size()) {
            return;
        }
        int times = input == ContainerInput.QUICK_MOVE ? 8 : 1;
        if (SupplyShopTrade.purchase(owner, offers.get(offerIndex), times)) {
            rebuild();
        }
    }

    private List<BuyOffer> buyOffers() {
        if (owner == null || !(owner.level() instanceof ServerLevel level)) {
            return List.of();
        }
        return SeasonalSupplyShop.buyOffers(level, climate, category);
    }

    private static int catalogIndex(int slot) {
        for (int i = 0; i < CATALOG_SLOTS.length; i++) {
            if (CATALOG_SLOTS[i] == slot) {
                return i;
            }
        }
        return -1;
    }

    private static int[] catalogSlots() {
        int[] slots = new int[PAGE_SIZE];
        int n = 0;
        for (int row = 1; row <= 4; row++) {
            for (int col = 1; col <= 7; col++) {
                slots[n++] = row * 9 + col;
            }
        }
        return slots;
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
        return ItemStack.EMPTY;
    }
}
