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
 * Data-pack entry: thirst / temperature change when a food or drink finishes being used.
 *
 * <p>Location: {@code data/<namespace>/realismsurvival/drink/<name>.json}
 * (directory name is historical; entries cover meals as well as potions.)
 *
 * <pre>{@code
 * {
 *   "items": ["minecraft:potion"],        // HolderSet of items (grammar: see BiomeThirstProfile)
 *   "thirst_restore": 30.0,               // may be negative (e.g. salty / dry food)
 *   "temperature_delta": 0.0,             // optional; e.g. +0.5 for hot soup, -0.1 for juicy fruit
 *   "priority": 0
 * }
 * }</pre>
 *
 * <p>Vanilla item ids are matched by item type only. If a future ticket needs to distinguish
 * potion contents (water bottle vs. real potions), extend this record with an optional
 * {@code DataComponentPredicate} field and keep the JSON field optional for backwards compatibility.
 *
 * @param items            items this definition applies to
 * @param thirstRestore    thirst points added on consume (negative allowed)
 * @param temperatureDelta body-temp regulation strength toward 36.5 °C on consume
 *                         (positive = warm when cold, negative = cool when hot; never past normal)
 * @param priority         conflict resolution, highest wins
 */
public record DrinkDefinition(HolderSet<Item> items, float thirstRestore, float temperatureDelta, int priority)
        implements Prioritized {

    public static final Codec<DrinkDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            RegistryCodecs.homogeneousList(Registries.ITEM).fieldOf("items").forGetter(DrinkDefinition::items),
            Codec.floatRange(-100.0F, 100.0F).fieldOf("thirst_restore").forGetter(DrinkDefinition::thirstRestore),
            Codec.floatRange(-5.0F, 5.0F).optionalFieldOf("temperature_delta", 0.0F).forGetter(DrinkDefinition::temperatureDelta),
            Codec.INT.optionalFieldOf("priority", 0).forGetter(DrinkDefinition::priority)
    ).apply(instance, DrinkDefinition::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, DrinkDefinition> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.holderSet(Registries.ITEM), DrinkDefinition::items,
            ByteBufCodecs.FLOAT, DrinkDefinition::thirstRestore,
            ByteBufCodecs.FLOAT, DrinkDefinition::temperatureDelta,
            ByteBufCodecs.VAR_INT, DrinkDefinition::priority,
            DrinkDefinition::new
    );

    public boolean matches(Holder<Item> item) {
        return items.contains(item);
    }
}
