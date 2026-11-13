package com.example.realismsurvival.clinic;

import com.example.realismsurvival.RealismSurvivalConfig;
import com.example.realismsurvival.compat.shop.SgQShopCurrencyBridge;
import com.example.realismsurvival.component.DailyBoardData;
import com.example.realismsurvival.component.SurvivalAttachments;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class ClinicService {

    private ClinicService() {}

    public static long overworldDay(ServerPlayer player) {
        ServerLevel overworld = player.level().getServer().overworld();
        long clock = overworld.getOverworldClockTime();
        return Math.floorDiv(clock, 24_000L);
    }

    public static DailyBoardData syncedBoard(ServerPlayer player) {
        DailyBoardData data = SurvivalAttachments.getDailyBoard(player).forDay(overworldDay(player));
        SurvivalAttachments.setDailyBoard(player, data);
        return data;
    }

    /** @return true if treatment applied and gold taken */
    public static boolean treat(ServerPlayer player, ClinicTreatment treatment) {
        if (!SgQShopCurrencyBridge.walletAvailable()) {
            player.sendSystemMessage(Component.translatable("message.realismsurvival.clinic.no_wallet"), true);
            return false;
        }
        DailyBoardData board = syncedBoard(player);
        int used = board.clinicUsesOf(treatment.id());
        if (used >= treatment.dailyLimit()) {
            player.sendSystemMessage(
                    Component.translatable(
                            "message.realismsurvival.clinic.daily_limit",
                            Component.translatable("clinic.realismsurvival." + treatment.id()),
                            treatment.dailyLimit()),
                    true);
            return false;
        }
        int cost = treatment.cost();
        if (!SgQShopCurrencyBridge.hasBalance(player, cost)) {
            player.sendSystemMessage(
                    Component.translatable("message.realismsurvival.clinic.insufficient", cost), true);
            return false;
        }
        if (!SgQShopCurrencyBridge.withdraw(player, cost)) {
            player.sendSystemMessage(
                    Component.translatable("message.realismsurvival.clinic.insufficient", cost), true);
            return false;
        }
        treatment.apply(player);
        SurvivalAttachments.setDailyBoard(player, board.withClinicUse(treatment.id(), used + 1));
        player.sendSystemMessage(
                Component.translatable(
                        "message.realismsurvival.clinic.done",
                        Component.translatable("clinic.realismsurvival." + treatment.id()),
                        cost),
                true);
        return true;
    }

    /** Pays daily-board gold if under the per-day claim cap. */
    public static boolean claimDailyBoard(ServerPlayer player, int coins) {
        if (coins <= 0) {
            return false;
        }
        DailyBoardData board = syncedBoard(player);
        int limit = RealismSurvivalConfig.DAILY_BOARD_CLAIM_LIMIT.getAsInt();
        if (board.boardClaims() >= limit) {
            player.sendSystemMessage(
                    Component.translatable("message.realismsurvival.daily.limit", limit), true);
            return false;
        }
        if (!SgQShopCurrencyBridge.walletAvailable()) {
            player.sendSystemMessage(Component.translatable("message.realismsurvival.clinic.no_wallet"), true);
            return false;
        }
        if (!SgQShopCurrencyBridge.deposit(player, coins)) {
            return false;
        }
        SurvivalAttachments.setDailyBoard(player, board.withBoardClaims(board.boardClaims() + 1));
        player.sendSystemMessage(
                Component.translatable(
                        "message.realismsurvival.daily.claimed",
                        coins,
                        board.boardClaims() + 1,
                        limit),
                true);
        return true;
    }
}
