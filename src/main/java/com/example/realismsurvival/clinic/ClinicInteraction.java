package com.example.realismsurvival.clinic;

import com.example.realismsurvival.RealismSurvival;
import com.example.realismsurvival.registry.ModVillagers;
import com.example.realismsurvival.world.inventory.ClinicMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = RealismSurvival.MODID)
public final class ClinicInteraction {

    private ClinicInteraction() {}

    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof Villager villager) || event.getLevel().isClientSide()) {
            return;
        }
        VillagerProfession profession = villager.getVillagerData().profession().value();
        if (!ModVillagers.isDoctor(profession)) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        event.setCanceled(true);
        player.openMenu(new SimpleMenuProvider(
                (id, inventory, p) -> new ClinicMenu(id, inventory, player, villager),
                Component.translatable("menu.realismsurvival.clinic")));
    }
}
