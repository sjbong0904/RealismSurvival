package com.example.realismsurvival.command;

import com.example.realismsurvival.RealismSurvival;
import com.example.realismsurvival.clinic.ClinicService;
import com.example.realismsurvival.compat.shop.SgQShopCurrencyBridge;
import com.example.realismsurvival.component.SurvivalAttachments;
import com.example.realismsurvival.component.SurvivalData;
import com.example.realismsurvival.environment.TemperatureEvaluator;
import com.example.realismsurvival.event.WaterWorldHandler;
import com.example.realismsurvival.water.FreshPoolDecayData;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.JsonOps;
import java.util.Collection;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Debug / balancing commands (spec T-01).
 *
 * <pre>
 * /realism get
 * /realism stats
 * /realism set thirst &lt;0..100&gt;
 * /realism set temperature &lt;30..42&gt;
 * /realism coins give &lt;players&gt; &lt;amount&gt;
 * /realism daily claim &lt;amount&gt;   — used by FTB daily-board rewards (game-day capped)
 * </pre>
 */
@EventBusSubscriber(modid = RealismSurvival.MODID)
public final class RealismCommands {

    private RealismCommands() {}

    @SubscribeEvent
    static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("realism")
                        .then(Commands.literal("get")
                                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                                .executes(RealismCommands::get))
                        .then(Commands.literal("stats")
                                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                                .executes(RealismCommands::stats))
                        .then(Commands.literal("set")
                                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                                .then(Commands.literal("thirst")
                                        .then(Commands.argument("value", FloatArgumentType.floatArg(
                                                        SurvivalData.THIRST_MIN, SurvivalData.THIRST_MAX))
                                                .executes(ctx -> setThirst(ctx, FloatArgumentType.getFloat(ctx, "value")))))
                                .then(Commands.literal("temperature")
                                        .then(Commands.argument("value", FloatArgumentType.floatArg(
                                                        SurvivalData.TEMPERATURE_MIN, SurvivalData.TEMPERATURE_MAX))
                                                .executes(ctx -> setTemperature(ctx, FloatArgumentType.getFloat(ctx, "value"))))))
                        .then(Commands.literal("coins")
                                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                                .then(Commands.literal("give")
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(1.0, 1_000_000.0))
                                                        .executes(RealismCommands::coinsGive))))
                                .then(Commands.literal("get")
                                        .executes(RealismCommands::coinsGet)
                                        .then(Commands.argument("target", EntityArgument.player())
                                                .executes(RealismCommands::coinsGetTarget))))
                        .then(Commands.literal("daily")
                                .then(Commands.literal("claim")
                                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 100_000))
                                                .executes(RealismCommands::dailyClaim)))));
    }

    private static int coinsGive(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
        double amount = DoubleArgumentType.getDouble(ctx, "amount");
        int ok = 0;
        for (ServerPlayer player : targets) {
            if (SgQShopCurrencyBridge.deposit(player, amount)) {
                ok++;
            }
        }
        int paid = ok;
        ctx.getSource().sendSuccess(
                () -> Component.literal("Paid " + amount + " gold to " + paid + " player(s)"), true);
        return ok;
    }

    private static int coinsGet(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        double bal = SgQShopCurrencyBridge.getBalance(player);
        ctx.getSource().sendSuccess(() -> Component.literal("Gold: " + bal), false);
        return 1;
    }

    private static int coinsGetTarget(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "target");
        double bal = SgQShopCurrencyBridge.getBalance(player);
        ctx.getSource().sendSuccess(
                () -> Component.literal(player.getScoreboardName() + " gold: " + bal), false);
        return 1;
    }

    private static int dailyClaim(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        int amount = IntegerArgumentType.getInteger(ctx, "amount");
        return ClinicService.claimDailyBoard(player, amount) ? 1 : 0;
    }

    private static int get(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        SurvivalData data = SurvivalAttachments.get(player);

        String json = SurvivalData.CODEC
                .encodeStart(JsonOps.INSTANCE, data)
                .getOrThrow()
                .toString();

        String ambientNote = "";
        if (player.level() instanceof ServerLevel level) {
            Holder<Biome> biome = level.getBiome(player.blockPosition());
            float ambient = TemperatureEvaluator.computeAmbient(player, level, biome);
            ambientNote = " ambient=" + String.format(java.util.Locale.ROOT, "%.1f", ambient) + "C";
        }

        String message = json + ambientNote;
        ctx.getSource().sendSuccess(() -> Component.literal(message), false);
        return 1;
    }

    /** Load counters for 7.4 / 7.5 sizing decisions; read-only. */
    private static int stats(CommandContext<CommandSourceStack> ctx) {
        StringBuilder out = new StringBuilder("[RS stats]");
        for (ServerLevel level : ctx.getSource().getServer().getAllLevels()) {
            out.append(String.format(
                    java.util.Locale.ROOT,
                    "%n%s: pendingChunks=%d freshPoolTracked=%d",
                    level.dimension().identifier(),
                    WaterWorldHandler.pendingChunks(level.dimension()),
                    FreshPoolDecayData.get(level).trackedCount()));
        }
        out.append(String.format(
                java.util.Locale.ROOT,
                "%ncurrencySync: checks=%d writes=%d players=%d",
                SgQShopCurrencyBridge.syncChecks(),
                SgQShopCurrencyBridge.syncWrites(),
                ctx.getSource().getServer().getPlayerList().getPlayerCount()));
        String message = out.toString();
        ctx.getSource().sendSuccess(() -> Component.literal(message), false);
        return 1;
    }

    private static int setThirst(CommandContext<CommandSourceStack> ctx, float value) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        SurvivalData next = SurvivalAttachments.get(player).withThirst(value);
        SurvivalAttachments.set(player, next);
        ctx.getSource().sendSuccess(
                () -> Component.literal("thirst set to " + next.thirst()),
                true
        );
        return 1;
    }

    private static int setTemperature(CommandContext<CommandSourceStack> ctx, float value) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        SurvivalData next = SurvivalAttachments.get(player).withTemperature(value);
        SurvivalAttachments.set(player, next);
        ctx.getSource().sendSuccess(
                () -> Component.literal("temperature set to " + next.temperature()),
                true
        );
        return 1;
    }
}
