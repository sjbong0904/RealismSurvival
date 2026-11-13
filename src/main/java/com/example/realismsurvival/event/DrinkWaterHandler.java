package com.example.realismsurvival.event;

import com.example.realismsurvival.RealismSurvival;
import com.example.realismsurvival.RealismSurvivalConfig;
import com.example.realismsurvival.data.DrinkDefinition;
import com.example.realismsurvival.data.SurvivalDataManager;
import com.example.realismsurvival.api.water.WaterApi;
import com.example.realismsurvival.api.water.WaterQuality;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Shift + empty-hand right-click on a water source drinks from the world (T-04).
 *
 * <p>Emergency path: graded sources use a fraction of the bottle drink datapack entry
 * ({@code worldDrinkBottleFraction}), so bottles / filters / stills stay more efficient.
 * Ungraded fallback uses {@code drink/raw_water.json}. Quality status effects stay full strength.
 */
@EventBusSubscriber(modid = RealismSurvival.MODID)
public final class DrinkWaterHandler {

    private DrinkWaterHandler() {}

    public static final String RAW_WATER_DRINK = "raw_water";
    private static final String COOLDOWN_TAG = "RealismSurvivalWorldDrinkGameTime";
    private static final int COOLDOWN_TICKS = 20;

    @SubscribeEvent
    static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (tryDrink(event.getEntity(), event.getLevel(), event.getHand())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    @SubscribeEvent
    static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (tryDrink(event.getEntity(), event.getLevel(), event.getHand())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    private static boolean tryDrink(Player player, Level level, InteractionHand hand) {
        if (!RealismSurvivalConfig.THIRST_ENABLED.getAsBoolean()) {
            return false;
        }
        if (RealismSurvivalConfig.CREATIVE_BYPASS.getAsBoolean() && (player.isCreative() || player.isSpectator())) {
            return false;
        }
        if (!player.isShiftKeyDown() || hand != InteractionHand.MAIN_HAND) {
            return false;
        }
        ItemStack held = player.getItemInHand(hand);
        if (!held.isEmpty()) {
            return false;
        }

        BlockHitResult hit = Item.getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() != HitResult.Type.BLOCK) {
            return false;
        }
        BlockPos pos = hit.getBlockPos();
        if (!level.mayInteract(player, pos)) {
            return false;
        }

        FluidState fluidState = level.getFluidState(pos);
        if (!WaterApi.isInteractableWaterSource(fluidState)) {
            return false;
        }

        if (level.isClientSide()) {
            return true;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }

        long now = level.getGameTime();
        long last = serverPlayer.getPersistentData().getLongOr(COOLDOWN_TAG, Long.MIN_VALUE);
        if (now - last < COOLDOWN_TICKS) {
            return true;
        }

        WaterQuality graded = WaterQuality.fromFluidState(fluidState);
        WaterQuality quality = graded != null ? graded : WaterQuality.fromBiome(level.getBiome(pos));

        float thirst;
        float temp;
        if (graded != null) {
            DrinkDefinition bottle = SurvivalDataManager.drink(graded.bottleItem().builtInRegistryHolder()).orElse(null);
            float fraction = (float) RealismSurvivalConfig.WORLD_DRINK_BOTTLE_FRACTION.getAsDouble();
            thirst = (bottle != null ? bottle.thirstRestore() : 0.0F) * fraction;
            temp = (bottle != null ? bottle.temperatureDelta() : 0.0F) * fraction;
        } else {
            DrinkDefinition raw = SurvivalDataManager.drinkById(RealismSurvival.id(RAW_WATER_DRINK)).orElse(null);
            thirst = raw != null ? raw.thirstRestore() : 4.0F;
            temp = raw != null ? raw.temperatureDelta() : 0.0F;
        }

        // Cancelled PlayerDrinkEvent: no thirst, no sound, no cooldown (still claim the click).
        if (!SurvivalTickHandler.applyDrink(serverPlayer, ItemStack.EMPTY, quality, thirst, temp)) {
            return true;
        }

        level.playSound(
                null,
                serverPlayer.getX(),
                serverPlayer.getY(),
                serverPlayer.getZ(),
                SoundEvents.GENERIC_DRINK,
                SoundSource.PLAYERS,
                0.5F,
                serverPlayer.getRandom().nextFloat() * 0.1F + 0.9F
        );
        serverPlayer.getPersistentData().putLong(COOLDOWN_TAG, now);
        return true;
    }
}
