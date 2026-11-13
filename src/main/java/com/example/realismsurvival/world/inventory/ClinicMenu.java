package com.example.realismsurvival.world.inventory;

import com.example.realismsurvival.clinic.ClinicService;
import com.example.realismsurvival.clinic.ClinicTreatment;
import com.example.realismsurvival.registry.ModMenus;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/** Button-only doctor menu. Buttons 0–3 map to {@link ClinicTreatment} ordinals. */
public class ClinicMenu extends AbstractContainerMenu {

    private static final double MAX_DISTANCE_SQR = 8.0 * 8.0;

    private final @Nullable ServerPlayer owner;
    private final @Nullable Entity doctor;
    private final int[] usesLeft = new int[ClinicTreatment.values().length];

    public ClinicMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null, null);
    }

    public ClinicMenu(
            int containerId, Inventory playerInventory, @Nullable ServerPlayer owner, @Nullable Entity doctor) {
        super(ModMenus.CLINIC.get(), containerId);
        this.owner = owner;
        this.doctor = doctor;
        for (int i = 0; i < usesLeft.length; i++) {
            final int index = i;
            addDataSlot(new DataSlot() {
                @Override
                public int get() {
                    return usesLeft[index];
                }

                @Override
                public void set(int value) {
                    usesLeft[index] = value;
                }
            });
        }
        refreshUses();
    }

    private void refreshUses() {
        if (owner == null) {
            return;
        }
        var board = ClinicService.syncedBoard(owner);
        ClinicTreatment[] values = ClinicTreatment.values();
        for (int i = 0; i < values.length; i++) {
            usesLeft[i] = Math.max(0, values[i].dailyLimit() - board.clinicUsesOf(values[i].id()));
        }
    }

    public int usesLeft(int ordinal) {
        if (ordinal < 0 || ordinal >= usesLeft.length) {
            return 0;
        }
        return usesLeft[ordinal];
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(player instanceof ServerPlayer serverPlayer) || owner != serverPlayer) {
            return false;
        }
        ClinicTreatment[] values = ClinicTreatment.values();
        if (id < 0 || id >= values.length) {
            return false;
        }
        boolean ok = ClinicService.treat(serverPlayer, values[id]);
        refreshUses();
        broadcastChanges();
        return ok;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        if (doctor == null) {
            return true;
        }
        if (doctor instanceof Villager villager && !villager.isAlive()) {
            return false;
        }
        return player.distanceToSqr(doctor) <= MAX_DISTANCE_SQR;
    }
}
