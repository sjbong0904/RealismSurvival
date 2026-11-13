package com.example.realismsurvival.environment;

import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.StringRepresentable;

/** Four-season calendar used by HUD, climate offsets, precip, and crop seasonality. */
public enum Season implements StringRepresentable {
    // No U+FE0F variation selectors — those render as a literal "VS" glyph in MC fonts.
    SPRING("🌸", 0x7EC850),
    SUMMER("☀", 0xFFD54A),
    AUTUMN("🍂", 0xFF8A3D),
    WINTER("❄", 0x8EC8FF);

    public static final StringRepresentable.EnumCodec<Season> CODEC = StringRepresentable.fromEnum(Season::values);

    private final String emoji;
    private final int hudRgb;

    Season(String emoji, int hudRgb) {
        this.emoji = emoji;
        this.hudRgb = hudRgb;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public Component displayName() {
        return Component.translatable("season.realismsurvival." + getSerializedName());
    }

    public String emoji() {
        return emoji;
    }

    /** RGB used for instruments calendar line (no alpha). */
    public int hudRgb() {
        return hudRgb;
    }

    public Style hudStyle() {
        return Style.EMPTY.withColor(TextColor.fromRgb(hudRgb));
    }

    public static Season byName(String name) {
        return Season.valueOf(name.toUpperCase(Locale.ROOT));
    }
}
