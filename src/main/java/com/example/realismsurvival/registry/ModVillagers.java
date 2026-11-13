package com.example.realismsurvival.registry;

import com.example.realismsurvival.RealismSurvival;
import com.google.common.collect.ImmutableSet;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Clinic table POI + doctor profession. Healing is a custom RS menu, not vanilla trades. */
public final class ModVillagers {

    private ModVillagers() {}

    public static final ResourceKey<PoiType> CLINIC_POI_KEY =
            ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE, RealismSurvival.id("clinic"));
    public static final ResourceKey<VillagerProfession> DOCTOR_KEY =
            ResourceKey.create(Registries.VILLAGER_PROFESSION, RealismSurvival.id("doctor"));

    public static final DeferredRegister<PoiType> POI_TYPES =
            DeferredRegister.create(Registries.POINT_OF_INTEREST_TYPE, RealismSurvival.MODID);
    public static final DeferredRegister<VillagerProfession> PROFESSIONS =
            DeferredRegister.create(Registries.VILLAGER_PROFESSION, RealismSurvival.MODID);

    public static final DeferredHolder<PoiType, PoiType> CLINIC_POI = POI_TYPES.register("clinic", () -> {
        Set<BlockState> states = ImmutableSet.copyOf(ModBlocks.CLINIC_TABLE.get().getStateDefinition().getPossibleStates());
        return new PoiType(states, 1, 1);
    });

    public static final DeferredHolder<VillagerProfession, VillagerProfession> DOCTOR = PROFESSIONS.register(
            "doctor",
            () -> new VillagerProfession(
                    Component.translatable("entity.minecraft.villager.realismsurvival.doctor"),
                    ModVillagers::isClinic,
                    ModVillagers::isClinic,
                    ImmutableSet.of(),
                    ImmutableSet.of(),
                    SoundEvents.VILLAGER_WORK_CLERIC,
                    Int2ObjectMaps.emptyMap()));

    private static boolean isClinic(Holder<PoiType> holder) {
        return holder.is(CLINIC_POI_KEY) || holder.is(CLINIC_POI.getKey());
    }

    public static void register(IEventBus modEventBus) {
        POI_TYPES.register(modEventBus);
        PROFESSIONS.register(modEventBus);
    }

    public static boolean isDoctor(VillagerProfession profession) {
        return BuiltInRegistries.VILLAGER_PROFESSION
                        .getResourceKey(profession)
                        .map(DOCTOR_KEY::equals)
                        .orElse(false)
                || (DOCTOR.isBound() && profession == DOCTOR.get());
    }
}
