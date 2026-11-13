package com.example.realismsurvival.api;

import com.example.realismsurvival.api.environment.EnvironmentContext;
import com.example.realismsurvival.api.water.WaterQuality;
import com.example.realismsurvival.component.SurvivalAttachments;
import com.example.realismsurvival.component.SurvivalData;
import com.example.realismsurvival.data.SurvivalDataManager;
import com.example.realismsurvival.environment.EnvironmentProbe;
import com.example.realismsurvival.event.NaturalRegenHandler;
import com.example.realismsurvival.event.SurvivalTickHandler;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import org.jspecify.annotations.Nullable;

/**
 * Public facade for player thirst / temperature / equipment / drink application.
 * Prefer this over reaching into {@code component} / {@code event} / {@code data} from addons.
 */
public final class SurvivalApi {

    private SurvivalApi() {}

    public static SurvivalData getData(Player player) {
        return SurvivalAttachments.get(player);
    }

    /** Logical server only. */
    public static void setData(Player player, SurvivalData data) {
        SurvivalAttachments.set(player, data);
    }

    public static void addThirst(Player player, float delta) {
        setData(player, getData(player).addThirst(delta));
    }

    public static void addTemperature(Player player, float delta) {
        setData(player, getData(player).addTemperature(delta));
    }

    public static void withThirst(Player player, float thirst) {
        setData(player, getData(player).withThirst(thirst));
    }

    public static void withTemperature(Player player, float temperature) {
        setData(player, getData(player).withTemperature(temperature));
    }

    /**
     * Armor + Curios (if loaded) + {@link com.example.realismsurvival.api.event.CollectEquippedItemsEvent}.
     */
    public static List<ItemStack> collectEquipped(LivingEntity entity) {
        return EquipmentAccess.collectEquipped(entity);
    }

    /** Datapack insulation sum over {@link #collectEquipped(LivingEntity)}, clamped to {@code [0, 1]}. */
    public static float wornInsulation(LivingEntity entity) {
        return SurvivalDataManager.wornInsulation(entity);
    }

    /**
     * Sample surroundings (shelter / wetness / breath events included).
     * Same probe the core uses each survival evaluation window.
     */
    public static EnvironmentContext probeEnvironment(ServerPlayer player) {
        ServerLevel level = player.level();
        Holder<Biome> biome = level.getBiome(player.blockPosition());
        return EnvironmentProbe.probe(player, level, biome);
    }

    /**
     * Posts {@link com.example.realismsurvival.api.event.PlayerDrinkEvent} then applies thirst / temperature / effects.
     *
     * @return {@code false} if canceled — caller must not consume the item
     */
    public static boolean applyDrink(
            ServerPlayer player,
            ItemStack stack,
            @Nullable WaterQuality quality,
            float thirstRestore,
            float temperatureDelta
    ) {
        return SurvivalTickHandler.applyDrink(player, stack, quality, thirstRestore, temperatureDelta);
    }

    /**
     * @param effectMultiplier scales graded-water status effect durations (bucket sip = fill/3)
     */
    public static boolean applyDrink(
            ServerPlayer player,
            ItemStack stack,
            @Nullable WaterQuality quality,
            float thirstRestore,
            float temperatureDelta,
            float effectMultiplier
    ) {
        return SurvivalTickHandler.applyDrink(player, stack, quality, thirstRestore, temperatureDelta, effectMultiplier);
    }

    /**
     * Apply a custom heal without {@link NaturalRegenHandler} scaling (food-regen gate).
     * Prefer this for dedicated recovery items; heals of {@code ≥ 4} are already left alone.
     */
    public static void allowUnscaledHeal(Runnable healAction) {
        NaturalRegenHandler.allowNextHeal(healAction);
    }
}
