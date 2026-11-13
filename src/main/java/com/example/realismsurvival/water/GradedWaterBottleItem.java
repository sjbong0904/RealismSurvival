package com.example.realismsurvival.water;

import com.example.realismsurvival.api.water.WaterQuality;
import java.util.function.Supplier;
import net.minecraft.world.item.Item;

/**
 * Drinkable bottle for a {@link WaterQuality}. Stats / effects / use hint come from
 * {@code DrinkTooltipHandler} so all edibles share one tooltip path.
 *
 * <p>Quality is resolved lazily so item registration does not require the
 * {@code water_quality} registry to be bound yet.
 */
public final class GradedWaterBottleItem extends Item {

    private final Supplier<WaterQuality> quality;

    public GradedWaterBottleItem(Supplier<WaterQuality> quality, Properties properties) {
        super(properties);
        this.quality = quality;
    }

    public WaterQuality quality() {
        return quality.get();
    }
}
