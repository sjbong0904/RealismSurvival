package com.example.rsmedicine.client;

import com.example.rsmedicine.MedicineAction;
import com.example.rsmedicine.MedicineItem;
import com.example.rsmedicine.RsMedicine;
import com.mojang.datafixers.util.Either;
import java.util.List;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;

/**
 * Inserts HP heart / effect-icon rows under medicine item descriptions.
 */
@EventBusSubscriber(modid = RsMedicine.MODID, value = Dist.CLIENT)
public final class MedicineTooltipHandler {

    private MedicineTooltipHandler() {}

    @SubscribeEvent(priority = EventPriority.LOW)
    static void onGatherTooltipComponents(RenderTooltipEvent.GatherComponents event) {
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof MedicineItem medicine)) {
            return;
        }
        MedicineAction action = medicine.action();
        List<Either<FormattedText, TooltipComponent>> elements = event.getTooltipElements();
        int insertAt = Math.min(2, elements.size());

        if (action.heals()) {
            elements.add(insertAt++, Either.right(new MedicineHealTooltip(action.healAmount())));
        }
        if (action.allCuresHaveIcons()) {
            var icons = action.cureEffectIcons();
            if (!icons.isEmpty()) {
                elements.add(insertAt, Either.right(new MedicineCureIconsTooltip(icons)));
            }
        }
    }
}
