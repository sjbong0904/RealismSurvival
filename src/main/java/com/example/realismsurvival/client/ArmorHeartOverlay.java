package com.example.realismsurvival.client;

import com.example.realismsurvival.RealismSurvival;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Armor HUD (spec §8.6): hide the vanilla armor row and draw material-tinted
 * heart <em>container</em> overlays on the HP row so we can verify coordinates.
 *
 * <p>The custom sprites contain only the perimeter and have a fully transparent center, so vanilla
 * health/status fills remain visible. Per-material PNGs are unnecessary: runtime tint supplies the
 * dominant material color. Datapack {@code armor_tint} classification comes later.
 */
public final class ArmorHeartOverlay {

    private static final Identifier CONTAINER = RealismSurvival.id("hud/armor_container");
    private static final Identifier HALF_CONTAINER = RealismSurvival.id("hud/armor_container_half");

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    /** Fallback when armor points exist but no known material matched. */
    private static final int COLOR_DEFAULT = ARGB.color(255, 200, 200, 210);

    private ArmorHeartOverlay() {}

    public static void render(GuiGraphicsExtractor graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = HudLayout.cameraPlayer(minecraft);
        if (player == null) {
            return;
        }

        int armor = player.getArmorValue();
        if (armor <= 0) {
            return;
        }

        int tint = dominantTint(player);
        Gui gui = minecraft.gui;
        // After PLAYER_HEALTH (single row): hearts sit one ROW_HEIGHT below the current leftHeight cursor.
        int y = graphics.guiHeight() - gui.leftHeight + HudLayout.ROW_HEIGHT;
        int xLeft = graphics.guiWidth() / 2 - HudLayout.STATUS_BAR_HALF_WIDTH;

        for (int i = 0; i < HudLayout.ICON_COUNT; i++) {
            int threshold = i * 2 + 1;
            int x = xLeft + i * HudLayout.ICON_STRIDE;
            if (threshold < armor) {
                blitContainer(graphics, x, y, tint, false);
            } else if (threshold == armor) {
                blitContainer(graphics, x, y, tint, true);
            }
        }
    }

    private static void blitContainer(GuiGraphicsExtractor graphics, int x, int y, int tint, boolean half) {
        Identifier sprite = half ? HALF_CONTAINER : CONTAINER;
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, HudLayout.ICON_SIZE, HudLayout.ICON_SIZE, tint);
    }

    /**
     * Sum armor attribute points per material group on the four armor slots; highest sum wins.
     * Tie-break: netherite &gt; diamond &gt; iron &gt; chain &gt; gold &gt; leather &gt; turtle &gt; other.
     */
    static int dominantTint(Player player) {
        int[] sums = new int[TintGroup.VALUES.length];
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            int points = armorPointsOn(stack, slot);
            if (points <= 0) {
                continue;
            }
            sums[TintGroup.fromItem(stack).ordinal()] += points;
        }

        TintGroup best = TintGroup.OTHER;
        int bestSum = 0;
        for (TintGroup group : TintGroup.VALUES) {
            int sum = sums[group.ordinal()];
            if (sum > bestSum || (sum == bestSum && sum > 0 && group.tier > best.tier)) {
                bestSum = sum;
                best = group;
            }
        }
        return bestSum > 0 ? best.color : COLOR_DEFAULT;
    }

    private static int armorPointsOn(ItemStack stack, EquipmentSlot slot) {
        double[] total = {0.0};
        stack.forEachModifier(slot, (attribute, modifier) -> {
            if (attribute.is(Attributes.ARMOR) && modifier.operation() == AttributeModifier.Operation.ADD_VALUE) {
                total[0] += modifier.amount();
            }
        });
        return (int) Math.round(total[0]);
    }

    enum TintGroup {
        NETHERITE(60, ARGB.color(255, 70, 60, 65)),
        DIAMOND(50, ARGB.color(255, 74, 237, 217)),
        IRON(40, ARGB.color(255, 200, 200, 210)),
        CHAIN(30, ARGB.color(255, 150, 160, 170)),
        GOLD(20, ARGB.color(255, 230, 190, 60)),
        LEATHER(10, ARGB.color(255, 160, 100, 60)),
        TURTLE(5, ARGB.color(255, 60, 160, 80)),
        OTHER(0, COLOR_DEFAULT);

        static final TintGroup[] VALUES = values();

        final int tier;
        final int color;

        TintGroup(int tier, int color) {
            this.tier = tier;
            this.color = color;
        }

        static TintGroup fromItem(ItemStack stack) {
            String path = stack.typeHolder()
                    .unwrapKey()
                    .map(key -> key.identifier().getPath())
                    .orElse("");
            if (path.contains("netherite")) {
                return NETHERITE;
            }
            if (path.contains("diamond")) {
                return DIAMOND;
            }
            if (path.contains("iron")) {
                return IRON;
            }
            if (path.contains("chainmail") || path.contains("chain")) {
                return CHAIN;
            }
            if (path.contains("golden") || path.contains("gold")) {
                return GOLD;
            }
            if (path.contains("leather")) {
                return LEATHER;
            }
            if (path.contains("turtle")) {
                return TURTLE;
            }
            return OTHER;
        }
    }
}
