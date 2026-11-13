package com.example.realismsurvival.api.event;

import com.example.realismsurvival.api.water.WaterQuality;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.jspecify.annotations.Nullable;

/**
 * Fired on the logical server when a drink is about to apply thirst/temperature (and optional
 * graded-water status effects), after core defaults are resolved.
 *
 * <p>For held item drinks this is posted on the <em>last use-tick</em> (before vanilla consumes
 * the stack). Cancel skips stats/effects and aborts the use so the item is kept. World-water and
 * bucket sips also honor cancel (no sound/cooldown/fill loss). Mutate {@link #thirstRestore} /
 * {@link #temperatureDelta} to change the outcome. Addons subscribe on {@code NeoForge.EVENT_BUS}.
 */
public final class PlayerDrinkEvent extends PlayerEvent implements ICancellableEvent {

    private final ItemStack consumed;
    private final @Nullable WaterQuality quality;
    private float thirstRestore;
    private float temperatureDelta;
    private boolean applyQualityEffects;

    public PlayerDrinkEvent(
            ServerPlayer player,
            ItemStack consumed,
            @Nullable WaterQuality quality,
            float thirstRestore,
            float temperatureDelta
    ) {
        super(player);
        this.consumed = consumed;
        this.quality = quality;
        this.thirstRestore = thirstRestore;
        this.temperatureDelta = temperatureDelta;
        this.applyQualityEffects = quality != null;
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

    public float getThirstRestore() {
        return thirstRestore;
    }

    public void setThirstRestore(float thirstRestore) {
        this.thirstRestore = thirstRestore;
    }

    public float getTemperatureDelta() {
        return temperatureDelta;
    }

    public void setTemperatureDelta(float temperatureDelta) {
        this.temperatureDelta = temperatureDelta;
    }

    /** When {@code false}, graded-water status effects (nausea, slowness, …) are skipped. */
    public boolean shouldApplyQualityEffects() {
        return applyQualityEffects;
    }

    public void setApplyQualityEffects(boolean applyQualityEffects) {
        this.applyQualityEffects = applyQualityEffects;
    }
}
