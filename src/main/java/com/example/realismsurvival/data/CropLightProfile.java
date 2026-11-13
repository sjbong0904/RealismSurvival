package com.example.realismsurvival.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;

/**
 * Data-pack entry: how a crop's growth rate scales with light.
 *
 * <p>Location: {@code data/<namespace>/realismsurvival/crop_light/<name>.json}
 *
 * <pre>{@code
 * {
 *   "crops": "#minecraft:crops",
 *   "metric": "sun",
 *   "bands": [
 *     { "min": 0, "max": 8, "multiplier": 0.35 },
 *     { "min": 9, "max": 15, "multiplier": 1.2 }
 *   ],
 *   "priority": 0
 * }
 * }</pre>
 *
 * <p>{@link LightMetric#SUN} uses sky light minus sky darkening, so underground torch farms do not
 * count as sunlight and night slows sun-loving crops. {@link LightMetric#SKY} ignores time of day
 * (open sky exposure), which suits shade crops that resent canopy gaps.
 *
 * @param crops    crop blocks this profile applies to
 * @param metric   which light value to sample
 * @param bands    inclusive light ranges → growth multipliers (unmatched light → {@code 0})
 * @param priority conflict resolution, highest wins
 */
public record CropLightProfile(HolderSet<Block> crops, LightMetric metric, List<LightBand> bands, int priority)
        implements Prioritized {

    public static final Codec<CropLightProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            RegistryCodecs.homogeneousList(Registries.BLOCK).fieldOf("crops").forGetter(CropLightProfile::crops),
            LightMetric.CODEC.fieldOf("metric").forGetter(CropLightProfile::metric),
            LightBand.CODEC.listOf().fieldOf("bands").forGetter(CropLightProfile::bands),
            Codec.INT.optionalFieldOf("priority", 0).forGetter(CropLightProfile::priority)
    ).apply(instance, CropLightProfile::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CropLightProfile> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.holderSet(Registries.BLOCK), CropLightProfile::crops,
            LightMetric.STREAM_CODEC, CropLightProfile::metric,
            LightBand.STREAM_CODEC.apply(ByteBufCodecs.list()), CropLightProfile::bands,
            ByteBufCodecs.VAR_INT, CropLightProfile::priority,
            CropLightProfile::new
    );

    public boolean matches(Holder<Block> crop) {
        return crops.contains(crop);
    }

    /** Samples the configured light metric at {@code pos}. */
    public int measure(ServerLevel level, BlockPos pos) {
        return metric.measure(level, pos);
    }

    /**
     * Growth multiplier for a measured light level. Ranges not covered by any band yield {@code 0}
     * (no growth) so datapacks must span the levels they care about.
     */
    public float multiplierFor(int lightLevel) {
        for (LightBand band : bands) {
            if (band.contains(lightLevel)) {
                return band.multiplier();
            }
        }
        return 0.0F;
    }

    public float multiplierAt(ServerLevel level, BlockPos pos) {
        return multiplierFor(measure(level, pos));
    }

    /** Inclusive light band mapped to a growth multiplier. */
    public record LightBand(int min, int max, float multiplier) {
        public static final Codec<LightBand> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.intRange(0, 15).fieldOf("min").forGetter(LightBand::min),
                Codec.intRange(0, 15).fieldOf("max").forGetter(LightBand::max),
                Codec.floatRange(0.0F, 10.0F).fieldOf("multiplier").forGetter(LightBand::multiplier)
        ).apply(instance, LightBand::new));

        public static final StreamCodec<ByteBuf, LightBand> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, LightBand::min,
                ByteBufCodecs.VAR_INT, LightBand::max,
                ByteBufCodecs.FLOAT, LightBand::multiplier,
                LightBand::new
        );

        public boolean contains(int light) {
            return light >= min && light <= max;
        }
    }

    /** Which brightness channel feeds {@link #bands}. */
    public enum LightMetric implements StringRepresentable {
        /** Sky light after sky darkening — true daylight. */
        SUN {
            @Override
            public int measure(ServerLevel level, BlockPos pos) {
                return Math.max(0, level.getBrightness(LightLayer.SKY, pos) - level.getSkyDarken());
            }
        },
        /** Sky-access only (ignores time of day). */
        SKY {
            @Override
            public int measure(ServerLevel level, BlockPos pos) {
                return level.getBrightness(LightLayer.SKY, pos);
            }
        },
        /** Block (torch / glowstone) light only. */
        BLOCK {
            @Override
            public int measure(ServerLevel level, BlockPos pos) {
                return level.getBrightness(LightLayer.BLOCK, pos);
            }
        },
        /** Vanilla-style max(sky darkened, block). */
        MAX {
            @Override
            public int measure(ServerLevel level, BlockPos pos) {
                return level.getRawBrightness(pos, 0);
            }
        };

        public static final Codec<LightMetric> CODEC = StringRepresentable.fromEnum(LightMetric::values);

        public static final StreamCodec<ByteBuf, LightMetric> STREAM_CODEC = ByteBufCodecs.STRING_UTF8
                .map(name -> LightMetric.valueOf(name.toUpperCase(Locale.ROOT)), LightMetric::getSerializedName);

        public abstract int measure(ServerLevel level, BlockPos pos);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
