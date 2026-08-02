package io.github.cruciblemc.necrotempus.modules.features.font;

import io.github.cruciblemc.necrotempus.NecroTempusConfig;

public final class FontFeatureToggles {

    private FontFeatureToggles() {}

    public static boolean isGlyphsEnabled() {
        return NecroTempusConfig.glyphs;
    }

    public static boolean isModernFontsEnabled() {
        return NecroTempusConfig.modernFonts;
    }

    public static boolean isAngelicaGlyphsIntegrationEnabled() {
        return isGlyphsEnabled() && NecroTempusConfig.angelicaGlyphsIntegration;
    }

    public static boolean isAngelicaModernFontsIntegrationEnabled() {
        return isModernFontsEnabled() && NecroTempusConfig.angelicaModernFontsIntegration;
    }

    public static boolean isVanillaFontMixinEnabled() {
        return isGlyphsEnabled() || isModernFontsEnabled();
    }

    public static boolean isAngelicaFontMixinEnabled() {
        return isAngelicaGlyphsIntegrationEnabled() || isAngelicaModernFontsIntegrationEnabled();
    }
}
