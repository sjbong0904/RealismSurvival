package com.example.rsmedicine;

import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;

/**
 * Low-chance bonus buff after finishing a medicine item or a drinkable potion (not splash/lingering).
 */
final class MedicineBonusHandler {

    private static final float BONUS_CHANCE = 0.05F;

    private static final List<Holder<MobEffect>> BONUSES = List.of(
            MobEffects.REGENERATION,
            MobEffects.RESISTANCE,
            MobEffects.ABSORPTION,
            MobEffects.SPEED
    );

    private MedicineBonusHandler() {}

    @SubscribeEvent
    static void onUseFinish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ItemStack stack = event.getItem();
        if (stack.getItem() instanceof MedicineItem) {
            // MedicineItem already rolls in finishUsingItem to avoid double-roll when both fire.
            return;
        }
        if (!isDrinkablePotion(stack)) {
            return;
        }
        tryRollBonus(player);
    }

    static void tryRollBonus(ServerPlayer player) {
        RandomSource random = player.getRandom();
        if (random.nextFloat() >= BONUS_CHANCE) {
            return;
        }
        Holder<MobEffect> effect = BONUSES.get(random.nextInt(BONUSES.size()));
        int ticks = MedicineTiming.randomDurationTicks(random);
        player.addEffect(new MobEffectInstance(effect, ticks, 0));
    }

    private static boolean isDrinkablePotion(ItemStack stack) {
        if (stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION) || stack.is(Items.TIPPED_ARROW)) {
            return false;
        }
        if (stack.is(Items.POTION)) {
            return true;
        }
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        return contents != null && !stack.is(Items.SPLASH_POTION) && !stack.is(Items.LINGERING_POTION);
    }
}
