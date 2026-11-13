package com.example.realismsurvival.world.inventory;

import com.example.realismsurvival.api.ModItemTags;
import com.example.realismsurvival.blockentity.WaterStillBlockEntity;
import com.example.realismsurvival.registry.ModMenus;
import com.example.realismsurvival.water.BucketFill;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

public class WaterStillMenu extends AbstractContainerMenu {

    private static final int MACHINE_SLOTS = 5;
    private static final int PLAYER_SLOTS_END = 41;

    private final Container container;
    private final ContainerData data;
    private final Level level;

    public WaterStillMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(MACHINE_SLOTS), new SimpleContainerData(4));
    }

    public WaterStillMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(ModMenus.WATER_STILL.get(), containerId);
        checkContainerSize(container, MACHINE_SLOTS);
        checkContainerDataCount(data, 4);
        this.container = container;
        this.data = data;
        this.level = playerInventory.player.level();

        addSlot(new Slot(container, WaterStillBlockEntity.SLOT_FUEL, 56, 53) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return isFuel(stack);
            }
        }.setBackground(MachineSlotIcons.COAL));
        addSlot(new Slot(container, WaterStillBlockEntity.SLOT_WATER, 56, 17) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItemTags.STILL_INPUT_BUCKETS) && BucketFill.getFill(stack) > 0;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        }.setBackground(MachineSlotIcons.WATER_BUCKET));
        addSlot(new Slot(container, WaterStillBlockEntity.SLOT_EMPTY_CONTAINER, 26, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.BUCKET) || stack.is(Items.GLASS_BOTTLE);
            }

            @Override
            public int getMaxStackSize(ItemStack stack) {
                return stack.is(Items.BUCKET) ? 1 : super.getMaxStackSize(stack);
            }
        }.setBackground(MachineSlotIcons.GLASS_BOTTLE));
        addSlot(new Slot(container, WaterStillBlockEntity.SLOT_DISTILLED, MachineSlotIcons.OUT_LEFT_X, MachineSlotIcons.OUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        }.setBackground(MachineSlotIcons.WATER_BOTTLE));
        addSlot(new Slot(container, WaterStillBlockEntity.SLOT_SALT, MachineSlotIcons.OUT_RIGHT_X, MachineSlotIcons.OUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        }.setBackground(MachineSlotIcons.SALT));
        addStandardInventorySlots(playerInventory, 8, 84);
        addDataSlots(data);
    }

    private boolean isFuel(ItemStack stack) {
        return stack.getBurnTime(RecipeType.SMELTING, level.fuelValues()) > 0;
    }

    public float getBurnProgress() {
        int total = data.get(WaterStillBlockEntity.DATA_COOK_TOTAL);
        int current = data.get(WaterStillBlockEntity.DATA_COOK_PROGRESS);
        return total > 0 && current > 0 ? Mth.clamp((float) current / total, 0.0F, 1.0F) : 0.0F;
    }

    public float getLitProgress() {
        int duration = data.get(WaterStillBlockEntity.DATA_LIT_DURATION);
        if (duration == 0) {
            duration = 200;
        }
        return Mth.clamp((float) data.get(WaterStillBlockEntity.DATA_LIT_TIME) / duration, 0.0F, 1.0F);
    }

    public boolean isLit() {
        return data.get(WaterStillBlockEntity.DATA_LIT_TIME) > 0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack original = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        original = stack.copy();

        if (index < MACHINE_SLOTS) {
            // Machine → player (never re-route into another machine slot)
            if (!moveItemStackTo(stack, MACHINE_SLOTS, PLAYER_SLOTS_END, true)) {
                return ItemStack.EMPTY;
            }
            if (index == WaterStillBlockEntity.SLOT_DISTILLED || index == WaterStillBlockEntity.SLOT_SALT) {
                slot.onQuickCraft(stack, original);
            }
        } else {
            if (stack.is(ModItemTags.STILL_INPUT_BUCKETS) && BucketFill.getFill(stack) > 0) {
                if (!moveItemStackTo(stack, WaterStillBlockEntity.SLOT_WATER, WaterStillBlockEntity.SLOT_WATER + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (stack.is(Items.GLASS_BOTTLE)) {
                if (!moveItemStackTo(
                        stack,
                        WaterStillBlockEntity.SLOT_EMPTY_CONTAINER,
                        WaterStillBlockEntity.SLOT_EMPTY_CONTAINER + 1,
                        false
                )) {
                    return ItemStack.EMPTY;
                }
            } else if (stack.is(Items.BUCKET)) {
                if (!moveItemStackTo(
                        stack,
                        WaterStillBlockEntity.SLOT_EMPTY_CONTAINER,
                        WaterStillBlockEntity.SLOT_EMPTY_CONTAINER + 1,
                        false
                )) {
                    return ItemStack.EMPTY;
                }
            } else if (isFuel(stack)) {
                if (!moveItemStackTo(stack, WaterStillBlockEntity.SLOT_FUEL, WaterStillBlockEntity.SLOT_FUEL + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                return ItemStack.EMPTY;
            }
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

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }
}
