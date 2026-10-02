package io.github.cruciblemc.necrotempus.modules.features.font;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.cruciblemc.necrotempus.NecroTempusConfig;

public class FontFeatureTogglesTest {

    private boolean originalGlyphs;
    private boolean originalModernFonts;
    private boolean originalAngelicaGlyphs;
    private boolean originalAngelicaModernFonts;

    @BeforeEach
    public void captureConfiguration() {
        originalGlyphs = NecroTempusConfig.glyphs;
        originalModernFonts = NecroTempusConfig.modernFonts;
        originalAngelicaGlyphs = NecroTempusConfig.angelicaGlyphsIntegration;
        originalAngelicaModernFonts = NecroTempusConfig.angelicaModernFontsIntegration;
    }

    @AfterEach
    public void restoreConfiguration() {
        NecroTempusConfig.glyphs = originalGlyphs;
        NecroTempusConfig.modernFonts = originalModernFonts;
        NecroTempusConfig.angelicaGlyphsIntegration = originalAngelicaGlyphs;
        NecroTempusConfig.angelicaModernFontsIntegration = originalAngelicaModernFonts;
    }

    @Test
    public void defaultConfigurationEnablesBothModulesAndIntegrations() {
        assertTrue(NecroTempusConfig.glyphs);
        assertTrue(NecroTempusConfig.modernFonts);
        assertTrue(NecroTempusConfig.angelicaGlyphsIntegration);
        assertTrue(NecroTempusConfig.angelicaModernFontsIntegration);
    }

    @Test
    public void derivedActivationMatchesEveryConfigurationCombination() {
        boolean[] values = { false, true };

        for (boolean glyphs : values) {
            for (boolean modernFonts : values) {
                for (boolean angelicaGlyphs : values) {
                    for (boolean angelicaModernFonts : values) {
                        NecroTempusConfig.glyphs = glyphs;
                        NecroTempusConfig.modernFonts = modernFonts;
                        NecroTempusConfig.angelicaGlyphsIntegration = angelicaGlyphs;
                        NecroTempusConfig.angelicaModernFontsIntegration = angelicaModernFonts;

                        assertEquals(glyphs, FontFeatureToggles.isGlyphsEnabled());
                        assertEquals(modernFonts, FontFeatureToggles.isModernFontsEnabled());
                        assertEquals(glyphs && angelicaGlyphs, FontFeatureToggles.isAngelicaGlyphsIntegrationEnabled());
                        assertEquals(
                            modernFonts && angelicaModernFonts,
                            FontFeatureToggles.isAngelicaModernFontsIntegrationEnabled());
                        assertEquals(glyphs || modernFonts, FontFeatureToggles.isVanillaFontMixinEnabled());
                        assertEquals(
                            (glyphs && angelicaGlyphs) || (modernFonts && angelicaModernFonts),
                            FontFeatureToggles.isAngelicaFontMixinEnabled());
                    }
                }
            }
        }
    }
}
