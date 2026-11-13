package com.example.realismsurvival.network;

import com.example.realismsurvival.RealismSurvival;
import com.example.realismsurvival.data.BiomeThirstProfile;
import com.example.realismsurvival.data.CropGrowthModifier;
import com.example.realismsurvival.data.CropLightProfile;
import com.example.realismsurvival.data.DrinkDefinition;
import com.example.realismsurvival.data.InsulationDefinition;
import com.example.realismsurvival.data.SurvivalDataManager;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Full datapack table dump (server → client). The physical client applies this into
 * {@code ClientSurvivalData} via {@code RegisterClientPayloadHandlersEvent} (T-09 / spec §7.6).
 */
@EventBusSubscriber(modid = RealismSurvival.MODID)
public record SyncSurvivalDataPayload(
        Map<Identifier, BiomeThirstProfile> biomeThirst,
        Map<Identifier, CropGrowthModifier> cropGrowth,
        Map<Identifier, CropLightProfile> cropLight,
        Map<Identifier, DrinkDefinition> drinks,
        Map<Identifier, InsulationDefinition> insulation
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncSurvivalDataPayload> TYPE =
            new CustomPacketPayload.Type<>(RealismSurvival.id("sync_survival_data"));

    private static final StreamCodec<RegistryFriendlyByteBuf, Map<Identifier, BiomeThirstProfile>> BIOME_THIRST_MAP =
            ByteBufCodecs.map(HashMap::new, Identifier.STREAM_CODEC, BiomeThirstProfile.STREAM_CODEC);
    private static final StreamCodec<RegistryFriendlyByteBuf, Map<Identifier, CropGrowthModifier>> CROP_GROWTH_MAP =
            ByteBufCodecs.map(HashMap::new, Identifier.STREAM_CODEC, CropGrowthModifier.STREAM_CODEC);
    private static final StreamCodec<RegistryFriendlyByteBuf, Map<Identifier, CropLightProfile>> CROP_LIGHT_MAP =
            ByteBufCodecs.map(HashMap::new, Identifier.STREAM_CODEC, CropLightProfile.STREAM_CODEC);
    private static final StreamCodec<RegistryFriendlyByteBuf, Map<Identifier, DrinkDefinition>> DRINKS_MAP =
            ByteBufCodecs.map(HashMap::new, Identifier.STREAM_CODEC, DrinkDefinition.STREAM_CODEC);
    private static final StreamCodec<RegistryFriendlyByteBuf, Map<Identifier, InsulationDefinition>> INSULATION_MAP =
            ByteBufCodecs.map(HashMap::new, Identifier.STREAM_CODEC, InsulationDefinition.STREAM_CODEC);

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncSurvivalDataPayload> STREAM_CODEC = StreamCodec.composite(
            BIOME_THIRST_MAP, SyncSurvivalDataPayload::biomeThirst,
            CROP_GROWTH_MAP, SyncSurvivalDataPayload::cropGrowth,
            CROP_LIGHT_MAP, SyncSurvivalDataPayload::cropLight,
            DRINKS_MAP, SyncSurvivalDataPayload::drinks,
            INSULATION_MAP, SyncSurvivalDataPayload::insulation,
            SyncSurvivalDataPayload::new
    );

    /** Snapshot of the current server-side {@link SurvivalDataManager} tables. */
    public static SyncSurvivalDataPayload fromServer() {
        return new SyncSurvivalDataPayload(
                SurvivalDataManager.allBiomeThirst(),
                SurvivalDataManager.allCropGrowth(),
                SurvivalDataManager.allCropLight(),
                SurvivalDataManager.allDrinks(),
                SurvivalDataManager.allInsulation()
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @SubscribeEvent
    static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(TYPE, STREAM_CODEC);
    }
}
