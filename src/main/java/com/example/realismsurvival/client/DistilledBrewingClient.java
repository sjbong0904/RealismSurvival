package com.example.realismsurvival.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.alchemy.PotionBrewing;
import org.jspecify.annotations.Nullable;

/** Client-only {@link PotionBrewing} lookup for distilled brewing-stand UI / JEI. */
public final class DistilledBrewingClient {

    private DistilledBrewingClient() {}

    public static @Nullable PotionBrewing brewingOrNull() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null ? mc.level.potionBrewing() : null;
    }
}
