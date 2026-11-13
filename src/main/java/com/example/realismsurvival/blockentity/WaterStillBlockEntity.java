package com.example.realismsurvival.blockentity;

import com.example.realismsurvival.RealismSurvivalConfig;
import com.example.realismsurvival.block.WaterStillBlock;
import com.example.realismsurvival.registry.ModBlockEntities;
import com.example.realismsurvival.registry.ModItems;
import net.minecraft.world.Container;
import com.example.realismsurvival.api.ModItemTags;
import com.example.realismsurvival.api.water.WaterApi;
import com.example.realismsurvival.registry.ModWaterQualities;
import com.example.realismsurvival.water.BucketFill;
import com.example.realismsurvival.water.MachineFluidInput;
import com.example.realismsurvival.water.VanillaWaterItems;
import com.example.realismsurvival.world.inventory.WaterStillMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class WaterStillBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {

    public static final int SLOT_FUEL = 0;
    public static final int SLOT_WATER = 1;
    public static final int SLOT_EMPTY_CONTAINER = 2;
    public static final int SLOT_DISTILLED = 3;
    public static final int SLOT_SALT = 4;

    public static final int DATA_LIT_TIME = 0;
    public static final int DATA_LIT_DURATION = 1;
    public static final int DATA_COOK_PROGRESS = 2;
    public static final int DATA_COOK_TOTAL = 3;
    public static final int DATA_COUNT = 4;

    /** Top: water + empty vessel (furnace ingredient face). */
    private static final int[] SLOTS_UP = {SLOT_WATER, SLOT_EMPTY_CONTAINER};
    /**
     * Bottom: products + leftover empty buckets in input slots (furnace pulls spent
     * buckets from the fuel slot the same way — otherwise WATER stays blocked).
     */
    private static final int[] SLOTS_DOWN = {SLOT_DISTILLED, SLOT_SALT, SLOT_WATER, SLOT_EMPTY_CONTAINER};
    /** Sides: fuel (furnace) plus inputs so side hoppers can feed the machine. */
    private static final int[] SLOTS_SIDES = {SLOT_FUEL, SLOT_WATER, SLOT_EMPTY_CONTAINER};

    private NonNullList<ItemStack> items = NonNullList.withSize(5, ItemStack.EMPTY);
    private int litTimeRemaining;
    private int litTotalTime;
    private int cookProgress;
    private int cookTotalTime;
    private boolean bucketCycle;

    /** Pipe / Create fluid input → water bucket slot (fresh / salt / stagnant). */
    private final MachineFluidInput fluidInput = new MachineFluidInput(
            q -> q == ModWaterQualities.FRESH.get()
                    || q == ModWaterQualities.SALT.get()
                    || q == ModWaterQualities.STAGNANT.get(),
            this::setChanged
    );

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int id) {
            return switch (id) {
                case DATA_LIT_TIME -> litTimeRemaining;
                case DATA_LIT_DURATION -> Math.min(litTotalTime, Short.MAX_VALUE);
                case DATA_COOK_PROGRESS -> cookProgress;
                case DATA_COOK_TOTAL -> cookTotalTime;
                default -> 0;
            };
        }

        @Override
        public void set(int id, int value) {
            switch (id) {
                case DATA_LIT_TIME -> litTimeRemaining = value;
                case DATA_LIT_DURATION -> litTotalTime = value;
                case DATA_COOK_PROGRESS -> cookProgress = value;
                case DATA_COOK_TOTAL -> cookTotalTime = value;
                default -> {}
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public WaterStillBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WATER_STILL.get(), pos, state);
    }

    public ContainerData getDataAccess() {
        return dataAccess;
    }

    public MachineFluidInput getFluidInput() {
        return fluidInput;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, WaterStillBlockEntity entity) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        boolean changed = entity.fluidInput.tryFillBucketSlot(entity.items, SLOT_WATER);
        changed |= entity.rewriteLegacyWaterInput(pos);
        boolean wasLit = entity.litTimeRemaining > 0;
        boolean isLit = wasLit;
        if (entity.litTimeRemaining > 0) {
            entity.litTimeRemaining--;
            isLit = entity.litTimeRemaining > 0;
        }

        int bottleTicks = RealismSurvivalConfig.STILL_PROCESS_TICKS.getAsInt();
        if (isLit || entity.hasFuelAndWork()) {
            if (entity.canProcess()) {
                if (!isLit && entity.tryConsumeFuel(serverLevel)) {
                    isLit = true;
                    changed = true;
                }
                if (isLit) {
                    if (entity.cookTotalTime <= 0) {
                        entity.cookTotalTime = entity.computeCycleTime(bottleTicks);
                        entity.bucketCycle = entity.isBucketOutputMode();
                    }
                    entity.cookProgress++;
                    changed = true;
                    if (entity.cookProgress >= entity.cookTotalTime) {
                        entity.finishCycle();
                        entity.cookProgress = 0;
                        entity.cookTotalTime = 0;
                    }
                }
            } else if (entity.cookProgress > 0 || entity.cookTotalTime > 0) {
                entity.cookProgress = 0;
                entity.cookTotalTime = 0;
                changed = true;
            }
        } else if (entity.cookProgress > 0) {
            entity.cookProgress = Mth.clamp(entity.cookProgress - 2, 0, entity.cookTotalTime);
            changed = true;
        }

        if (wasLit != isLit) {
            changed = true;
            state = state.setValue(WaterStillBlock.LIT, isLit);
            level.setBlock(pos, state, 3);
        }
        if (changed) {
            setChanged(level, pos, state);
        }
    }

    private boolean hasFuelAndWork() {
        return !items.get(SLOT_FUEL).isEmpty() && canProcess();
    }

    private boolean tryConsumeFuel(ServerLevel level) {
        ItemStack fuel = items.get(SLOT_FUEL);
        int burn = fuel.getBurnTime(RecipeType.SMELTING, level.fuelValues());
        if (burn <= 0) {
            return false;
        }
        litTimeRemaining = burn;
        litTotalTime = burn;
        ItemStackTemplate remainder = fuel.getItem().getCraftingRemainder();
        fuel.shrink(1);
        if (fuel.isEmpty()) {
            items.set(SLOT_FUEL, remainder != null ? remainder.create() : ItemStack.EMPTY);
        }
        return true;
    }

    private int computeCycleTime(int bottleTicks) {
        int base = isBucketOutputMode() ? bottleTicks * 3 : bottleTicks;
        if (isSolidStillInput(items.get(SLOT_WATER))) {
            double mult = RealismSurvivalConfig.STILL_SOLID_PROCESS_MULTIPLIER.getAsDouble();
            base = Math.max(1, (int) Math.round(base * mult));
        }
        return base;
    }

    private boolean isBucketOutputMode() {
        return items.get(SLOT_EMPTY_CONTAINER).is(Items.BUCKET);
    }

    /** Ice / snow need melt time; water buckets do not. */
    private static boolean isSolidStillInput(ItemStack stack) {
        return WaterApi.isGradedIceBucket(stack.getItem())
                || stack.is(ModItems.SNOW_BUCKET.get())
                || stack.is(Items.POWDER_SNOW_BUCKET);
    }

    private boolean canProcess() {
        ItemStack input = items.get(SLOT_WATER);
        if (!input.is(ModItemTags.STILL_INPUT_BUCKETS) || !BucketFill.supportsFill(input)) {
            return false;
        }
        // Single bucket only — fill is per-stack component.
        if (input.getCount() != 1) {
            return false;
        }
        int fill = BucketFill.getFill(input);
        if (fill <= 0) {
            return false;
        }
        ItemStack container = items.get(SLOT_EMPTY_CONTAINER);
        int saltNeeded = input.is(ModItemTags.STILL_SALT_SOURCE_BUCKETS)
                ? (container.is(Items.BUCKET) ? 3 : 1)
                : 0;
        if (saltNeeded > 0 && !canAcceptSalt(saltNeeded)) {
            return false;
        }
        if (container.is(Items.GLASS_BOTTLE)) {
            return canMergeDistilled(ModWaterQualities.DISTILLED.get().createBottle());
        }
        if (container.is(Items.BUCKET) && container.getCount() >= 1) {
            if (fill < BucketFill.MAX) {
                return false;
            }
            return canMergeDistilled(BucketFill.createBucket(ModWaterQualities.DISTILLED.get(), BucketFill.MAX));
        }
        return false;
    }

    private boolean canAcceptSalt(int count) {
        ItemStack slot = items.get(SLOT_SALT);
        if (slot.isEmpty()) {
            return count <= 64;
        }
        return slot.is(ModItems.SALT.get()) && slot.getCount() + count <= slot.getMaxStackSize();
    }

    private boolean canMergeDistilled(ItemStack stack) {
        ItemStack slot = items.get(SLOT_DISTILLED);
        if (slot.isEmpty()) {
            return true;
        }
        return ItemStack.isSameItemSameComponents(slot, stack) && slot.getCount() < slot.getMaxStackSize();
    }

    private void finishCycle() {
        ItemStack input = items.get(SLOT_WATER);
        if (!BucketFill.supportsFill(input)) {
            return;
        }
        boolean saltSource = input.is(ModItemTags.STILL_SALT_SOURCE_BUCKETS);
        boolean bucketOut = bucketCycle;

        if (bucketOut) {
            items.get(SLOT_EMPTY_CONTAINER).shrink(1);
            if (items.get(SLOT_EMPTY_CONTAINER).isEmpty()) {
                items.set(SLOT_EMPTY_CONTAINER, ItemStack.EMPTY);
            }
            items.set(SLOT_WATER, new ItemStack(Items.BUCKET));
            mergeIntoSlot(SLOT_DISTILLED, BucketFill.createBucket(ModWaterQualities.DISTILLED.get(), BucketFill.MAX));
            if (saltSource) {
                addSalt(3);
            }
        } else {
            items.get(SLOT_EMPTY_CONTAINER).shrink(1);
            if (items.get(SLOT_EMPTY_CONTAINER).isEmpty()) {
                items.set(SLOT_EMPTY_CONTAINER, ItemStack.EMPTY);
            }
            int fill = BucketFill.getFill(input);
            if (fill <= 1) {
                items.set(SLOT_WATER, new ItemStack(Items.BUCKET));
            } else {
                items.set(SLOT_WATER, BucketFill.afterPour(input));
            }
            mergeIntoSlot(SLOT_DISTILLED, ModWaterQualities.DISTILLED.get().createBottle());
            if (saltSource) {
                addSalt(1);
            }
        }
        setChanged();
    }

    private void addSalt(int count) {
        ItemStack slot = items.get(SLOT_SALT);
        if (slot.isEmpty()) {
            items.set(SLOT_SALT, new ItemStack(ModItems.SALT.get(), count));
        } else if (slot.is(ModItems.SALT.get())) {
            int room = slot.getMaxStackSize() - slot.getCount();
            if (room > 0) {
                slot.grow(Math.min(count, room));
            }
        }
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
        cookProgress = input.getIntOr("CookProgress", 0);
        cookTotalTime = input.getIntOr("CookTotalTime", 0);
        litTimeRemaining = input.getIntOr("LitTime", 0);
        litTotalTime = input.getIntOr("LitTotalTime", 0);
        bucketCycle = input.getBooleanOr("BucketCycle", false);
        fluidInput.load(input);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("CookProgress", cookProgress);
        output.putInt("CookTotalTime", cookTotalTime);
        output.putInt("LitTime", litTimeRemaining);
        output.putInt("LitTotalTime", litTotalTime);
        output.putBoolean("BucketCycle", bucketCycle);
        ContainerHelper.saveAllItems(output, items);
        fluidInput.save(output);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.realismsurvival.water_still");
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new WaterStillMenu(containerId, inventory, this, dataAccess);
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
        if (BucketFill.supportsFill(stack) || stack.is(Items.BUCKET)) {
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
        if (direction != Direction.DOWN) {
            return false;
        }
        if (slot == SLOT_DISTILLED || slot == SLOT_SALT) {
            return true;
        }
        // Pull spent empty buckets out so a new water bucket can enter.
        return (slot == SLOT_WATER || slot == SLOT_EMPTY_CONTAINER) && stack.is(Items.BUCKET);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_FUEL -> isFuel(stack);
            case SLOT_WATER -> canPlaceWaterInput(stack);
            case SLOT_EMPTY_CONTAINER -> canPlaceEmptyContainer(stack);
            default -> false;
        };
    }

    private boolean isFuel(ItemStack stack) {
        if (level == null) {
            return false;
        }
        return stack.getBurnTime(RecipeType.SMELTING, level.fuelValues()) > 0;
    }

    /**
     * Hoppers ignore {@link #getMaxStackSize(ItemStack)} when merging, so refuse a second
     * bucket while one is present (also blocks insert while a spent empty bucket remains).
     */
    private boolean canPlaceWaterInput(ItemStack stack) {
        // Hopper may deliver leftover vanilla water_bucket; tick rewrites it to graded.
        if (VanillaWaterItems.isVanillaWaterBucket(stack)) {
            return items.get(SLOT_WATER).isEmpty();
        }
        if (!stack.is(ModItemTags.STILL_INPUT_BUCKETS)
                || !BucketFill.supportsFill(stack)
                || BucketFill.getFill(stack) <= 0) {
            return false;
        }
        return items.get(SLOT_WATER).isEmpty();
    }

    /** Convert hopper/UI leftovers so {@link #canProcess()} can run. */
    private boolean rewriteLegacyWaterInput(BlockPos pos) {
        ItemStack input = items.get(SLOT_WATER);
        if (level == null || !VanillaWaterItems.isVanillaWaterBucket(input)) {
            return false;
        }
        items.set(SLOT_WATER, VanillaWaterItems.replaceWaterBucket(input, level, pos));
        return true;
    }

    private boolean canPlaceEmptyContainer(ItemStack stack) {
        if (stack.is(Items.BUCKET)) {
            return items.get(SLOT_EMPTY_CONTAINER).isEmpty();
        }
        if (stack.is(Items.GLASS_BOTTLE)) {
            ItemStack cur = items.get(SLOT_EMPTY_CONTAINER);
            return cur.isEmpty() || cur.is(Items.GLASS_BOTTLE);
        }
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }
}
