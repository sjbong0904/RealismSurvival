package com.example.realismsurvival.component;

import com.example.realismsurvival.RealismSurvival;
import com.mojang.serialization.Codec;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Registry of every NeoForge {@link AttachmentType} owned by this mod.
 *
 * <p>Attachment lifecycle for {@link #SURVIVAL_DATA} (spec §6):
 * <ol>
 *   <li><b>Default</b>: a player who has never been seen gets {@link SurvivalData#DEFAULT} lazily on first {@code getData}.</li>
 *   <li><b>Persistence</b>: serialized with {@link SurvivalData#MAP_CODEC} into the player's NBT ({@code neoforge:attachments}).</li>
 *   <li><b>Death</b>: {@code copyOnDeath()} carries the record to the respawned entity, so dying does not reset thirst/temperature.
 *       Gameplay-level resets (e.g. "respawn at 60 thirst") must be done explicitly in {@code PlayerEvent.PlayerRespawnEvent}.</li>
 *   <li><b>Sync</b>: every {@code setData} on the logical server pushes the full record to the <em>owning</em> player only.
 *       Other players never receive it (bandwidth + privacy). The client HUD reads the synced copy.</li>
 * </ol>
 */
public final class SurvivalAttachments {

    private SurvivalAttachments() {}

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, RealismSurvival.MODID);

    /** {@code realismsurvival:survival_data} — thirst + body temperature for every {@link Player}. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<SurvivalData>> SURVIVAL_DATA =
            ATTACHMENT_TYPES.register("survival_data", () -> AttachmentType.builder(() -> SurvivalData.DEFAULT)
                    // 1) Disk persistence via Codec. Must come before copyOnDeath() (builder precondition).
                    .serialize(SurvivalData.MAP_CODEC)
                    // 2) Survive death/respawn and End-portal return.
                    .copyOnDeath()
                    // 3) Server -> owning client only. `holder` is the entity carrying the data; `to` is the receiver.
                    .sync((holder, to) -> holder == to, SurvivalData.STREAM_CODEC)
                    .build());

    /**
     * Chunk flag: vanilla {@code Blocks.WATER} in this chunk has been rewritten to graded fluids.
     * Set during worldgen ({@code initializeLight}); legacy chunks convert once on load.
     */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Boolean>> WATER_CONVERTED =
            ATTACHMENT_TYPES.register(
                    "water_converted",
                    () -> AttachmentType.builder(() -> false)
                            .serialize(Codec.BOOL.fieldOf("value"))
                            .build()
            );

    /** Overworld-day counters for daily board gold claims and clinic treatment limits. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<DailyBoardData>> DAILY_BOARD =
            ATTACHMENT_TYPES.register(
                    "daily_board",
                    () -> AttachmentType.builder(() -> DailyBoardData.DEFAULT)
                            .serialize(DailyBoardData.MAP_CODEC)
                            .copyOnDeath()
                            .build());

    /** Call once from the mod constructor with the <b>mod</b> event bus. */
    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }

    // ---- Convenience accessors (side-agnostic reads, server-only writes) --------------------------------------------

    /** Read the current record. Safe on both sides; on the client this is the last synced copy. */
    public static SurvivalData get(Player player) {
        return player.getData(SURVIVAL_DATA);
    }

    /**
     * Store a new record. <b>Logical server only.</b> Triggers automatic sync to the owning client.
     *
     * @throws IllegalStateException when called on the logical client, to catch side-leaks early in development.
     */
    public static void set(Player player, SurvivalData data) {
        if (player.level().isClientSide()) {
            throw new IllegalStateException("SurvivalData must only be written on the logical server");
        }
        player.setData(SURVIVAL_DATA, data);
    }

    public static DailyBoardData getDailyBoard(Player player) {
        return player.getData(DAILY_BOARD);
    }

    public static void setDailyBoard(Player player, DailyBoardData data) {
        if (player.level().isClientSide()) {
            throw new IllegalStateException("DailyBoardData must only be written on the logical server");
        }
        player.setData(DAILY_BOARD, data);
    }
}
