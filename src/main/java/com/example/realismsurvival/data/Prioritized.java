package com.example.realismsurvival.data;

/**
 * Shared contract for data-pack entries that may overlap. When several entries match the same
 * biome/item/crop, {@link SurvivalDataManager} picks the one with the highest {@link #priority()}.
 * Ties are broken by the entry's {@code Identifier} (alphabetical) so results are deterministic
 * regardless of file-system iteration order.
 */
public interface Prioritized {

    int priority();
}
