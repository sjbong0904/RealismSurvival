package com.example.realismsurvival.blockentity;

import com.example.realismsurvival.compat.shop.SupplyCategory;
import com.example.realismsurvival.compat.shop.SupplyMerchantSpawner;
import com.example.realismsurvival.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class SupplyPostMarkerBlockEntity extends BlockEntity {

    private boolean merchantReady;
    private SupplyCategory category;

    public SupplyPostMarkerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SUPPLY_POST_MARKER.get(), pos, state);
    }

    public void setCategory(SupplyCategory category) {
        this.category = category;
        setChanged();
    }

    public SupplyCategory category() {
        return category == null ? SupplyCategory.fromPos(getBlockPos()) : category;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SupplyPostMarkerBlockEntity be) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        if (be.category == null) {
            be.category = SupplyCategory.fromPos(pos);
            be.merchantReady = false;
            be.setChanged();
        }
        if (be.merchantReady) {
            return;
        }
        if ((level.getGameTime() + pos.asLong()) % 40L != 0L) {
            return;
        }
        if (SupplyMerchantSpawner.ensureNearby(server, pos, be.category)) {
            be.merchantReady = true;
            be.setChanged();
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        category = SupplyCategory.byId(input.getStringOr("SupplyCategory", ""));
        // MerchantSpecialized: older stalls used MerchantSettled and have no category yet.
        merchantReady = category != null && input.getBooleanOr("MerchantSpecialized", false);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (category != null) {
            output.putString("SupplyCategory", category.id());
        }
        output.putBoolean("MerchantSpecialized", merchantReady);
    }
}
