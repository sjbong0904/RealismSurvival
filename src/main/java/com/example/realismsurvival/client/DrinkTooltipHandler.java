package com.example.realismsurvival.client;

import com.example.realismsurvival.RealismSurvival;
import com.example.realismsurvival.RealismSurvivalConfig;
import com.example.realismsurvival.data.DrinkDefinition;
import com.example.realismsurvival.api.water.WaterApi;
import com.example.realismsurvival.api.water.WaterQuality;
import com.example.realismsurvival.water.BucketFill;
import com.example.realismsurvival.water.ModEnchantments;
import com.example.realismsurvival.water.SnowBucketItem;
import com.example.realismsurvival.water.WaterTooltips;
import com.mojang.datafixers.util.Either;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * Edible / drinkable tooltips: thirst droplets + temperature icons + graded-water effects.
 * Hunger / saturation icons are provided by AppleSkin (required on CLIENT).
 * Food/consumable potion effects are left to vanilla / other mods (no duplicate listing).
 *
 * <p>Stable order: name → AppleSkin food row (if any) → thirst → temp icons → …rest
 */
@EventBusSubscriber(modid = RealismSurvival.MODID, value = Dist.CLIENT)
public final class DrinkTooltipHandler {

    private DrinkTooltipHandler() {}

    /**
     * LOW so AppleSkin inserts first; thirst + temp go immediately after that row
     * (or right under the name when AppleSkin has no food row).
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    static void onGatherTooltipComponents(RenderTooltipEvent.GatherComponents event) {
        boolean thirstOn = RealismSurvivalConfig.THIRST_ENABLED.getAsBoolean();
        boolean tempOn = RealismSurvivalConfig.TEMPERATURE_ENABLED.getAsBoolean();
        if (!thirstOn && !tempOn) {
            return;
        }

        ItemStack stack = event.getItemStack();
        if (!isEdibleOrSipBucket(stack)) {
            return;
        }

        Player player = Minecraft.getInstance().player;
        DrinkStats drinkStats = resolveDrinkStats(stack, player);
        if ((!drinkStats.hasDrink || drinkStats.thirst == 0.0F) && drinkStats.temp == 0.0F) {
            return;
        }

        var elements = event.getTooltipElements();
        int insertAt = indexAfterAppleSkinOrName(elements);

        if (thirstOn && drinkStats.hasDrink && drinkStats.thirst != 0.0F) {
            elements.add(insertAt++, Either.right(new ThirstStatsTooltip(drinkStats.thirst)));
        }
        if (tempOn && drinkStats.temp != 0.0F) {
            elements.add(insertAt, Either.right(new TemperatureStatsTooltip(drinkStats.temp)));
        }
    }

    @SubscribeEvent
    static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!isEdibleOrSipBucket(stack)) {
            return;
        }

        List<Component> tip = event.getToolTip();
        float tickRate = event.getContext().tickRate();
        DrinkStats drinkStats = resolveDrinkStats(stack, event.getEntity());

        // Only graded-water quality effects — never re-list FOOD/CONSUMABLE effects (FD/vanilla already do).
        if (drinkStats.quality != null && !stack.has(DataComponents.POTION_CONTENTS)) {
            List<MobEffectInstance> effects =
                    WaterTooltips.scaleEffectDurations(drinkStats.quality.drinkEffects(), drinkStats.effectScale);
            WaterTooltips.appendEffectTooltip(effects, tip::add, tickRate);
        }

        if (WaterApi.isGradedWaterBottle(stack.getItem())) {
            WaterTooltips.appendBottleUseHint(tip::add);
        } else if (WaterApi.isGradedWaterBucket(stack.getItem())) {
            WaterTooltips.appendBucketUseHint("tooltip.realismsurvival.water.bucket_use", stack, tip::add);
        } else if (WaterApi.isGradedIceBucket(stack.getItem())) {
            WaterTooltips.appendBucketUseHint("tooltip.realismsurvival.water.ice_bucket_use", stack, tip::add);
        } else if (stack.getItem() instanceof SnowBucketItem || stack.is(Items.POWDER_SNOW_BUCKET)) {
            WaterTooltips.appendBucketUseHint("tooltip.realismsurvival.water.snow_bucket_use", stack, tip::add);
        }
    }

    /**
     * After AppleSkin's food-value {@link TooltipComponent} when present; otherwise right after the name.
     */
    private static int indexAfterAppleSkinOrName(List<Either<FormattedText, TooltipComponent>> elements) {
        for (int i = 0; i < elements.size(); i++) {
            TooltipComponent component = elements.get(i).right().orElse(null);
            if (component != null && isAppleSkinFoodTooltip(component)) {
                return i + 1;
            }
        }
        return Math.min(1, elements.size());
    }

    /** Avoid compile dependency on AppleSkin; match by class name. */
    private static boolean isAppleSkinFoodTooltip(TooltipComponent component) {
        String name = component.getClass().getName();
        return name.startsWith("squeek.appleskin.") && name.contains("FoodTooltip");
    }

    private static DrinkStats resolveDrinkStats(ItemStack stack, @org.jspecify.annotations.Nullable Player player) {
        float thirst = 0.0F;
        float temp = 0.0F;
        float effectScale = 1.0F;
        boolean hasDrink = false;
        WaterQuality quality = WaterQuality.fromBucketItem(stack.getItem());

        if (quality != null) {
            effectScale = BucketFill.sipMultiplier(stack);
            DrinkDefinition drink = ClientSurvivalData.drink(quality.bottleItem().builtInRegistryHolder()).orElse(null);
            if (drink != null) {
                thirst = drink.thirstRestore() * effectScale;
                temp = drink.temperatureDelta() * effectScale;
                hasDrink = true;
            }
        } else if (WaterApi.isGradedIceBucket(stack.getItem())) {
            quality = WaterApi.fromIceBucketItem(stack.getItem());
            effectScale = BucketFill.sipMultiplier(stack);
            // Thirst from bottle (same ratio as liquid bucket); cooling from ice entry.
            DrinkDefinition bottle = quality != null
                    ? ClientSurvivalData.drink(quality.bottleItem().builtInRegistryHolder()).orElse(null)
                    : null;
            DrinkDefinition ice = ClientSurvivalData.drink(stack.typeHolder()).orElse(null);
            if (bottle != null) {
                thirst = bottle.thirstRestore() * effectScale;
                hasDrink = true;
            }
            if (ice != null) {
                temp = ice.temperatureDelta() * effectScale;
                hasDrink = true;
            } else if (bottle != null) {
                temp = bottle.temperatureDelta() * effectScale;
            }
        } else if (stack.getItem() instanceof SnowBucketItem || stack.is(Items.POWDER_SNOW_BUCKET)) {
            effectScale = BucketFill.sipMultiplier(stack);
            // Preview mid-band random sip (actual roll varies).
            thirst = 4.5F * effectScale;
            temp = -0.45F * effectScale;
            hasDrink = true;
        } else {
            DrinkDefinition drink = ClientSurvivalData.drink(stack.typeHolder()).orElse(null);
            if (drink != null) {
                thirst = drink.thirstRestore();
                temp = drink.temperatureDelta();
                hasDrink = true;
            }
            quality = WaterQuality.fromBottleItem(stack.getItem());
        }

        if (player != null) {
            temp += ModEnchantments.drinkTemperatureDelta(stack, player.registryAccess());
        }

        return new DrinkStats(thirst, temp, effectScale, hasDrink, quality);
    }

    private static boolean isEdibleOrSipBucket(ItemStack stack) {
        if (WaterApi.isGradedWaterBucket(stack.getItem())
                || WaterApi.isGradedIceBucket(stack.getItem())
                || stack.getItem() instanceof SnowBucketItem
                || stack.is(Items.POWDER_SNOW_BUCKET)) {
            return true;
        }
        return stack.has(DataComponents.FOOD) || stack.has(DataComponents.CONSUMABLE);
    }

    private record DrinkStats(
            float thirst,
            float temp,
            float effectScale,
            boolean hasDrink,
            WaterQuality quality
    ) {}
}
