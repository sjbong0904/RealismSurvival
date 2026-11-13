package com.example.realismsurvival.water;

import com.example.realismsurvival.api.water.WaterQuality;
import java.util.function.Predicate;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

/**
 * Side fluid tank for filter/still: pipes (Create later, or any {@code Capabilities.Fluid.BLOCK} consumer)
 * insert graded water; each full bucket is materialized into the machine's water item slot.
 */
public final class MachineFluidInput {

    private final Predicate<WaterQuality> accepted;
    private final Runnable onChanged;
    private final FluidStacksResourceHandler tank;

    public MachineFluidInput(Predicate<WaterQuality> accepted, Runnable onChanged) {
        this.accepted = accepted;
        this.onChanged = onChanged;
        this.tank = new FluidStacksResourceHandler(1, GradedFluids.BUCKET_MB * 4) {
            @Override
            public boolean isValid(int index, FluidResource resource) {
                WaterQuality quality = GradedFluids.qualityOf(resource);
                return quality != null && accepted.test(quality);
            }

            @Override
            protected void onContentsChanged(int index, FluidStack previousContents) {
                onChanged.run();
            }
        };
    }

    public FluidStacksResourceHandler handler() {
        return tank;
    }

    /**
     * If the water item slot is empty and the tank holds ≥1 bucket of an accepted grade,
     * drain one bucket into that slot as a full graded bucket.
     *
     * @return {@code true} if a bucket was created
     */
    public boolean tryFillBucketSlot(NonNullList<ItemStack> items, int waterSlot) {
        if (!items.get(waterSlot).isEmpty()) {
            return false;
        }
        FluidResource resource = tank.getResource(0);
        int amount = tank.getAmountAsInt(0);
        if (amount < GradedFluids.BUCKET_MB) {
            return false;
        }
        WaterQuality quality = GradedFluids.qualityOf(resource);
        if (quality == null || !accepted.test(quality)) {
            return false;
        }

        try (Transaction tx = Transaction.openRoot()) {
            int extracted = tank.extract(0, resource, GradedFluids.BUCKET_MB, tx);
            if (extracted < GradedFluids.BUCKET_MB) {
                return false;
            }
            tx.commit();
        }

        items.set(waterSlot, BucketFill.createBucket(quality, BucketFill.MAX));
        onChanged.run();
        return true;
    }

    public void save(ValueOutput output) {
        tank.serialize(output.child("fluid_input"));
    }

    public void load(ValueInput input) {
        tank.deserialize(input.childOrEmpty("fluid_input"));
    }

    public @Nullable WaterQuality peekQuality() {
        return GradedFluids.qualityOf(tank.getResource(0));
    }
}
