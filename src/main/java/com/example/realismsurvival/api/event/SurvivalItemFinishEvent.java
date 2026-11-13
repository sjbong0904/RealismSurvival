package com.example.realismsurvival.api.event;

import com.example.realismsurvival.api.water.WaterQuality;
import com.example.realismsurvival.component.SurvivalData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.jspecify.annotations.Nullable;

/**
 * Fired on the logical server after an item use finishes and the core has applied any
 * {@code drink} datapack / {@link PlayerDrinkEvent} effects. Food, medicine, stimulant, and
 * contagion addons hook here without competing with the drink pipeline.
 *
 * <p>Not cancellable — the item was already consumed. Mutate player state via {@code SurvivalApi}.
 */
public final class SurvivalItemFinishEvent extends PlayerEvent {

    private final ItemStack consumed;
    private final @Nullable WaterQuality quality;
    private final boolean drinkApplied;
    private final SurvivalData dataAfter;

    public SurvivalItemFinishEvent(
            ServerPlayer player,
            ItemStack consumed,
            @Nullable WaterQuality quality,
            boolean drinkApplied,
            SurvivalData dataAfter
    ) {
        super(player);
        this.consumed = consumed;
        this.quality = quality;
        this.drinkApplied = drinkApplied;
        this.dataAfter = dataAfter;
    }

    @Override
    public ServerPlayer getEntity() {
        return (ServerPlayer) super.getEntity();
    }

    public ItemStack getConsumed() {
        return consumed;
    }

    public @Nullable WaterQuality getQuality() {
        return quality;
    }

    /** True when core thirst/temperature drink logic ran (and was not canceled). */
    public boolean wasDrinkApplied() {
        return drinkApplied;
    }

    public SurvivalData getDataAfter() {
        return dataAfter;
    }
}
