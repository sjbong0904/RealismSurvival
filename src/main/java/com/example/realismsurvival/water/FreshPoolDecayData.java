package com.example.realismsurvival.water;

import com.example.realismsurvival.RealismSurvival;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Per-dimension first-seen game times for fresh water source cells (stagnation grace clocks).
 */
public final class FreshPoolDecayData extends SavedData {

    private static final Codec<Entry> ENTRY_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.fieldOf("pos").forGetter(Entry::pos),
            Codec.LONG.fieldOf("first_seen").forGetter(Entry::firstSeen)
    ).apply(instance, Entry::new));

    public static final Codec<FreshPoolDecayData> CODEC =
            ENTRY_CODEC.listOf().xmap(FreshPoolDecayData::fromEntries, FreshPoolDecayData::toEntries);

    public static final SavedDataType<FreshPoolDecayData> TYPE = new SavedDataType<>(
            RealismSurvival.id("fresh_pool_decay"),
            FreshPoolDecayData::new,
            CODEC
    );

    private final Long2LongOpenHashMap firstSeenByPos = new Long2LongOpenHashMap();

    public FreshPoolDecayData() {
        this.firstSeenByPos.defaultReturnValue(Long.MIN_VALUE);
    }

    private FreshPoolDecayData(Long2LongOpenHashMap map) {
        this();
        this.firstSeenByPos.putAll(map);
    }

    public static FreshPoolDecayData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    /** @return first-seen time, or {@link Long#MIN_VALUE} if unknown */
    public long getFirstSeen(long posKey) {
        return firstSeenByPos.get(posKey);
    }

    public long getFirstSeen(BlockPos pos) {
        return getFirstSeen(pos.asLong());
    }

    /** Records {@code gameTime} only if this cell is not already tracked. */
    public void notice(BlockPos pos, long gameTime) {
        long key = pos.asLong();
        if (!firstSeenByPos.containsKey(key)) {
            firstSeenByPos.put(key, gameTime);
            setDirty();
        }
    }

    /** Test / admin: force the grace clock for a cell. */
    public void setFirstSeen(BlockPos pos, long gameTime) {
        firstSeenByPos.put(pos.asLong(), gameTime);
        setDirty();
    }

    public void removeKey(long posKey) {
        if (firstSeenByPos.containsKey(posKey)) {
            firstSeenByPos.remove(posKey);
            setDirty();
        }
    }

    public void removeAll(Iterable<BlockPos> positions) {
        boolean changed = false;
        for (BlockPos pos : positions) {
            long key = pos.asLong();
            if (firstSeenByPos.containsKey(key)) {
                firstSeenByPos.remove(key);
                changed = true;
            }
        }
        if (changed) {
            setDirty();
        }
    }

    /** Tracked fresh source cells in this dimension (measurement, 7.4). */
    public int trackedCount() {
        return firstSeenByPos.size();
    }

    Long2LongMap map() {
        return firstSeenByPos;
    }

    private static FreshPoolDecayData fromEntries(List<Entry> entries) {
        Long2LongOpenHashMap map = new Long2LongOpenHashMap();
        map.defaultReturnValue(Long.MIN_VALUE);
        for (Entry entry : entries) {
            map.put(entry.pos(), entry.firstSeen());
        }
        return new FreshPoolDecayData(map);
    }

    private List<Entry> toEntries() {
        List<Entry> list = new ArrayList<>(firstSeenByPos.size());
        for (Long2LongMap.Entry e : firstSeenByPos.long2LongEntrySet()) {
            list.add(new Entry(e.getLongKey(), e.getLongValue()));
        }
        return list;
    }

    private record Entry(long pos, long firstSeen) {}
}
