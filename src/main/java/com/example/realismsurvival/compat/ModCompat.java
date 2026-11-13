package com.example.realismsurvival.compat;

import com.example.realismsurvival.RealismSurvival;
import net.neoforged.fml.ModList;

/**
 * Soft presence checks for the planned NeoForge 26.1 modpack stack.
 * Detection / logging only — no gameplay changes when these mods are present or absent.
 */
public final class ModCompat {

    private ModCompat() {}

    // ---- Planned pack stack (user list) -------------------------------------------------------------------------------

    public static final String JEI = "jei";
    /** Cloth Config API — many config screens depend on this. */
    public static final String CLOTH_CONFIG = "cloth_config";
    /** Architectury API — multi-loader common layer. */
    public static final String ARCHITECTURY = "architectury";
    public static final String APPLESKIN = "appleskin";
    public static final String SODIUM = "sodium";
    public static final String CURIOS = "curios";
    public static final String IRIS = "iris";
    public static final String FARMERS_DELIGHT = "farmersdelight";
    /** SuperMartijn642's Core Lib */
    public static final String SUPERMARTIJN_CORE = "supermartijn642corelib";
    /** Serene Seasons — optional soft-compat for season authority when present. */
    public static final String SERENE_SEASONS = "sereneseasons";
    /** GlitchCore — library pulled in with Serene Seasons. */
    public static final String GLITCHCORE = "glitchcore";
    /** Aquaculture 2 — optional soft-compat for fishing/fish content when present. */
    public static final String AQUACULTURE = "aquaculture";
    /** Fish of Thieves — optional soft-compat; requires Cloth Config. */
    public static final String FISH_OF_THIEVES = "fishofthieves";
    /**
     * Create — optional soft-compat (fluid/item automation). Not yet published for Minecraft 26.1;
     * detection is ready for when a NeoForge 26.1 build exists.
     */
    public static final String CREATE = "create";
    /**
     * Large Fluid Tank ({@code fluidtank}) — fluid storage stand-in for Create tanks on 26.1.
     * Requires Scalable Cat's Force (Scala language loader; not a normal mod id).
     */
    public static final String FLUID_TANK = "fluidtank";
    /** Pipez — fluid/item pipes for track-B automation without Create. */
    public static final String PIPEZ = "pipez";

    // ---- Pack QoL / maps / voice / ambient / perf (docs/MODPACK.md) ----------------------------------------------------

    public static final String JADE = "jade";
    /**
     * Jade Addons — detection only. Not on localRuntime until upstream drops {@code @OnlyIn}
     * (NeoForge 26.1 boot warning).
     */
    public static final String JADE_ADDONS = "jadeaddons";
    /** SeasonHUD — season/date overlay; hidden when FTB minimap is visible (RS draws it under biome). */
    public static final String SEASON_HUD = "seasonhud";
    /** Waystones Teleport Pets — pets follow Waystone warps (mod id {@code w2pets}). */
    public static final String WAYSTONES_TELEPORT_PETS = "w2pets";
    /** FTB Chunks — minimap, world map, claims, forceload (packcontent). */
    public static final String FTB_CHUNKS = "ftbchunks";
    public static final String FTB_TEAMS = "ftbteams";
    public static final String FTB_QUESTS = "ftbquests";
    public static final String MOUSE_TWEAKS = "mousetweaks";
    public static final String TRASH_SLOT = "trashslot";
    public static final String BALM = "balm";
    public static final String IRON_CHESTS = "ironchest";
    public static final String VOICE_CHAT = "voicechat";
    public static final String AMBIENT_SOUNDS = "ambientsounds";
    public static final String CREATIVE_CORE = "creativecore";
    public static final String ENTITY_CULLING = "entityculling";
    public static final String MODERN_FIX = "modernfix";
    public static final String FERRITE_CORE = "ferritecore";

    // ---- Review / hold (detection only; not always localRuntime) -------------------------------------------------------

    public static final String COMFORTS = "comforts";
    public static final String BIOMES_O_PLENTY = "biomesoplenty";
    public static final String TERRA_BLENDER = "terrablender";
    public static final String DYNAMIC_TREES = "dynamictrees";
    public static final String ARTIFACTS = "artifacts";
    public static final String GUARD_VILLAGERS = "guardvillagers";
    public static final String WAYSTONES = "waystones";
    public static final String SHOGI = "shogi";
    /** Q Shop — seasonal supply post GUI. */
    public static final String QSHOP = "qshop";
    /** SG Economy — inventory gold display / virtual wallet. */
    public static final String SG_ECONOMY = "sg_economy";
    /** Easy NPC — supply merchants that open Q Shop. */
    public static final String EASY_NPC = "easy_npc";

    public static boolean loaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    /** Call from common setup — logs which pack-stack mods were detected. */
    public static void logDetected() {
        StringBuilder sb = new StringBuilder("[RealismSurvival] Pack-stack detection:");
        for (String id : packStackIds()) {
            append(sb, id);
        }
        RealismSurvival.LOGGER.info(sb.toString());
    }

    /** Ordered list matching the planned modpack checklist. */
    public static String[] packStackIds() {
        return new String[] {
                JEI,
                CLOTH_CONFIG,
                ARCHITECTURY,
                APPLESKIN,
                SODIUM,
                CURIOS,
                IRIS,
                FARMERS_DELIGHT,
                SUPERMARTIJN_CORE,
                SERENE_SEASONS,
                GLITCHCORE,
                AQUACULTURE,
                FISH_OF_THIEVES,
                CREATE,
                FLUID_TANK,
                PIPEZ,
                JADE,
                JADE_ADDONS,
                SEASON_HUD,
                WAYSTONES_TELEPORT_PETS,
                FTB_CHUNKS,
                FTB_TEAMS,
                FTB_QUESTS,
                MOUSE_TWEAKS,
                TRASH_SLOT,
                BALM,
                IRON_CHESTS,
                VOICE_CHAT,
                AMBIENT_SOUNDS,
                CREATIVE_CORE,
                ENTITY_CULLING,
                MODERN_FIX,
                FERRITE_CORE,
                COMFORTS,
                BIOMES_O_PLENTY,
                TERRA_BLENDER,
                DYNAMIC_TREES,
                ARTIFACTS,
                GUARD_VILLAGERS,
                WAYSTONES,
                SHOGI,
                QSHOP,
                SG_ECONOMY,
                EASY_NPC
        };
    }

    private static void append(StringBuilder sb, String modId) {
        sb.append(' ').append(modId).append('=').append(loaded(modId) ? "yes" : "no");
    }
}
