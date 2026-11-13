package com.example.realismsurvival.water;

import java.util.function.Supplier;
import net.minecraft.world.item.alchemy.PotionBrewing;
import org.jspecify.annotations.Nullable;

/** Bound from the client entrypoint so common brewing code never loads client classes on a dedicated server. */
public final class ClientBrewingBridge {

    private static Supplier<@Nullable PotionBrewing> clientBrewing = () -> null;

    private ClientBrewingBridge() {}

    public static void bind(Supplier<@Nullable PotionBrewing> supplier) {
        clientBrewing = supplier != null ? supplier : () -> null;
    }

    public static @Nullable PotionBrewing get() {
        return clientBrewing.get();
    }
}
