package com.example.realismsurvival.api.event;

import com.example.realismsurvival.api.water.WaterQuality;
import java.util.Objects;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.bus.api.Event;

/**
 * Fired after the core picks a default {@link WaterQuality} from biome tags / fallbacks,
 * before that value is used for conversion, scooping, or irrigation biome fallback.
 *
 * <p>Addons may {@link #setQuality} to override. Not cancellable — leave quality unchanged to accept the default.
 */
public final class ResolveWaterQualityEvent extends Event {

    private final Holder<Biome> biome;
    private WaterQuality quality;

    public ResolveWaterQualityEvent(Holder<Biome> biome, WaterQuality quality) {
        this.biome = biome;
        this.quality = quality;
    }

    public Holder<Biome> getBiome() {
        return biome;
    }

    public WaterQuality getQuality() {
        return quality;
    }

    public void setQuality(WaterQuality quality) {
        this.quality = Objects.requireNonNull(quality, "quality");
    }
}
