package com.example.realismsurvival.client;

import com.example.realismsurvival.api.compat.CuriosAccess;
import com.example.realismsurvival.registry.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/** Hand or Curios-belt visibility gates for the instruments HUD. */
public final class InstrumentAccess {

    /** Serene Seasons calendar — season/date HUD gate (RS no longer ships its own calendar). */
    private static final Identifier SERENE_CALENDAR_ID =
            Identifier.fromNamespaceAndPath("sereneseasons", "calendar");

    private InstrumentAccess() {}

    public static boolean hasThermometer(Player player) {
        return holdingOrCurio(player, ModItems.THERMOMETER.get());
    }

    public static boolean hasClock(Player player) {
        return holdingOrCurio(player, Items.CLOCK);
    }

    public static boolean hasCalendar(Player player) {
        return BuiltInRegistries.ITEM
                .getOptional(SERENE_CALENDAR_ID)
                .filter(item -> item != Items.AIR)
                .map(item -> holdingOrCurio(player, item))
                .orElse(false);
    }

    private static boolean holdingOrCurio(Player player, Item item) {
        if (player.getMainHandItem().is(item) || player.getOffhandItem().is(item)) {
            return true;
        }
        if (!CuriosAccess.isLoaded()) {
            return false;
        }
        final boolean[] found = {false};
        CuriosAccess.forEachEquipped(player, stack -> {
            if (!found[0] && stack.is(item)) {
                found[0] = true;
            }
        });
        return found[0];
    }
}
