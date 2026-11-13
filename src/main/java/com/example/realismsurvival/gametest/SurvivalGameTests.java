package com.example.realismsurvival.gametest;

import com.example.realismsurvival.RealismSurvival;
import com.example.realismsurvival.RealismSurvivalConfig;
import com.example.realismsurvival.api.event.PlayerDrinkEvent;
import com.example.realismsurvival.api.environment.EnvironmentContext;
import com.example.realismsurvival.api.water.WaterApi;
import com.example.realismsurvival.api.water.WaterQuality;
import com.example.realismsurvival.component.SurvivalAttachments;
import com.example.realismsurvival.component.SurvivalData;
import com.example.realismsurvival.data.DrinkDefinition;
import com.example.realismsurvival.data.SurvivalDataManager;
import com.example.realismsurvival.environment.EnvironmentProbe;
import com.example.realismsurvival.environment.TemperatureEvaluator;
import com.example.realismsurvival.event.SurvivalTickHandler;
import com.example.realismsurvival.event.VanillaWaterItemRewriteHandler;
import com.example.realismsurvival.blockentity.WaterStillBlockEntity;
import com.example.realismsurvival.registry.ModBlocks;
import com.example.realismsurvival.registry.ModFluids;
import com.example.realismsurvival.registry.ModItems;
import com.example.realismsurvival.registry.ModWaterQualities;
import com.example.realismsurvival.water.CauldronWater;
import com.example.realismsurvival.water.FreshPoolDecayData;
import com.example.realismsurvival.water.FreshWaterStagnator;
import com.example.realismsurvival.water.GradedFlowingFluid;
import com.example.realismsurvival.water.GradedFluids;
import com.example.realismsurvival.water.VanillaWaterItems;
import com.example.realismsurvival.water.WaterQualityStates;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.cauldron.CauldronInteractions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Bucketable;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.fish.Cod;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Core survival GameTests (T-11 + base-core contract): thirst, drink, respawn, cancel, temperature, water quality.
 *
 * <p>Linked from {@code data/realismsurvival/test_instance/*.json} via {@code BuiltInRegistries.TEST_FUNCTION}.
 */
public final class SurvivalGameTests {

    private SurvivalGameTests() {}

    public static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
            DeferredRegister.create(BuiltInRegistries.TEST_FUNCTION, RealismSurvival.MODID);

    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> THIRST_DRAIN =
            TEST_FUNCTIONS.register("thirst_drain", () -> SurvivalGameTests::thirstDrain);

    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> DRINK_RESTORE =
            TEST_FUNCTIONS.register("drink_restore", () -> SurvivalGameTests::drinkRestore);

    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> RESPAWN_RESET =
            TEST_FUNCTIONS.register("respawn_reset", () -> SurvivalGameTests::respawnReset);

    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> DRINK_CANCEL =
            TEST_FUNCTIONS.register("drink_cancel", () -> SurvivalGameTests::drinkCancel);

    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TEMPERATURE_CONVERGE =
            TEST_FUNCTIONS.register("temperature_converge", () -> SurvivalGameTests::temperatureConverge);

    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> WATER_QUALITY_RESOLVE =
            TEST_FUNCTIONS.register("water_quality_resolve", () -> SurvivalGameTests::waterQualityResolve);

    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> WATER_QUALITY_STATE =
            TEST_FUNCTIONS.register("water_quality_state", () -> SurvivalGameTests::waterQualityState);

    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> PENALTY_HYPOTHERMIA =
            TEST_FUNCTIONS.register("penalty_hypothermia", () -> SurvivalGameTests::penaltyHypothermia);

    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CAULDRON_QUALITY_IO =
            TEST_FUNCTIONS.register("cauldron_quality_io", () -> SurvivalGameTests::cauldronQualityIo);

    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> BUCKET_FISH_CAPTURE =
            TEST_FUNCTIONS.register("bucket_fish_capture", () -> SurvivalGameTests::bucketFishCapture);

    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> AXOLOTL_FEED_LEFTOVER =
            TEST_FUNCTIONS.register("axolotl_feed_leftover", () -> SurvivalGameTests::axolotlFeedLeftover);

    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> DISPENSER_WATER_BOTTLE =
            TEST_FUNCTIONS.register("dispenser_water_bottle", () -> SurvivalGameTests::dispenserWaterBottle);

    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> DISTILLED_BREWING =
            TEST_FUNCTIONS.register("distilled_brewing", () -> SurvivalGameTests::distilledBrewing);

    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> LEGACY_WATER_ITEM_REWRITE =
            TEST_FUNCTIONS.register("legacy_water_item_rewrite", () -> SurvivalGameTests::legacyWaterItemRewrite);

    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> DISTILLED_NO_INFINITE_SOURCE =
            TEST_FUNCTIONS.register("distilled_no_infinite_source", () -> SurvivalGameTests::distilledNoInfiniteSource);

    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> GRADED_WATER_VANILLA_TAGS =
            TEST_FUNCTIONS.register("graded_water_vanilla_tags", () -> SurvivalGameTests::gradedWaterVanillaTags);

    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> FRESH_POOL_STAGNATION =
            TEST_FUNCTIONS.register("fresh_pool_stagnation", () -> SurvivalGameTests::freshPoolStagnation);

    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> MACHINE_FLUID_INPUT =
            TEST_FUNCTIONS.register("machine_fluid_input", () -> SurvivalGameTests::machineFluidInput);

    public static void register(IEventBus modBus) {
        TEST_FUNCTIONS.register(modBus);
    }

    /**
     * Like {@link GameTestHelper#makeMockServerPlayerInLevel()} but survival-mode (creative bypass must not apply).
     */
    private static ServerPlayer mockSurvivalPlayer(GameTestHelper helper) {
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "rs-gametest"), false);
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), cookie.gameProfile(), cookie.clientInformation()) {
            {
                Objects.requireNonNull(helper);
            }

            @Override
            public GameType gameMode() {
                return GameType.SURVIVAL;
            }
        };
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        helper.getLevel().getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        // Survival GameType alone is not enough — abilities still default to creative-like fills.
        player.getAbilities().instabuild = false;
        player.getAbilities().mayfly = false;
        player.onUpdateAbilities();
        return player;
    }

    /** One evaluation window must reduce thirst below the starting value. */
    private static void thirstDrain(GameTestHelper helper) {
        ServerPlayer player = mockSurvivalPlayer(helper);
        SurvivalAttachments.set(player, new SurvivalData(100.0F, SurvivalData.TEMPERATURE_DEFAULT));
        player.tickCount = RealismSurvivalConfig.TICK_INTERVAL.getAsInt();

        NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(player));

        float after = SurvivalAttachments.get(player).thirst();
        helper.assertTrue(after < 100.0F, "thirst should drain on evaluation tick (was " + after + ")");
        helper.succeed();
    }

    /** Datapack {@code drink/potion.json} restores thirst through {@link SurvivalTickHandler#applyDrink}. */
    private static void drinkRestore(GameTestHelper helper) {
        ServerPlayer player = mockSurvivalPlayer(helper);
        SurvivalAttachments.set(player, new SurvivalData(10.0F, SurvivalData.TEMPERATURE_DEFAULT));

        DrinkDefinition drink = SurvivalDataManager.drink(Items.POTION.builtInRegistryHolder())
                .orElseThrow(() -> helper.assertionException("missing drink entry for minecraft:potion"));
        boolean applied = SurvivalTickHandler.applyDrink(
                player, new ItemStack(Items.POTION), null, drink.thirstRestore(), drink.temperatureDelta());
        helper.assertTrue(applied, "drink should apply when not canceled");

        float after = SurvivalAttachments.get(player).thirst();
        float expected = 10.0F + drink.thirstRestore();
        helper.assertTrue(Math.abs(after - expected) < 0.01F, "thirst after drink expected " + expected + " got " + after);
        helper.succeed();
    }

    /** Death respawn with {@code respawnResetOnDeath=true} applies configured thirst/temperature. */
    private static void respawnReset(GameTestHelper helper) {
        ServerPlayer player = mockSurvivalPlayer(helper);
        SurvivalAttachments.set(player, new SurvivalData(5.0F, 32.0F));

        NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerRespawnEvent(player, false));

        SurvivalData data = SurvivalAttachments.get(player);
        float expectedThirst = (float) RealismSurvivalConfig.RESPAWN_THIRST.getAsDouble();
        float expectedTemp = (float) RealismSurvivalConfig.RESPAWN_TEMPERATURE.getAsDouble();
        helper.assertTrue(Math.abs(data.thirst() - expectedThirst) < 0.01F, "respawn thirst expected " + expectedThirst + " got " + data.thirst());
        helper.assertTrue(Math.abs(data.temperature() - expectedTemp) < 0.01F, "respawn temp expected " + expectedTemp + " got " + data.temperature());
        helper.succeed();
    }

    /** Canceled {@link PlayerDrinkEvent} must not change thirst and must return false. */
    private static void drinkCancel(GameTestHelper helper) {
        ServerPlayer player = mockSurvivalPlayer(helper);
        SurvivalAttachments.set(player, new SurvivalData(40.0F, SurvivalData.TEMPERATURE_DEFAULT));

        CancelDrinkListener cancel = new CancelDrinkListener();
        NeoForge.EVENT_BUS.register(cancel);
        try {
            boolean applied = SurvivalTickHandler.applyDrink(player, ItemStack.EMPTY, null, 25.0F, 0.0F);
            helper.assertTrue(!applied, "canceled drink must return false");
            float after = SurvivalAttachments.get(player).thirst();
            helper.assertTrue(Math.abs(after - 40.0F) < 0.01F, "canceled drink must not change thirst (was " + after + ")");
        } finally {
            NeoForge.EVENT_BUS.unregister(cancel);
        }
        helper.succeed();
    }

    /**
     * °C/hour rate table + one-hour evaluate step must match {@code rate × multiplier}.
     */
    private static void temperatureConverge(GameTestHelper helper) {
        float normal = SurvivalData.TEMPERATURE_DEFAULT;
        helper.assertTrue(
                Math.abs(TemperatureEvaluator.bodyTempRatePerHour(
                        20.0F, 0.0F, false, TemperatureEvaluator.Activity.IDLE, normal)) < 1.0E-4F,
                "comfort band idle should be 0"
        );
        helper.assertTrue(
                Math.abs(TemperatureEvaluator.bodyTempRatePerHour(
                        20.0F, 0.0F, false, TemperatureEvaluator.Activity.SPRINT, normal) - 0.05F) < 1.0E-4F,
                "sprint heat weaker than env bands"
        );
        helper.assertTrue(
                Math.abs(TemperatureEvaluator.bodyTempRatePerHour(
                        20.0F, 0.0F, false, TemperatureEvaluator.Activity.WALK, 34.0F) - 0.05F) < 1.0E-4F,
                "walk converges toward normal (capped)"
        );
        helper.assertTrue(
                Math.abs(TemperatureEvaluator.bodyTempRatePerHour(
                        5.0F, 0.0F, false, TemperatureEvaluator.Activity.WALK, 34.0F) - (-0.5125F)) < 1.0E-4F,
                "cold env still dominates walk converge"
        );
        helper.assertTrue(
                Math.abs(TemperatureEvaluator.bodyTempRatePerHour(
                        5.0F, 0.0F, false, TemperatureEvaluator.Activity.IDLE, normal) - (-0.5625F)) < 1.0E-4F,
                "0–10 C band midpoint"
        );
        helper.assertTrue(
                Math.abs(TemperatureEvaluator.bodyTempRatePerHour(
                        20.0F, 0.0F, true, TemperatureEvaluator.Activity.IDLE, normal) - (-4.5F)) < 1.0E-4F,
                "immersion rate"
        );
        helper.assertTrue(
                Math.abs(TemperatureEvaluator.bodyTempRatePerHour(
                        20.0F, 0.0F, true, TemperatureEvaluator.Activity.SWIM, normal) - (-4.46F)) < 1.0E-4F,
                "immersion + swim activity"
        );

        ServerPlayer player = mockSurvivalPlayer(helper);
        ServerLevel level = helper.getLevel();
        Holder<Biome> biome = level.getBiome(player.blockPosition());
        EnvironmentContext environment = EnvironmentProbe.probe(player, level, biome);

        float startTemp = SurvivalData.TEMPERATURE_DEFAULT;
        SurvivalData start = new SurvivalData(100.0F, startTemp);
        SurvivalAttachments.set(player, start);

        float ambient = TemperatureEvaluator.computeAmbient(player, level, biome);
        float insulation = SurvivalDataManager.wornInsulation(player);
        TemperatureEvaluator.Activity activity = TemperatureEvaluator.resolveActivity(player);
        float rate = TemperatureEvaluator.bodyTempRatePerHour(
                ambient, insulation, player.isInWater(), activity, startTemp
        );
        float multiplier = (float) RealismSurvivalConfig.TEMPERATURE_RATE_MULTIPLIER.getAsDouble();
        float seconds = 3600.0F;
        float expected = Mth.clamp(
                startTemp + rate * multiplier * (seconds / 3600.0F),
                SurvivalData.TEMPERATURE_MIN,
                SurvivalData.TEMPERATURE_MAX
        );

        SurvivalData after = TemperatureEvaluator.evaluate(player, level, biome, environment, start, seconds);
        helper.assertTrue(
                Math.abs(after.temperature() - expected) < 0.05F,
                "1h step should apply rate " + rate + " → " + expected + " got " + after.temperature()
        );
        helper.succeed();
    }

    /** {@link WaterApi#resolveFromBiome} returns a registered built-in quality. */
    private static void waterQualityResolve(GameTestHelper helper) {
        ServerPlayer player = mockSurvivalPlayer(helper);
        Holder<Biome> biome = helper.getLevel().getBiome(player.blockPosition());
        WaterQuality quality = WaterApi.resolveFromBiome(biome);
        helper.assertTrue(quality != null, "resolveFromBiome must return a quality");
        helper.assertTrue(
                WaterApi.registry().getKey(quality) != null,
                "quality must be in WaterQuality registry"
        );
        helper.succeed();
    }

    /**
     * Shared fluid + QUALITY: stagnant source keeps identity; distilled source carved into
     * stagnant flowing cell still reads as distilled.
     */
    private static void waterQualityState(GameTestHelper helper) {
        BlockPos stagnantRel = new BlockPos(1, 2, 1);
        BlockPos flowRel = new BlockPos(2, 2, 1);
        helper.setBlock(stagnantRel, Blocks.AIR.defaultBlockState());
        helper.setBlock(flowRel, Blocks.AIR.defaultBlockState());

        GradedFlowingFluid fluid = ModFluids.graded();
        WaterQuality stagnant = ModWaterQualities.STAGNANT.get();
        WaterQuality distilled = ModWaterQualities.DISTILLED.get();

        helper.setBlock(stagnantRel, fluid.sourceOf(stagnant, false).createLegacyBlock());
        FluidState stagnantState = helper.getLevel().getFluidState(helper.absolutePos(stagnantRel));
        helper.assertTrue(WaterApi.isGradedWaterFluid(stagnantState.getType()), "must be shared graded fluid");
        helper.assertTrue(
                WaterApi.fromFluidState(stagnantState) == stagnant,
                "source QUALITY must be stagnant"
        );

        helper.setBlock(flowRel, fluid.flowingOf(stagnant, 7, false).createLegacyBlock());
        helper.setBlock(flowRel, fluid.sourceOf(distilled, false).createLegacyBlock());
        FluidState carved = helper.getLevel().getFluidState(helper.absolutePos(flowRel));
        helper.assertTrue(
                WaterApi.fromFluidState(carved) == distilled,
                "source placed into flowing path must keep distilled QUALITY"
        );
        helper.assertTrue(
                WaterQualityStates.sameQuality(carved, fluid.sourceOf(distilled, false)),
                "QUALITY ids must match distilled"
        );
        helper.succeed();
    }

    /** Severe hypothermia applies Slowness via the penalty handler. */
    private static void penaltyHypothermia(GameTestHelper helper) {
        ServerPlayer player = mockSurvivalPlayer(helper);
        float severe = (float) RealismSurvivalConfig.HYPOTHERMIA_SEVERE_THRESHOLD.getAsDouble();
        SurvivalAttachments.set(player, new SurvivalData(100.0F, severe));
        player.tickCount = RealismSurvivalConfig.TICK_INTERVAL.getAsInt();

        NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(player));

        helper.assertTrue(
                player.hasEffect(MobEffects.SLOWNESS),
                "severe hypothermia should apply Slowness"
        );
        helper.succeed();
    }

    /**
     * Water cauldron QUALITY round-trip + mismatch reject (no water place via FAIL).
     * Worldgen default quality id is distilled.
     */
    private static void cauldronQualityIo(GameTestHelper helper) {
        ServerPlayer player = mockSurvivalPlayer(helper);
        ServerLevel level = helper.getLevel();
        BlockPos rel = new BlockPos(1, 2, 1);
        BlockPos abs = helper.absolutePos(rel);

        WaterQuality fresh = ModWaterQualities.FRESH.get();
        WaterQuality salt = ModWaterQualities.SALT.get();
        WaterQuality distilled = ModWaterQualities.DISTILLED.get();

        helper.assertTrue(
                CauldronWater.DEFAULT_QUALITY_ID == WaterQualityStates.idOf(distilled),
                "worldgen / default cauldron QUALITY id must be distilled"
        );

        BlockState fullFresh = CauldronWater.full(fresh);
        helper.setBlock(rel, fullFresh);
        BlockState placed = level.getBlockState(abs);
        helper.assertTrue(CauldronWater.isWaterCauldron(placed), "must be water cauldron");
        helper.assertTrue(CauldronWater.quality(placed) == fresh, "placed cauldron QUALITY must be fresh");
        helper.assertTrue(
                WaterApi.fromBucketItem(CauldronWater.createBucket(placed).getItem()) == fresh,
                "scoop bucket must be fresh"
        );
        helper.assertTrue(
                WaterApi.fromBottleItem(CauldronWater.createBottle(placed).getItem()) == fresh,
                "scoop bottle must be fresh"
        );
        helper.assertTrue(!CauldronWater.accepts(placed, salt), "salt must not mix into fresh cauldron");

        ItemStack saltBucket = salt.createBucket();
        player.setItemInHand(InteractionHand.MAIN_HAND, saltBucket);
        InteractionResult mismatch = CauldronInteractions.WATER
                .get(saltBucket)
                .interact(placed, level, abs, player, InteractionHand.MAIN_HAND, saltBucket);
        helper.assertTrue(mismatch == InteractionResult.FAIL, "mismatch pour must FAIL (not place water)");

        ItemStack freshBucket = fresh.createBucket();
        player.setItemInHand(InteractionHand.MAIN_HAND, freshBucket);
        InteractionResult pour = CauldronInteractions.WATER
                .get(freshBucket)
                .interact(Blocks.CAULDRON.defaultBlockState(), level, abs, player, InteractionHand.MAIN_HAND, freshBucket);
        helper.assertTrue(pour.consumesAction(), "fresh bucket into empty cauldron should succeed");
        helper.assertTrue(
                CauldronWater.quality(level.getBlockState(abs)) == fresh,
                "empty cauldron filled with fresh bucket must be fresh"
        );

        ItemStack rewrittenBucket = VanillaWaterItems.replaceWaterBucket(new ItemStack(Items.WATER_BUCKET), level, abs);
        helper.assertTrue(
                WaterApi.isGradedWaterBucket(rewrittenBucket.getItem()),
                "vanilla water bucket rewrite must yield a graded bucket"
        );
        ItemStack rewrittenBottle = VanillaWaterItems.replaceWaterPotionWithDistilled(
                net.minecraft.world.item.alchemy.PotionContents.createItemStack(
                        Items.POTION, net.minecraft.world.item.alchemy.Potions.WATER));
        helper.assertTrue(
                WaterApi.fromBottleItem(rewrittenBottle.getItem()) == distilled,
                "vanilla water potion rewrite must be distilled bottle"
        );
        helper.succeed();
    }

    /** Empty / graded full buckets can capture fish via {@link Bucketable#bucketMobPickup}. */
    private static void bucketFishCapture(GameTestHelper helper) {
        ServerPlayer player = mockSurvivalPlayer(helper);
        BlockPos rel = new BlockPos(1, 2, 1);
        helper.setBlock(rel, ModFluids.graded().sourceOf(ModWaterQualities.FRESH.get(), false).createLegacyBlock());

        Cod cod = helper.spawn(EntityType.COD, rel);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));
        Optional<InteractionResult> emptyCatch = Bucketable.bucketMobPickup(player, InteractionHand.MAIN_HAND, cod);
        helper.assertTrue(emptyCatch.isPresent() && emptyCatch.get().consumesAction(), "empty bucket must catch fish");
        helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).is(Items.COD_BUCKET), "result must be cod bucket");
        helper.assertTrue(!cod.isAlive(), "caught cod must be discarded");

        Cod cod2 = helper.spawn(EntityType.COD, rel);
        player.setItemInHand(InteractionHand.MAIN_HAND, ModWaterQualities.FRESH.get().createBucket());
        Optional<InteractionResult> gradedCatch = Bucketable.bucketMobPickup(player, InteractionHand.MAIN_HAND, cod2);
        helper.assertTrue(gradedCatch.isPresent() && gradedCatch.get().consumesAction(), "graded bucket must catch fish");
        helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).is(Items.COD_BUCKET), "graded catch must be cod bucket");
        helper.succeed();
    }

    /** Feeding tropical fish bucket leaves a graded water bucket, not vanilla. */
    private static void axolotlFeedLeftover(GameTestHelper helper) {
        ServerPlayer player = mockSurvivalPlayer(helper);
        BlockPos rel = new BlockPos(1, 2, 1);
        helper.setBlock(rel, ModFluids.graded().sourceOf(ModWaterQualities.FRESH.get(), false).createLegacyBlock());

        Axolotl axolotl = helper.spawn(EntityType.AXOLOTL, rel);
        axolotl.setAge(0);
        ItemStack food = new ItemStack(Items.TROPICAL_FISH_BUCKET);
        player.setItemInHand(InteractionHand.MAIN_HAND, food);
        // Call protected usePlayerItem (vanilla feed path) so AxolotlFeedMixin RETURN runs.
        try {
            var method = Axolotl.class.getDeclaredMethod(
                    "usePlayerItem", Player.class, InteractionHand.class, ItemStack.class);
            method.setAccessible(true);
            method.invoke(axolotl, player, InteractionHand.MAIN_HAND, food);
        } catch (ReflectiveOperationException e) {
            throw helper.assertionException("usePlayerItem invoke failed: " + e);
        }
        ItemStack leftover = player.getItemInHand(InteractionHand.MAIN_HAND);
        helper.assertTrue(!leftover.is(Items.WATER_BUCKET), "must not leave vanilla water_bucket");
        helper.assertTrue(
                WaterApi.isGradedWaterBucket(leftover.getItem()),
                "leftover must be graded water bucket (was " + leftover.getItem() + ")"
        );
        helper.succeed();
    }

    /** Dispenser glass bottle into graded water yields a graded bottle (not Potions.WATER). */
    private static void dispenserWaterBottle(GameTestHelper helper) {
        BlockPos dispRel = new BlockPos(1, 2, 1);
        BlockPos waterRel = new BlockPos(2, 2, 1);
        BlockPos powerRel = new BlockPos(1, 2, 0);

        WaterQuality fresh = ModWaterQualities.FRESH.get();
        helper.setBlock(
                waterRel,
                ModFluids.graded().sourceOf(fresh, false).createLegacyBlock()
        );
        helper.setBlock(
                dispRel,
                Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, Direction.EAST)
        );

        DispenserBlockEntity dispenser = helper.getBlockEntity(dispRel, DispenserBlockEntity.class);
        dispenser.setItem(0, new ItemStack(Items.GLASS_BOTTLE));

        // Rising edge schedules dispense tick (4 ticks).
        helper.setBlock(powerRel, Blocks.REDSTONE_BLOCK.defaultBlockState());

        helper.succeedWhen(() -> {
            ItemStack slot = dispenser.getItem(0);
            helper.assertTrue(
                    WaterApi.fromBottleItem(slot.getItem()) == fresh,
                    "dispenser must produce fresh water bottle, got " + slot
            );
        });
    }

    /** Distilled water bottle + nether wart → awkward potion (brewing bootstrap). */
    private static void distilledBrewing(GameTestHelper helper) {
        ItemStack distilled = new ItemStack(ModItems.DISTILLED_WATER_BOTTLE.get());
        ItemStack wart = new ItemStack(Items.NETHER_WART);
        var brewing = helper.getLevel().potionBrewing();
        helper.assertTrue(brewing.isInput(distilled), "distilled bottle must be a brewing input");
        helper.assertTrue(brewing.hasMix(distilled, wart), "distilled + nether wart must have a mix");
        ItemStack out = brewing.mix(wart, distilled);
        PotionContents contents = out.get(net.minecraft.core.component.DataComponents.POTION_CONTENTS);
        helper.assertTrue(contents != null && contents.is(Potions.AWKWARD), "output must be awkward potion, got " + out);
        helper.assertTrue(
                !brewing.isInput(ModWaterQualities.FRESH.get().createBottle()),
                "fresh bottle must not be a brewing input"
        );
        helper.succeed();
    }

    /** Leftover vanilla water_bucket / water potion in inventory become graded items. */
    private static void legacyWaterItemRewrite(GameTestHelper helper) {
        ServerPlayer player = mockSurvivalPlayer(helper);
        player.getInventory().setItem(0, new ItemStack(Items.WATER_BUCKET));
        player.getInventory().setItem(1, PotionContents.createItemStack(Items.POTION, Potions.WATER));

        VanillaWaterItemRewriteHandler.rewriteInventory(player);

        helper.assertTrue(
                WaterApi.isGradedWaterBucket(player.getInventory().getItem(0).getItem()),
                "vanilla water_bucket must rewrite to graded bucket"
        );
        helper.assertTrue(
                WaterApi.fromBottleItem(player.getInventory().getItem(1).getItem())
                        == ModWaterQualities.DISTILLED.get(),
                "vanilla water potion must rewrite to distilled bottle"
        );
        helper.succeed();
    }

    /** Distilled quality must never form infinite sources; other grades may. */
    private static void distilledNoInfiniteSource(GameTestHelper helper) {
        helper.assertTrue(
                !GradedFlowingFluid.canFormInfiniteSource(ModWaterQualities.DISTILLED.get()),
                "distilled must not form infinite sources"
        );
        helper.assertTrue(
                GradedFlowingFluid.canFormInfiniteSource(ModWaterQualities.FRESH.get()),
                "fresh must still form infinite sources"
        );
        helper.assertTrue(
                GradedFlowingFluid.canFormInfiniteSource(ModWaterQualities.SALT.get()),
                "salt must still form infinite sources"
        );

        // Two distilled sources in an L should not create a third source in the corner.
        GradedFlowingFluid fluid = ModFluids.graded();
        WaterQuality distilled = ModWaterQualities.DISTILLED.get();
        BlockPos a = new BlockPos(1, 2, 1);
        BlockPos b = new BlockPos(2, 2, 1);
        BlockPos corner = new BlockPos(1, 2, 2);
        helper.setBlock(a.below(), Blocks.STONE.defaultBlockState());
        helper.setBlock(b.below(), Blocks.STONE.defaultBlockState());
        helper.setBlock(corner.below(), Blocks.STONE.defaultBlockState());
        helper.setBlock(a, fluid.sourceOf(distilled, false).createLegacyBlock());
        helper.setBlock(b, fluid.sourceOf(distilled, false).createLegacyBlock());
        helper.setBlock(corner, Blocks.AIR.defaultBlockState());

        // Wait until flowing reaches the corner, then assert it never becomes a source.
        helper.succeedWhen(() -> {
            FluidState cornerFluid = helper.getLevel().getFluidState(helper.absolutePos(corner));
            helper.assertTrue(!cornerFluid.isEmpty(), "distilled must flow into the corner");
            helper.assertTrue(!cornerFluid.isSource(), "distilled corner must not become an infinite source");
        });
    }

    /**
     * Fresh pools: grace ∝ size; after grace a size-1 pool stagnates; age below grace stays fresh;
     * grace formula matches config.
     */
    private static void freshPoolStagnation(GameTestHelper helper) {
        helper.assertTrue(
                FreshWaterStagnator.graceTicksForSize(1) == FreshWaterStagnator.graceTicksPerSource(),
                "size-1 grace must equal ticks-per-source"
        );
        helper.assertTrue(
                FreshWaterStagnator.graceTicksForSize(3) == 3L * FreshWaterStagnator.graceTicksPerSource(),
                "grace must scale with pool size"
        );
        helper.assertTrue(
                !FreshWaterStagnator.shouldStagnate(FreshWaterStagnator.threshold(), Long.MAX_VALUE / 4),
                "pools at threshold must never stagnate"
        );

        ServerLevel level = helper.getLevel();
        GradedFlowingFluid fluid = ModFluids.graded();
        WaterQuality fresh = ModWaterQualities.FRESH.get();
        WaterQuality stagnant = ModWaterQualities.STAGNANT.get();

        BlockPos single = new BlockPos(1, 2, 1);
        helper.setBlock(single.below(), Blocks.STONE.defaultBlockState());
        helper.setBlock(single, fluid.sourceOf(fresh, false).createLegacyBlock());

        long now = level.getGameTime();
        FreshPoolDecayData.get(level).setFirstSeen(helper.absolutePos(single), now);
        helper.assertTrue(
                !FreshWaterStagnator.processPoolAt(level, helper.absolutePos(single)),
                "fresh pool within grace must not stagnate yet"
        );
        helper.assertTrue(
                WaterApi.fromFluidState(level.getFluidState(helper.absolutePos(single))) == fresh,
                "still fresh inside grace"
        );

        FreshPoolDecayData.get(level).setFirstSeen(
                helper.absolutePos(single),
                now - FreshWaterStagnator.graceTicksForSize(1)
        );
        helper.assertTrue(
                FreshWaterStagnator.processPoolAt(level, helper.absolutePos(single)),
                "size-1 pool past grace must stagnate"
        );
        helper.assertTrue(
                WaterApi.fromFluidState(level.getFluidState(helper.absolutePos(single))) == stagnant,
                "source must become stagnant"
        );

        // Two-source pool: grace is 2× — age of 1× must not convert.
        BlockPos a = new BlockPos(1, 2, 3);
        BlockPos b = new BlockPos(2, 2, 3);
        helper.setBlock(a.below(), Blocks.STONE.defaultBlockState());
        helper.setBlock(b.below(), Blocks.STONE.defaultBlockState());
        helper.setBlock(a, fluid.sourceOf(fresh, false).createLegacyBlock());
        helper.setBlock(b, fluid.sourceOf(fresh, false).createLegacyBlock());
        BlockPos absA = helper.absolutePos(a);
        BlockPos absB = helper.absolutePos(b);
        long pairGrace = FreshWaterStagnator.graceTicksForSize(2);
        FreshPoolDecayData.get(level).setFirstSeen(absA, now - pairGrace + 1);
        FreshPoolDecayData.get(level).setFirstSeen(absB, now - pairGrace + 1);
        helper.assertTrue(
                !FreshWaterStagnator.processPoolAt(level, absA),
                "size-2 pool must wait full 2× grace"
        );

        FreshPoolDecayData.get(level).setFirstSeen(absA, now - pairGrace);
        FreshPoolDecayData.get(level).setFirstSeen(absB, now - pairGrace);
        helper.assertTrue(
                FreshWaterStagnator.processPoolAt(level, absA),
                "size-2 pool past 2× grace must stagnate"
        );
        helper.assertTrue(
                WaterApi.fromFluidState(level.getFluidState(absA)) == stagnant
                        && WaterApi.fromFluidState(level.getFluidState(absB)) == stagnant,
                "both sources in the pool must stagnate together"
        );
        helper.succeed();
    }

    /**
     * Still accepts graded fluid via {@link Capabilities.Fluid#BLOCK} and materializes a bucket
     * (Create/pipe track without requiring Create on 26.1).
     */
    private static void machineFluidInput(GameTestHelper helper) {
        BlockPos rel = new BlockPos(1, 2, 1);
        helper.setBlock(rel, ModBlocks.WATER_STILL.get().defaultBlockState());
        WaterStillBlockEntity still = helper.getBlockEntity(rel, WaterStillBlockEntity.class);
        WaterQuality fresh = ModWaterQualities.FRESH.get();

        var handler = helper.getLevel().getCapability(
                Capabilities.Fluid.BLOCK,
                helper.absolutePos(rel),
                null
        );
        helper.assertTrue(handler != null, "still must expose Fluid.BLOCK capability");

        try (Transaction tx = Transaction.openRoot()) {
            int inserted = handler.insert(GradedFluids.resource(fresh), GradedFluids.BUCKET_MB, tx);
            helper.assertTrue(inserted == GradedFluids.BUCKET_MB, "must insert 1000mB fresh, got " + inserted);
            tx.commit();
        }

        try (Transaction tx = Transaction.openRoot()) {
            int rejected = handler.insert(
                    GradedFluids.resource(ModWaterQualities.DISTILLED.get()),
                    GradedFluids.BUCKET_MB,
                    tx
            );
            helper.assertTrue(rejected == 0, "distilled must not enter still fluid input");
            tx.commit();
        }

        WaterStillBlockEntity.serverTick(
                helper.getLevel(),
                helper.absolutePos(rel),
                helper.getBlockState(rel),
                still
        );
        ItemStack waterSlot = still.getItem(WaterStillBlockEntity.SLOT_WATER);
        helper.assertTrue(
                WaterApi.fromBucketItem(waterSlot.getItem()) == fresh,
                "fluid input must become a fresh water bucket, got " + waterSlot
        );
        helper.succeed();
    }

    /** Graded water participates in lily / frogspawn / bubble-column fluid tags. */
    private static void gradedWaterVanillaTags(GameTestHelper helper) {
        FluidState source = ModFluids.graded().sourceOf(ModWaterQualities.FRESH.get(), false);
        helper.assertTrue(source.is(net.minecraft.tags.FluidTags.WATER), "graded must be #minecraft:water");
        helper.assertTrue(
                source.is(net.minecraft.tags.FluidTags.SUPPORTS_LILY_PAD),
                "graded source must support lily pads"
        );
        helper.assertTrue(
                source.is(net.minecraft.tags.FluidTags.SUPPORTS_FROGSPAWN),
                "graded source must support frogspawn"
        );
        helper.assertTrue(
                source.is(net.minecraft.tags.FluidTags.BUBBLE_COLUMN_CAN_OCCUPY),
                "graded source must allow bubble columns"
        );
        helper.assertTrue(
                ModBlocks.GRADED_ICE.get().defaultBlockState().is(net.minecraft.tags.BlockTags.SUPPORTS_LILY_PAD),
                "graded_ice must support lily pads"
        );
        helper.succeed();
    }

    private static final class CancelDrinkListener {
        @SubscribeEvent
        public void onDrink(PlayerDrinkEvent event) {
            event.setCanceled(true);
        }
    }
}
