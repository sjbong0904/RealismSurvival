package com.example.realismsurvival.world.inventory;

import com.example.realismsurvival.api.ModItemTags;
import com.example.realismsurvival.blockentity.WaterFilterBlockEntity;
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

public class WaterFilterMenu extends AbstractContainerMenu {

    private static final int MACHINE_SLOTS = 5;
    private static final int PLAYER_SLOTS_END = 41;

    private final Container container;
    private final ContainerData data;

    public WaterFilterMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(MACHINE_SLOTS), new SimpleContainerData(2));
    }

    public WaterFilterMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(ModMenus.WATER_FILTER.get(), containerId);
        checkContainerSize(container, MACHINE_SLOTS);
        checkContainerDataCount(data, 2);
        this.container = container;
        this.data = data;

        // Menu indices match container indices (0..4) for quickMoveStack.
        addSlot(new Slot(container, WaterFilterBlockEntity.SLOT_INPUT_BUCKET, 56, 17) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItemTags.FILTER_INPUT_BUCKETS) && BucketFill.getFill(stack) > 0;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        }.setBackground(MachineSlotIcons.WATER_BUCKET));
        addSlot(new Slot(container, WaterFilterBlockEntity.SLOT_BOTTLE, 56, 53) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.GLASS_BOTTLE);
            }
        }.setBackground(MachineSlotIcons.GLASS_BOTTLE));
        addSlot(new Slot(container, WaterFilterBlockEntity.SLOT_RESULT_BOTTLE, MachineSlotIcons.OUT_LEFT_X, MachineSlotIcons.OUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        }.setBackground(MachineSlotIcons.WATER_BOTTLE));
        addSlot(new Slot(container, WaterFilterBlockEntity.SLOT_EMPTY_BUCKET, MachineSlotIcons.OUT_RIGHT_X, MachineSlotIcons.OUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        }.setBackground(MachineSlotIcons.BUCKET));
        addSlot(new Slot(container, WaterFilterBlockEntity.SLOT_CARTRIDGE, 56, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItemTags.FILTER_CARTRIDGES);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        }.setBackground(MachineSlotIcons.FILTER_CARTRIDGE));
        addStandardInventorySlots(playerInventory, 8, 84);
        addDataSlots(data);
    }

    public float getProgress() {
        int total = data.get(WaterFilterBlockEntity.DATA_TOTAL);
        int current = data.get(WaterFilterBlockEntity.DATA_PROGRESS);
        return total > 0 ? Mth.clamp((float) current / total, 0.0F, 1.0F) : 0.0F;
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
            if (!moveItemStackTo(stack, MACHINE_SLOTS, PLAYER_SLOTS_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.is(ModItemTags.FILTER_INPUT_BUCKETS) && BucketFill.getFill(stack) > 0) {
            if (!moveItemStackTo(
                    stack,
                    WaterFilterBlockEntity.SLOT_INPUT_BUCKET,
                    WaterFilterBlockEntity.SLOT_INPUT_BUCKET + 1,
                    false
            )) {
                return ItemStack.EMPTY;
            }
        } else if (stack.is(ModItemTags.FILTER_CARTRIDGES)) {
            if (!moveItemStackTo(
                    stack,
                    WaterFilterBlockEntity.SLOT_CARTRIDGE,
                    WaterFilterBlockEntity.SLOT_CARTRIDGE + 1,
                    false
            )) {
                return ItemStack.EMPTY;
            }
        } else if (stack.is(Items.GLASS_BOTTLE)) {
            if (!moveItemStackTo(
                    stack,
                    WaterFilterBlockEntity.SLOT_BOTTLE,
                    WaterFilterBlockEntity.SLOT_BOTTLE + 1,
                    false
            )) {
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

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }
}
