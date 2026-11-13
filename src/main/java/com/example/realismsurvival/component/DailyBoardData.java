package com.example.realismsurvival.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Locale;
import java.util.Map;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Per-player Overworld-day counters for daily board claims and clinic visits. */
public record DailyBoardData(long dayIndex, int boardClaims, Map<String, Integer> clinicUses) {

    public static final DailyBoardData DEFAULT = new DailyBoardData(-1L, 0, Map.of());

    public static final MapCodec<DailyBoardData> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance
            .group(
                    Codec.LONG.fieldOf("day").forGetter(DailyBoardData::dayIndex),
                    Codec.INT.fieldOf("board").forGetter(DailyBoardData::boardClaims),
                    Codec.unboundedMap(Codec.STRING, Codec.INT)
                            .optionalFieldOf("clinic", Map.of())
                            .forGetter(DailyBoardData::clinicUses))
            .apply(instance, DailyBoardData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, DailyBoardData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG,
            DailyBoardData::dayIndex,
            ByteBufCodecs.VAR_INT,
            DailyBoardData::boardClaims,
            ByteBufCodecs.map(java.util.HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_INT),
            DailyBoardData::clinicUses,
            DailyBoardData::new);

    public DailyBoardData forDay(long day) {
        if (day == dayIndex) {
            return this;
        }
        return new DailyBoardData(day, 0, Map.of());
    }

    public DailyBoardData withBoardClaims(int claims) {
        return new DailyBoardData(dayIndex, claims, clinicUses);
    }

    public DailyBoardData withClinicUse(String treatmentId, int uses) {
        Map<String, Integer> next = new java.util.HashMap<>(clinicUses);
        next.put(treatmentId.toLowerCase(Locale.ROOT), uses);
        return new DailyBoardData(dayIndex, boardClaims, Map.copyOf(next));
    }

    public int clinicUsesOf(String treatmentId) {
        return clinicUses.getOrDefault(treatmentId.toLowerCase(Locale.ROOT), 0);
    }
}
