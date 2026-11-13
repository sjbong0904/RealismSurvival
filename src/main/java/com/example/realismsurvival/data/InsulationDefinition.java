package com.example.realismsurvival.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;

/**
 * Data-pack entry: body-temperature insulation from equipped items (T-06 / spec §9.3).
 *
 * <p>Location: {@code data/<namespace>/realismsurvival/insulation/<name>.json}
 *
 * <p>Counts for armor slots, Curios (when loaded), and stacks added via
 * {@link com.example.realismsurvival.api.event.CollectEquippedItemsEvent}.
 *
 * <pre>{@code
 * {
 *   "items": ["minecraft:leather_helmet", "#curios:necklace", "mymod:fur_cloak"],
 *   "insulation": 0.1,     // added per matching equipped stack
 *   "priority": 0
 * }
 * }</pre>
 *
 * @param items       items (or item tags) that grant insulation when equipped
 * @param insulation  contribution in {@code [0, 1]} per matching stack
 * @param priority    conflict resolution when several entries match one item
 */
public record InsulationDefinition(HolderSet<Item> items, float insulation, int priority) implements Prioritized {

    public static final Codec<InsulationDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            RegistryCodecs.homogeneousList(Registries.ITEM).fieldOf("items").forGetter(InsulationDefinition::items),
            Codec.floatRange(0.0F, 1.0F).fieldOf("insulation").forGetter(InsulationDefinition::insulation),
            Codec.INT.optionalFieldOf("priority", 0).forGetter(InsulationDefinition::priority)
    ).apply(instance, InsulationDefinition::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, InsulationDefinition> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.holderSet(Registries.ITEM), InsulationDefinition::items,
            ByteBufCodecs.FLOAT, InsulationDefinition::insulation,
            ByteBufCodecs.VAR_INT, InsulationDefinition::priority,
            InsulationDefinition::new
    );

    public boolean matches(Holder<Item> item) {
        return items.contains(item);
    }
}
