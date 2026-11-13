package com.example.realismsurvival.api.compat;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Soft Curios bridge (no compile dependency). When Curios is absent, all methods no-op.
 * Used so {@code insulation} datapack entries apply to accessory slots the same as armor.
 */
public final class CuriosAccess {

    private static final Logger LOGGER = LoggerFactory.getLogger("RealismSurvival/Curios");
    private static final boolean LOADED = ModList.get().isLoaded("curios");

    private static final Method GET_CURIOS_INVENTORY;
    private static final Method GET_EQUIPPED_CURIOS;

    static {
        Method getInv = null;
        Method getEquipped = null;
        if (LOADED) {
            try {
                Class<?> api = Class.forName("top.theillusivec4.curios.api.CuriosApi");
                getInv = api.getMethod("getCuriosInventory", LivingEntity.class);
                Class<?> handler = Class.forName("top.theillusivec4.curios.api.type.capability.ICuriosItemHandler");
                getEquipped = handler.getMethod("getEquippedCurios");
            } catch (ReflectiveOperationException e) {
                LOGGER.warn("[RealismSurvival] Curios present but API probe failed; accessories ignored for insulation", e);
            }
        }
        GET_CURIOS_INVENTORY = getInv;
        GET_EQUIPPED_CURIOS = getEquipped;
    }

    private CuriosAccess() {}

    public static boolean isLoaded() {
        return LOADED && GET_CURIOS_INVENTORY != null && GET_EQUIPPED_CURIOS != null;
    }

    public static void forEachEquipped(LivingEntity entity, Consumer<ItemStack> consumer) {
        if (!isLoaded()) {
            return;
        }
        try {
            Object optional = GET_CURIOS_INVENTORY.invoke(null, entity);
            if (!(optional instanceof Optional<?> opt) || opt.isEmpty()) {
                return;
            }
            Object handler = opt.get();
            Object itemHandler = GET_EQUIPPED_CURIOS.invoke(handler);
            if (itemHandler == null) {
                return;
            }
            Method getSlots = itemHandler.getClass().getMethod("getSlots");
            Method getStackInSlot = itemHandler.getClass().getMethod("getStackInSlot", int.class);
            int slots = (Integer) getSlots.invoke(itemHandler);
            for (int i = 0; i < slots; i++) {
                Object stackObj = getStackInSlot.invoke(itemHandler, i);
                if (stackObj instanceof ItemStack stack && !stack.isEmpty()) {
                    consumer.accept(stack);
                }
            }
        } catch (ReflectiveOperationException e) {
            LOGGER.debug("[RealismSurvival] Curios equipped scan failed", e);
        }
    }
}
