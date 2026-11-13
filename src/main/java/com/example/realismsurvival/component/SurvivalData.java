package com.example.realismsurvival.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;

/**
 * Immutable per-player survival state.
 *
 * <p>Design rules (see REALISM_SURVIVAL_SPEC.md §5):
 * <ul>
 *   <li>This is a <b>value object</b>. Never mutate; always derive a new instance via the {@code with*} / {@code add*} helpers
 *       and store it back with {@code player.setData(SurvivalAttachments.SURVIVAL_DATA, newValue)}.
 *       Calling {@code setData} is what triggers NeoForge's automatic client sync.</li>
 *   <li>The compact constructor clamps every field, so an instance can never hold an out-of-range value
 *       regardless of where it came from (disk, network, or game logic).</li>
 *   <li>The logical server is the single source of truth. Clients only read this record for rendering.</li>
 * </ul>
 *
 * @param thirst      hydration level in points, {@value #THIRST_MIN}..{@value #THIRST_MAX}. 100 = fully hydrated.
 * @param temperature core body temperature in °C, {@value #TEMPERATURE_MIN}..{@value #TEMPERATURE_MAX}. 36.5 = normal.
 */
public record SurvivalData(float thirst, float temperature) {

    // ---- Thirst ----------------------------------------------------------------------------------------------------
    public static final float THIRST_MIN = 0.0F;
    public static final float THIRST_MAX = 100.0F;
    public static final float THIRST_DEFAULT = 100.0F;
    /** Thirst is displayed on a vanilla-style 20-point scale (10 icons × 2 halves). */
    public static final int THIRST_DISPLAY_POINTS = 20;

    // ---- Temperature -----------------------------------------------------------------------------------------------
    public static final float TEMPERATURE_MIN = 30.0F;
    public static final float TEMPERATURE_MAX = 42.0F;
    public static final float TEMPERATURE_DEFAULT = 36.5F;
    /** Below this the player is considered hypothermic (see spec §9.3). */
    public static final float TEMPERATURE_HYPOTHERMIA = 35.0F;
    /** Above this the player is considered hyperthermic (see spec §9.3). */
    public static final float TEMPERATURE_HYPERTHERMIA = 38.5F;

    /** The value every new player starts with and the value restored when data is missing or corrupt. */
    public static final SurvivalData DEFAULT = new SurvivalData(THIRST_DEFAULT, TEMPERATURE_DEFAULT);

    // ---- Serialization ---------------------------------------------------------------------------------------------

    /**
     * Disk codec. Field ranges are enforced twice: by {@code floatRange} during decode (rejects corrupt saves loudly)
     * and by the compact constructor (defensive clamp for everything else).
     */
    public static final Codec<SurvivalData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.floatRange(THIRST_MIN, THIRST_MAX)
                    .optionalFieldOf("thirst", THIRST_DEFAULT)
                    .forGetter(SurvivalData::thirst),
            Codec.floatRange(TEMPERATURE_MIN, TEMPERATURE_MAX)
                    .optionalFieldOf("temperature", TEMPERATURE_DEFAULT)
                    .forGetter(SurvivalData::temperature)
    ).apply(instance, SurvivalData::new));

    /** {@link MapCodec} view required by {@code AttachmentType.Builder#serialize(MapCodec)}. */
    public static final MapCodec<SurvivalData> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.floatRange(THIRST_MIN, THIRST_MAX)
                    .optionalFieldOf("thirst", THIRST_DEFAULT)
                    .forGetter(SurvivalData::thirst),
            Codec.floatRange(TEMPERATURE_MIN, TEMPERATURE_MAX)
                    .optionalFieldOf("temperature", TEMPERATURE_DEFAULT)
                    .forGetter(SurvivalData::temperature)
    ).apply(instance, SurvivalData::new));

    /** Network codec (server → owning client). 8 bytes per sync. */
    public static final StreamCodec<ByteBuf, SurvivalData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, SurvivalData::thirst,
            ByteBufCodecs.FLOAT, SurvivalData::temperature,
            SurvivalData::new
    );

    /** Compact constructor: unconditional clamp so invariants hold for every instance. */
    public SurvivalData {
        thirst = Mth.clamp(thirst, THIRST_MIN, THIRST_MAX);
        temperature = Mth.clamp(temperature, TEMPERATURE_MIN, TEMPERATURE_MAX);
    }

    // ---- Derivation helpers (return new instances) -------------------------------------------------------------------

    public SurvivalData withThirst(float newThirst) {
        return new SurvivalData(newThirst, temperature);
    }

    public SurvivalData withTemperature(float newTemperature) {
        return new SurvivalData(thirst, newTemperature);
    }

    /** @param delta positive to hydrate, negative to drain. */
    public SurvivalData addThirst(float delta) {
        return withThirst(thirst + delta);
    }

    /** @param delta positive to warm up, negative to cool down. */
    public SurvivalData addTemperature(float delta) {
        return withTemperature(temperature + delta);
    }

    /**
     * Food/drink temperature regulation: move toward {@link #TEMPERATURE_DEFAULT}, never past it.
     *
     * <ul>
     *   <li>Warming ({@code delta > 0}): raises only while below normal, by at most {@code delta}.</li>
     *   <li>Cooling ({@code delta < 0}): lowers only while above normal, by at most {@code |delta|}.</li>
     *   <li>Already on the helpful side of normal: no change (no overshoot / stack abuse).</li>
     * </ul>
     */
    public SurvivalData applyTemperatureTowardNormal(float delta) {
        if (delta == 0.0F) {
            return this;
        }
        float normal = TEMPERATURE_DEFAULT;
        if (delta > 0.0F) {
            if (temperature >= normal) {
                return this;
            }
            return withTemperature(temperature + Math.min(delta, normal - temperature));
        }
        if (temperature <= normal) {
            return this;
        }
        float cool = -delta;
        return withTemperature(temperature - Math.min(cool, temperature - normal));
    }

    // ---- Derived state -----------------------------------------------------------------------------------------------

    /**
     * Thirst converted to the vanilla 20-point HUD scale.
     *
     * <p>Uses {@code ceil} so the bar only shows "empty" when thirst is truly 0; any residual hydration keeps at
     * least a half icon visible, which is the feedback players expect from hardcore meters.
     *
     * @return 0..20 inclusive
     */
    public int thirstPoints() {
        float scaled = thirst / THIRST_MAX * THIRST_DISPLAY_POINTS;
        return Mth.clamp(Mth.ceil(scaled), 0, THIRST_DISPLAY_POINTS);
    }

    /**
     * Body temperature on the same 0..20 half-icon scale as hunger/thirst.
     * {@link #TEMPERATURE_DEFAULT} maps to exactly 10 (five full diamonds — the middle).
     */
    public int temperaturePoints() {
        if (temperature <= TEMPERATURE_DEFAULT) {
            float span = TEMPERATURE_DEFAULT - TEMPERATURE_MIN;
            float t = span <= 0.0F ? 0.0F : (temperature - TEMPERATURE_MIN) / span;
            return Mth.clamp(Math.round(t * 10.0F), 0, 10);
        }
        float span = TEMPERATURE_MAX - TEMPERATURE_DEFAULT;
        float t = span <= 0.0F ? 1.0F : (temperature - TEMPERATURE_DEFAULT) / span;
        return Mth.clamp(10 + Math.round(t * 10.0F), 10, 20);
    }

    public boolean isDehydrated() {
        return thirst <= THIRST_MIN;
    }

    public boolean isHypothermic() {
        return temperature < TEMPERATURE_HYPOTHERMIA;
    }

    public boolean isHyperthermic() {
        return temperature > TEMPERATURE_HYPERTHERMIA;
    }
}
