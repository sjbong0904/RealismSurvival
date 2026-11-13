package com.example.realismsurvival.world.inventory;

import com.example.realismsurvival.RealismSurvival;
import net.minecraft.resources.Identifier;

/** Empty-slot background sprites (monochrome item silhouettes). */
public final class MachineSlotIcons {

    private MachineSlotIcons() {}

    public static final Identifier WATER_BUCKET = realism("water_bucket");
    public static final Identifier BUCKET = realism("bucket");
    public static final Identifier GLASS_BOTTLE = Identifier.withDefaultNamespace("container/slot/potion");
    public static final Identifier WATER_BOTTLE = realism("water_bottle");
    public static final Identifier COAL = realism("coal");
    public static final Identifier SALT = realism("salt");
    public static final Identifier FILTER_CARTRIDGE = realism("filter_cartridge");

    /** Side-by-side machine outputs (equal 18×18 slots). */
    public static final int OUT_LEFT_X = 105;
    public static final int OUT_RIGHT_X = 125;
    public static final int OUT_Y = 35;

    private static Identifier realism(String name) {
        return Identifier.fromNamespaceAndPath(RealismSurvival.MODID, "container/slot/" + name);
    }
}
