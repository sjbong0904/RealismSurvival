package com.example.realismsurvival.event;

import com.example.realismsurvival.RealismSurvival;
import com.example.realismsurvival.RealismSurvivalConfig;
import com.example.realismsurvival.api.ModDamageTypes;
import com.example.realismsurvival.api.environment.EnvironmentContext;
import com.example.realismsurvival.api.event.PlayerDrinkEvent;
import com.example.realismsurvival.api.event.SurvivalEvaluateEvent;
import com.example.realismsurvival.api.event.SurvivalItemFinishEvent;
import com.example.realismsurvival.api.event.SurvivalRestEvent;
import com.example.realismsurvival.api.event.ThirstDrainEvent;
import com.example.realismsurvival.component.SurvivalAttachments;
import com.example.realismsurvival.component.SurvivalData;
import com.example.realismsurvival.data.DrinkDefinition;
import com.example.realismsurvival.data.SurvivalDataManager;
import com.example.realismsurvival.environment.EnvironmentProbe;
import com.example.realismsurvival.environment.TemperatureEvaluator;
import com.example.realismsurvival.water.BucketFill;
import com.example.realismsurvival.water.GradedIceBucketItem;
import com.example.realismsurvival.water.GradedWaterBucketItem;
import com.example.realismsurvival.water.ModEnchantments;
import com.example.realismsurvival.water.SnowBucketItem;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import com.example.realismsurvival.api.water.WaterQuality;
import com.example.realismsurvival.registry.ModWaterQualities;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.biome.Biome;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.jspecify.annotations.Nullable;

/**
 * Logical-server game loop for survival stats.
 *
 * <p>Every handler here:
 * <ol>
 *   <li>Casts to {@link ServerPlayer} first. Client-side players are ignored, which is the
 *       "physical separation" rule in practice: this class never touches client classes.</li>
 *   <li>Reads the current {@link SurvivalData}, derives a new one, writes it back via
 *       {@link SurvivalAttachments#set}. The write is what syncs the HUD.</li>
 *   <li>Pulls every number from the data pack ({@link SurvivalDataManager}) or the server config.
 *       There are no magic balancing constants in this file.</li>
 * </ol>
 */
@EventBusSubscriber(modid = RealismSurvival.MODID)
public final class SurvivalTickHandler {

    private SurvivalTickHandler() {}

    private static final float TICKS_PER_SECOND = 20.0F;

    // ---- Periodic evaluation ------------------------------------------------------------------------------------------

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return; // client side or non-server player: nothing to do
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        int interval = RealismSurvivalConfig.TICK_INTERVAL.getAsInt();
        if (player.tickCount % interval != 0) {
            return;
        }
        if (RealismSurvivalConfig.CREATIVE_BYPASS.getAsBoolean() && (player.isCreative() || player.isSpectator())) {
            return;
        }

        float windowSeconds = interval / TICKS_PER_SECOND;
        Holder<Biome> biome = level.getBiome(player.blockPosition());
        EnvironmentContext environment = EnvironmentProbe.probe(player, level, biome);

        SurvivalData current = SurvivalAttachments.get(player);
        SurvivalEvaluateEvent.Pre pre = new SurvivalEvaluateEvent.Pre(player, level, environment, windowSeconds, current);
        NeoForge.EVENT_BUS.post(pre);
        if (pre.isCanceled()) {
            return;
        }

        SurvivalData next = pre.getData();
        if (RealismSurvivalConfig.THIRST_ENABLED.getAsBoolean()) {
            next = evaluateThirst(player, level, biome, environment, next, windowSeconds);
        }
        if (RealismSurvivalConfig.TEMPERATURE_ENABLED.getAsBoolean()) {
            next = TemperatureEvaluator.evaluate(player, level, biome, environment, next, windowSeconds);
        }

        boolean changed = !next.equals(current);
        if (changed) {
            SurvivalAttachments.set(player, next); // -> disk dirty + sync to owning client
        }

        NeoForge.EVENT_BUS.post(new SurvivalEvaluateEvent.Post(
                player, level, environment, windowSeconds, current, next, changed
        ));
        SurvivalEffectsHandler.apply(player, level, next, windowSeconds);
    }

    @SubscribeEvent
    static void onWakeUp(PlayerWakeUpEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        SurvivalData data = SurvivalAttachments.get(player);
        NeoForge.EVENT_BUS.post(new SurvivalRestEvent(player, level, data, event.wakeImmediately()));
    }

    /** Thirst drain + swim restore + dehydration damage. Posts {@link ThirstDrainEvent} for addons. */
    private static SurvivalData evaluateThirst(
            ServerPlayer player,
            ServerLevel level,
            Holder<Biome> biome,
            EnvironmentContext environment,
            SurvivalData data,
            float seconds
    ) {
        float drain = SurvivalDataManager.biomeThirst(biome)
                .map(profile -> profile.drainFor(seconds, player.isSprinting()))
                .orElseGet(() -> (float) RealismSurvivalConfig.FALLBACK_THIRST_PER_SECOND.getAsDouble() * seconds);

        if (data.temperature() > RealismSurvivalConfig.HYPERTHERMIA_THRESHOLD.getAsDouble()) {
            drain *= (float) RealismSurvivalConfig.HYPERTHERMIA_THIRST_MULTIPLIER.getAsDouble();
        }

        ThirstDrainEvent drainEvent = new ThirstDrainEvent(player, level, biome, data, seconds, drain);
        NeoForge.EVENT_BUS.post(drainEvent);
        if (drainEvent.isCanceled()) {
            return data;
        }
        drain = Math.max(0.0F, drainEvent.getDrain());

        SurvivalData next = data.addThirst(-drain);
        float swimRestore = swimThirstRestore(player, level, biome, environment, seconds);
        if (swimRestore > 0.0F) {
            next = next.addThirst(swimRestore);
        }

        if (next.isDehydrated()) {
            float damage = (float) RealismSurvivalConfig.DEHYDRATION_DAMAGE.getAsDouble() * seconds;
            if (damage > 0.0F) {
                player.hurtServer(level, level.damageSources().source(ModDamageTypes.DEHYDRATION), damage);
            }
        }
        return next;
    }

    /**
     * Thirst restore while immersed, proportional to elapsed evaluation seconds.
     * Distilled / fresh rates come from server config (per second); stagnant/salt/unknown 0.
     * Rain alone does not count. Head-submersion multiplies by {@code swimThirstDiveMultiplier}.
     */
    private static float swimThirstRestore(
            ServerPlayer player,
            ServerLevel level,
            Holder<Biome> biome,
            EnvironmentContext environment,
            float seconds
    ) {
        if (!player.isInWater() || seconds <= 0.0F) {
            return 0.0F;
        }

        var feetFluid = level.getFluidState(player.blockPosition());
        WaterQuality quality = WaterQuality.fromFluidState(feetFluid);
        if (quality == null && feetFluid.is(net.minecraft.tags.FluidTags.WATER)) {
            quality = WaterQuality.fromBiome(biome);
        }
        float restorePerSecond = swimThirstRestorePerSecond(quality);
        if (restorePerSecond <= 0.0F) {
            return 0.0F;
        }
        float restore = restorePerSecond * seconds;
        if (environment.submerged()) {
            restore *= (float) RealismSurvivalConfig.SWIM_THIRST_DIVE_MULTIPLIER.getAsDouble();
        }
        return restore;
    }

    /** Thirst points restored per second by water grade. */
    private static float swimThirstRestorePerSecond(@Nullable WaterQuality quality) {
        if (quality == null) {
            return 0.0F;
        }
        if (quality == ModWaterQualities.DISTILLED.get()) {
            return (float) RealismSurvivalConfig.SWIM_THIRST_DISTILLED_PER_SECOND.getAsDouble();
        }
        if (quality == ModWaterQualities.FRESH.get()) {
            return (float) RealismSurvivalConfig.SWIM_THIRST_FRESH_PER_SECOND.getAsDouble();
        }
        return 0.0F;
    }

    // ---- Drinking -------------------------------------------------------------------------------------------------------

    /**
     * Players for whom {@link #applyDrink} already ran on the last use-tick (so Finish must not re-apply).
     */
    private static final Set<UUID> DRINK_APPLIED_ON_TICK = new HashSet<>();

    /**
     * Last use-tick: apply drink <em>before</em> vanilla consumes the stack. If
     * {@link PlayerDrinkEvent} is canceled, abort the use so the item is not consumed
     * (NeoForge Finish is too late — item is already used).
     */
    @SubscribeEvent
    static void onItemUseTick(LivingEntityUseItemEvent.Tick event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (event.getDuration() != 1) {
            return;
        }
        ItemStack stack = event.getItem();
        // Buckets apply in finishUsingItem (water = sip; ice = full drink); skip tick apply.
        if (stack.getItem() instanceof GradedWaterBucketItem
                || stack.getItem() instanceof GradedIceBucketItem
                || stack.getItem() instanceof SnowBucketItem) {
            return;
        }
        ResolvedDrink resolved = resolveDrink(player, stack);
        if (!resolved.matched()) {
            return;
        }
        boolean applied = applyDrink(player, stack, resolved.quality(), resolved.thirstRestore(), resolved.temperatureDelta());
        if (!applied) {
            event.setCanceled(true);
            event.setDuration(0);
            return;
        }
        DRINK_APPLIED_ON_TICK.add(player.getUUID());
    }

    /**
     * Fallback when Finish runs without a prior tick apply (edge cases), plus {@link SurvivalItemFinishEvent}.
     */
    @SubscribeEvent
    static void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ItemStack stack = event.getItem();
        ResolvedDrink resolved = resolveDrink(player, stack);

        boolean drinkApplied = false;
        if (DRINK_APPLIED_ON_TICK.remove(player.getUUID())) {
            drinkApplied = true;
        } else if (!(stack.getItem() instanceof GradedWaterBucketItem)
                && !(stack.getItem() instanceof GradedIceBucketItem)
                && !(stack.getItem() instanceof SnowBucketItem)
                && resolved.matched()) {
            // Graded / snow buckets apply in their own finishUsingItem — do not double-apply here.
            drinkApplied = applyDrink(player, stack, resolved.quality(), resolved.thirstRestore(), resolved.temperatureDelta());
            if (!drinkApplied) {
                // Finish already consumed; restore the pre-use copy so cancel still means "kept the drink".
                event.setResultStack(stack.copy());
            }
        }

        NeoForge.EVENT_BUS.post(new SurvivalItemFinishEvent(
                player,
                stack,
                resolved.quality(),
                drinkApplied,
                SurvivalAttachments.get(player)
        ));
    }

    private record ResolvedDrink(
            boolean matched,
            @Nullable WaterQuality quality,
            float thirstRestore,
            float temperatureDelta
    ) {}

    private static ResolvedDrink resolveDrink(ServerPlayer player, ItemStack stack) {
        WaterQuality quality = WaterQuality.fromBottleItem(stack.getItem());
        if (quality == null && isVanillaWaterPotion(stack)) {
            // Leftover vanilla water potions (worldgen/loot legacy) drink as distilled.
            quality = ModWaterQualities.DISTILLED.get();
        }

        if (quality != null) {
            float enchantTemp = ModEnchantments.drinkTemperatureDelta(stack, player.registryAccess());
            DrinkDefinition drink = SurvivalDataManager.drink(quality.bottleItem().builtInRegistryHolder()).orElse(null);
            float thirstRestore = drink != null ? drink.thirstRestore() : 0.0F;
            float temperatureDelta = (drink != null ? drink.temperatureDelta() : 0.0F) + enchantTemp;
            boolean matched = drink != null || enchantTemp != 0.0F || quality != null;
            return new ResolvedDrink(matched, quality, thirstRestore, temperatureDelta);
        }

        DrinkDefinition drink = SurvivalDataManager.drink(stack.typeHolder()).orElse(null);
        if (drink != null) {
            return new ResolvedDrink(true, null, drink.thirstRestore(), drink.temperatureDelta());
        }
        return new ResolvedDrink(false, null, 0.0F, 0.0F);
    }

    /**
     * Posts {@link PlayerDrinkEvent}, then applies thirst / temperature (and optional quality effects).
     * Also used by world-water drinking (T-04) with {@link ItemStack#EMPTY}.
     *
     * @return {@code false} if {@link PlayerDrinkEvent} was canceled (caller must not consume the item).
     */
    public static boolean applyDrink(
            ServerPlayer player,
            ItemStack stack,
            @Nullable WaterQuality quality,
            float thirstRestore,
            float temperatureDelta
    ) {
        return applyDrink(player, stack, quality, thirstRestore, temperatureDelta, 1.0F);
    }

    /**
     * @param effectMultiplier scales graded-water status effect durations (bucket sip = fill/3).
     * @return {@code false} if {@link PlayerDrinkEvent} was canceled (caller must not consume the item).
     */
    public static boolean applyDrink(
            ServerPlayer player,
            ItemStack stack,
            @Nullable WaterQuality quality,
            float thirstRestore,
            float temperatureDelta,
            float effectMultiplier
    ) {
        boolean thirstOn = RealismSurvivalConfig.THIRST_ENABLED.getAsBoolean();
        boolean tempOn = RealismSurvivalConfig.TEMPERATURE_ENABLED.getAsBoolean();
        float thirst = thirstOn ? thirstRestore : 0.0F;
        float temp = tempOn ? temperatureDelta : 0.0F;

        PlayerDrinkEvent drinkEvent = new PlayerDrinkEvent(player, stack, quality, thirst, temp);
        NeoForge.EVENT_BUS.post(drinkEvent);
        if (drinkEvent.isCanceled()) {
            return false;
        }

        thirst = drinkEvent.getThirstRestore();
        temp = drinkEvent.getTemperatureDelta();
        if (thirst != 0.0F || temp != 0.0F) {
            SurvivalData current = SurvivalAttachments.get(player);
            SurvivalAttachments.set(
                    player,
                    current.addThirst(thirst).applyTemperatureTowardNormal(temp)
            );
        }

        if (drinkEvent.shouldApplyQualityEffects() && drinkEvent.getQuality() != null) {
            drinkEvent.getQuality().applyDrinkEffects(player, effectMultiplier);
        }
        return true;
    }

    /**
     * One sip from a graded water bucket. Uses the bottle drink datapack entry × {@link BucketFill#sipMultiplier}.
     * Called from {@link com.example.realismsurvival.water.GradedWaterBucketItem#finishUsingItem} with the pre-sip stack.
     *
     * @return {@code false} if the drink was canceled — do not reduce fill / replace the stack.
     */
    public static boolean applyBucketSip(ServerPlayer player, ItemStack bucket, WaterQuality quality) {
        float mult = BucketFill.sipMultiplier(bucket);
        float enchantTemp = ModEnchantments.drinkTemperatureDelta(bucket, player.registryAccess());
        DrinkDefinition drink = SurvivalDataManager.drink(quality.bottleItem().builtInRegistryHolder()).orElse(null);
        float thirst = (drink != null ? drink.thirstRestore() : 0.0F) * mult;
        float temp = (drink != null ? drink.temperatureDelta() : 0.0F) * mult + enchantTemp;
        return applyDrink(player, bucket, quality, thirst, temp, mult);
    }

    /**
     * One sip from an ice bucket. Thirst matches the grade's <em>bottle</em> (same ratio as liquid
     * buckets); cooling uses the ice-bucket drink entry when present.
     *
     * @return {@code false} if canceled — do not reduce fill.
     */
    public static boolean applyIceBucketSip(ServerPlayer player, ItemStack bucket, WaterQuality quality) {
        float mult = BucketFill.sipMultiplier(bucket);
        float enchantTemp = ModEnchantments.drinkTemperatureDelta(bucket, player.registryAccess());
        DrinkDefinition bottle = SurvivalDataManager.drink(quality.bottleItem().builtInRegistryHolder()).orElse(null);
        DrinkDefinition ice = SurvivalDataManager.drink(bucket.getItem().builtInRegistryHolder()).orElse(null);
        float thirst = (bottle != null ? bottle.thirstRestore() : 0.0F) * mult;
        float tempBase = ice != null
                ? ice.temperatureDelta()
                : (bottle != null ? bottle.temperatureDelta() : 0.0F);
        float temp = tempBase * mult + enchantTemp;
        return applyDrink(player, bucket, quality, thirst, temp, mult);
    }

    /**
     * One sip from a snow bucket. Mild cooling always; thirst restore and a light debuff are random
     * within a tame band (may roll zero thirst / no debuff).
     *
     * @return {@code false} if canceled — do not reduce fill.
     */
    public static boolean applySnowBucketSip(ServerPlayer player, ItemStack bucket) {
        float mult = BucketFill.sipMultiplier(bucket);
        RandomSource random = player.getRandom();
        // ~25% chance of no thirst; else 2–7 points × sip strength.
        float thirst = 0.0F;
        if (random.nextFloat() > 0.25F) {
            thirst = (2.0F + random.nextFloat() * 5.0F) * mult;
        }
        // Always a little cooling toward normal.
        float temp = -(0.25F + random.nextFloat() * 0.45F) * mult;
        float enchantTemp = ModEnchantments.drinkTemperatureDelta(bucket, player.registryAccess());
        temp += enchantTemp;

        if (!applyDrink(player, bucket, null, thirst, temp, 0.0F)) {
            return false;
        }

        // ~28% chance of a short mild debuff (hunger / fatigue / slowness).
        if (random.nextFloat() < 0.28F) {
            int duration = Math.round((60 + random.nextInt(60)) * Math.max(0.35F, mult));
            MobEffectInstance effect = switch (random.nextInt(3)) {
                case 0 -> new MobEffectInstance(MobEffects.HUNGER, duration, 0);
                case 1 -> new MobEffectInstance(MobEffects.MINING_FATIGUE, duration, 0);
                default -> new MobEffectInstance(MobEffects.SLOWNESS, duration, 0);
            };
            player.addEffect(effect);
        }
        return true;
    }

    private static boolean isVanillaWaterPotion(ItemStack stack) {
        if (!stack.is(Items.POTION)) {
            return false;
        }
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        return contents != null && contents.is(Potions.WATER);
    }
}
