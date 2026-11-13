package com.example.realismsurvival.event;

import com.example.realismsurvival.RealismSurvival;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;

/**
 * Raises default stack sizes for clutter-prone items that already distinguish variants via
 * data components (potion contents, stored enchantments, …).
 *
 * <p>Stacks only merge when components match: Sharpness III books stack together, but not with
 * Protection III. Water bottles stack with water bottles, not with Strength potions.
 *
 * <p><b>Filled buckets stay at vanilla stack size 1</b> (including graded water / ice / snow buckets
 * and {@link Items#WATER_BUCKET} / {@link Items#MILK_BUCKET}). Empty {@link Items#BUCKET} remains 16.
 *
 * <p>Uses {@link ModifyDefaultComponentsEvent} (mod bus) — stack size is an item default, not a
 * per-world server config value.
 */
@EventBusSubscriber(modid = RealismSurvival.MODID)
public final class ItemComponentModifiers {

    private ItemComponentModifiers() {}

    /**
     * Drink bottles / potions (not filled buckets). Matches vanilla {@link Items#HONEY_BOTTLE}.
     * Graded water bottles use this via {@link com.example.realismsurvival.registry.ModItems}.
     */
    public static final int HYDRATION_CONTAINER_STACK = 16;

    /** Enchanted books: identical {@code STORED_ENCHANTMENTS} only. */
    public static final int ENCHANTED_BOOK_STACK = 64;

    @SubscribeEvent
    static void onModifyDefaultComponents(ModifyDefaultComponentsEvent event) {
        event.modify(Items.POTION, builder -> builder.set(DataComponents.MAX_STACK_SIZE, HYDRATION_CONTAINER_STACK));
        // Do not raise WATER_BUCKET / MILK_BUCKET — filled buckets stay stacksTo(1) like vanilla.
        event.modify(Items.ENCHANTED_BOOK, builder -> builder.set(DataComponents.MAX_STACK_SIZE, ENCHANTED_BOOK_STACK));
        // Allow warming / cooling on empty glass bottles (enchanting table + anvil).
        event.modify(Items.GLASS_BOTTLE, builder -> builder.set(
                DataComponents.ENCHANTABLE,
                new net.minecraft.world.item.enchantment.Enchantable(15)
        ));
        // Filter cartridge durability is set on item registration (.durability) — never read
        // ModConfig here; ModifyDefaultComponentsEvent runs before configs are loaded.
        RealismSurvival.LOGGER.info(
                "[RealismSurvival] Stack sizes: potion={}, enchanted_book={}; filled buckets remain 1",
                HYDRATION_CONTAINER_STACK,
                ENCHANTED_BOOK_STACK
        );
    }
}
