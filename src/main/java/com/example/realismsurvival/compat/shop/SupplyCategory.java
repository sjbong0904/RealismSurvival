package com.example.realismsurvival.compat.shop;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * One supply post is one of these merchants. The worldgen roll is even, so each kind is about a quarter of stalls.
 */
public enum SupplyCategory {
    FOOD("food", "식량", "entity.realismsurvival.supply_merchant.food", VillagerProfession.FARMER, Blocks.HAY_BLOCK.defaultBlockState()),
    WEAPONS("weapons", "무기", "entity.realismsurvival.supply_merchant.weapons", VillagerProfession.WEAPONSMITH, Blocks.GRINDSTONE.defaultBlockState()),
    POTIONS("potions", "물약", "entity.realismsurvival.supply_merchant.potions", VillagerProfession.CLERIC, Blocks.BREWING_STAND.defaultBlockState()),
    MINERALS("minerals", "광물", "entity.realismsurvival.supply_merchant.minerals", VillagerProfession.MASON, Blocks.COAL_BLOCK.defaultBlockState());

    private final String id;
    private final String label;
    private final String nameKey;
    private final ResourceKey<VillagerProfession> profession;
    private final BlockState accent;

    SupplyCategory(String id, String label, String nameKey, ResourceKey<VillagerProfession> profession, BlockState accent) {
        this.id = id;
        this.label = label;
        this.nameKey = nameKey;
        this.profession = profession;
        this.accent = accent;
    }

    public String id() {
        return id;
    }

    /** Shop-title fragment. The rest of the supply UI is already Korean. */
    public String label() {
        return label;
    }

    public String nameKey() {
        return nameKey;
    }

    public String entityTag() {
        return ShopMods.NPC_SHOP_TAG + "_" + id;
    }

    public ResourceKey<VillagerProfession> profession() {
        return profession;
    }

    public BlockState accent() {
        return accent;
    }

    public static SupplyCategory roll(RandomSource random) {
        SupplyCategory[] values = values();
        return values[random.nextInt(values.length)];
    }

    /** Stable kind for stalls that were generated before categories existed. */
    public static SupplyCategory fromPos(BlockPos pos) {
        SupplyCategory[] values = values();
        int index = Math.floorMod(pos.getX() * 31 + pos.getZ() * 17 + pos.getY(), values.length);
        return values[index];
    }

    public static SupplyCategory byId(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        for (SupplyCategory category : values()) {
            if (category.id.equals(id)) {
                return category;
            }
        }
        return null;
    }

    public static SupplyCategory fromEntity(Entity entity) {
        if (entity == null) {
            return FOOD;
        }
        for (SupplyCategory category : values()) {
            if (entity.entityTags().contains(category.entityTag())) {
                return category;
            }
        }
        return FOOD;
    }
}
