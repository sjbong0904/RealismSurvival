package com.example.realismsurvival.blockentity;

import com.example.realismsurvival.RealismSurvivalConfig;
import com.example.realismsurvival.api.ModItemTags;
import com.example.realismsurvival.api.water.WaterApi;
import com.example.realismsurvival.block.WaterFilterBlock;
import com.example.realismsurvival.registry.ModBlockEntities;
import com.example.realismsurvival.registry.ModWaterQualities;
import com.example.realismsurvival.water.BucketFill;
import com.example.realismsurvival.water.MachineFluidInput;
import net.minecraft.world.Container;
import com.example.realismsurvival.world.inventory.WaterFilterMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class WaterFilterBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {

    public static final int SLOT_INPUT_BUCKET = 0;
    public static final int SLOT_BOTTLE = 1;
    public static final int SLOT_RESULT_BOTTLE = 2;
    public static final int SLOT_EMPTY_BUCKET = 3;
    public static final int SLOT_CARTRIDGE = 4;

    public static final int DATA_PROGRESS = 0;
    public static final int DATA_TOTAL = 1;
    public static final int DATA_COUNT = 2;

    private static final int[] SLOTS_UP = {SLOT_INPUT_BUCKET, SLOT_BOTTLE};
    private static final int[] SLOTS_DOWN = {SLOT_RESULT_BOTTLE, SLOT_EMPTY_BUCKET};
    private static final int[] SLOTS_SIDES = {SLOT_INPUT_BUCKET, SLOT_BOTTLE, SLOT_CARTRIDGE};

    private NonNullList<ItemStack> items = NonNullList.withSize(5, ItemStack.EMPTY);
    private int progress;
    private int totalTime = 200;

    /** Pipe fluid input → stagnant bucket slot. */
    private final MachineFluidInput fluidInput = new MachineFluidInput(
            q -> q == ModWaterQualities.STAGNANT.get(),
            this::setChanged
    );

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int id) {
            return switch (id) {
                case DATA_PROGRESS -> progress;
                case DATA_TOTAL -> totalTime;
                default -> 0;
            };
        }

        @Override
        public void set(int id, int value) {
            switch (id) {
                case DATA_PROGRESS -> progress = value;
                case DATA_TOTAL -> totalTime = value;
                default -> {}
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public WaterFilterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WATER_FILTER.get(), pos, state);
    }

    public ContainerData getDataAccess() {
        return dataAccess;
    }

    public MachineFluidInput getFluidInput() {
        return fluidInput;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, WaterFilterBlockEntity entity) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        boolean changed = entity.fluidInput.tryFillBucketSlot(entity.items, SLOT_INPUT_BUCKET);
        entity.totalTime = RealismSurvivalConfig.FILTER_PROCESS_TICKS.getAsInt();
        if (entity.canProcess()) {
            entity.progress++;
            changed = true;
            if (entity.progress >= entity.totalTime) {
                entity.progress = 0;
                entity.finishCycle(serverLevel);
            }
        } else if (entity.progress > 0) {
            entity.progress = 0;
            changed = true;
        }

        boolean filtering = entity.progress > 0;
        if (state.getValue(WaterFilterBlock.FILTERING) != filtering) {
            level.setBlock(pos, state.setValue(WaterFilterBlock.FILTERING, filtering), 3);
            changed = true;
        }
        if (changed) {
            setChanged(level, pos, state);
        }
    }

    private void finishCycle(ServerLevel level) {
        ItemStack bottles = items.get(SLOT_BOTTLE);
        bottles.shrink(1);
        if (bottles.isEmpty()) {
            items.set(SLOT_BOTTLE, ItemStack.EMPTY);
        }

        mergeIntoSlot(SLOT_RESULT_BOTTLE, ModWaterQualities.FRESH.get().createBottle());

        ItemStack input = items.get(SLOT_INPUT_BUCKET);
        int fill = BucketFill.getFill(input);
        if (fill <= 1) {
            items.set(SLOT_INPUT_BUCKET, ItemStack.EMPTY);
            mergeIntoSlot(SLOT_EMPTY_BUCKET, new ItemStack(Items.BUCKET));
        } else {
            BucketFill.setFill(input, fill - 1);
        }

        damageCartridge(level);
        setChanged();
    }

    private void damageCartridge(ServerLevel level) {
        ItemStack cartridge = items.get(SLOT_CARTRIDGE);
        if (cartridge.isEmpty() || !cartridge.isDamageableItem()) {
            return;
        }
        // No operator player — break into empty when durability is spent.
        cartridge.hurtAndBreak(1, level, null, broken -> items.set(SLOT_CARTRIDGE, ItemStack.EMPTY));
    }

    private boolean canProcess() {
        if (!hasUsableCartridge()) {
            return false;
        }
        ItemStack input = items.get(SLOT_INPUT_BUCKET);
        // Single bucket only — fill is per-stack component; never mutate a multi-stack.
        if (!input.is(ModItemTags.FILTER_INPUT_BUCKETS)
                || input.getCount() != 1
                || BucketFill.getFill(input) <= 0) {
            return false;
        }
        if (!items.get(SLOT_BOTTLE).is(Items.GLASS_BOTTLE)) {
            return false;
        }
        ItemStack result = ModWaterQualities.FRESH.get().createBottle();
        if (!canMerge(items.get(SLOT_RESULT_BOTTLE), result)) {
            return false;
        }
        if (BucketFill.getFill(input) <= 1) {
            return canMerge(items.get(SLOT_EMPTY_BUCKET), new ItemStack(Items.BUCKET));
        }
        return true;
    }

    private boolean hasUsableCartridge() {
        ItemStack cartridge = items.get(SLOT_CARTRIDGE);
        return cartridge.getCount() == 1
                && cartridge.is(ModItemTags.FILTER_CARTRIDGES)
                && (!cartridge.isDamageableItem() || cartridge.getDamageValue() < cartridge.getMaxDamage());
    }

    private static boolean canMerge(ItemStack slot, ItemStack stack) {
        if (slot.isEmpty()) {
            return true;
        }
        return ItemStack.isSameItemSameComponents(slot, stack) && slot.getCount() < slot.getMaxStackSize();
    }

    private void mergeIntoSlot(int slot, ItemStack stack) {
        ItemStack existing = items.get(slot);
        if (existing.isEmpty()) {
            items.set(slot, stack);
        } else {
            existing.grow(stack.getCount());
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(5, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
        progress = input.getIntOr("Progress", 0);
        totalTime = input.getIntOr("TotalTime", 200);
        fluidInput.load(input);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Progress", progress);
        output.putInt("TotalTime", totalTime);
        ContainerHelper.saveAllItems(output, items);
        fluidInput.save(output);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.realismsurvival.water_filter");
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new WaterFilterMenu(containerId, inventory, this, dataAccess);
    }

    @Override
    public int getContainerSize() {
        return items.size();
    }

    /**
     * Hopper / transfer capacity uses this (not Menu {@code Slot} limits).
     * Keep water buckets at 1 so multi-stacks cannot stall {@code canProcess}.
     */
    @Override
    public int getMaxStackSize(ItemStack stack) {
        if (WaterApi.isGradedWaterBucket(stack.getItem()) || stack.is(Items.BUCKET)) {
            return 1;
        }
        if (stack.is(ModItemTags.FILTER_CARTRIDGES)) {
            return 1;
        }
        return super.getMaxStackSize(stack);
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    public int[] getSlotsForFace(Direction direction) {
        if (direction == Direction.DOWN) {
            return SLOTS_DOWN;
        }
        return direction == Direction.UP ? SLOTS_UP : SLOTS_SIDES;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        if (slot == SLOT_RESULT_BOTTLE || slot == SLOT_EMPTY_BUCKET) {
            return direction == Direction.DOWN;
        }
        // Allow hoppers to pull spent/partial cartridges (not from above — that face feeds inputs).
        if (slot == SLOT_CARTRIDGE) {
            return direction != Direction.UP;
        }
        return false;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            // Hoppers merge using the item max stack, not getMaxStackSize — keep count at 1.
            case SLOT_INPUT_BUCKET -> stack.is(ModItemTags.FILTER_INPUT_BUCKETS)
                    && BucketFill.getFill(stack) > 0
                    && items.get(SLOT_INPUT_BUCKET).isEmpty();
            case SLOT_BOTTLE -> stack.is(Items.GLASS_BOTTLE);
            case SLOT_CARTRIDGE -> stack.is(ModItemTags.FILTER_CARTRIDGES)
                    && items.get(SLOT_CARTRIDGE).isEmpty();
            default -> false;
        };
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }
}
