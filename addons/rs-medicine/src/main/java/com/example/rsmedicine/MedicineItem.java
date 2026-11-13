package com.example.rsmedicine;

import com.example.realismsurvival.api.SurvivalApi;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * Usable recovery / cure item. Consume animation is eat-like; no food component.
 * Heal hearts / cure icons are drawn client-side ({@code MedicineTooltipHandler}).
 */
public final class MedicineItem extends Item {

    private static final int USE_TICKS = 32;

    private final MedicineAction action;
    private final String tooltipKey;

    public MedicineItem(MedicineAction action, String tooltipKey, Properties properties) {
        super(properties);
        this.action = action;
        this.tooltipKey = tooltipKey;
    }

    public MedicineAction action() {
        return action;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> tooltip,
            TooltipFlag flag
    ) {
        tooltip.accept(Component.translatable(tooltipKey).withStyle(ChatFormatting.GRAY));
        // Heart / effect-icon rows are added on the client. Text list when any cure lacks an icon.
        if (action.curesAnything() && !action.allCuresHaveIcons()) {
            tooltip.accept(buildCureListLine(action.cureDisplayNames()).withStyle(ChatFormatting.BLUE));
        }
    }

    static MutableComponent buildCureListLine(List<Component> names) {
        MutableComponent joined = Component.empty();
        for (int i = 0; i < names.size(); i++) {
            if (i > 0) {
                joined.append(Component.literal(", "));
            }
            joined.append(names.get(i));
        }
        return Component.translatable("tooltip.rsmedicine.cures_on_use", joined);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return USE_TICKS;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.EAT;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        entity.gameEvent(net.minecraft.world.level.gameevent.GameEvent.EAT);
        entity.playSound(
                SoundEvents.GENERIC_EAT.value(),
                0.6F,
                level.getRandom().triangle(1.0F, 0.1F)
        );

        if (!level.isClientSide() && entity instanceof ServerPlayer player) {
            action.applyCures(player);
            if (action.heals()) {
                float amount = action.healAmount();
                SurvivalApi.allowUnscaledHeal(() -> player.heal(amount));
            }
            MedicineBonusHandler.tryRollBonus(player);
        }

        if (entity instanceof Player player && player.hasInfiniteMaterials()) {
            return stack;
        }
        stack.shrink(1);
        return stack;
    }
}
