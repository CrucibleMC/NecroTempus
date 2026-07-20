package io.github.cruciblemc.necrotempus.modules.features.glyphs.compat.angelica;

import net.minecraft.util.ResourceLocation;

import com.gtnewhorizons.angelica.client.font.FontProvider;

import io.github.cruciblemc.necrotempus.modules.features.glyphs.CustomGlyphs;
import io.github.cruciblemc.necrotempus.modules.features.glyphs.GlyphsRegistry;
import io.github.cruciblemc.necrotempus.modules.features.modernfonts.ModernFontEntry;
import io.github.cruciblemc.necrotempus.modules.features.modernfonts.ModernFontSupport;
import io.github.cruciblemc.necrotempus.utils.MathUtils;

public class FontProviderGlyph implements FontProvider {

    public static final FontProviderGlyph INSTANCE = new FontProviderGlyph();

    /**
     * Set before returning INSTANCE from nt$redirectGetFontProvider.
     * Angelica's BatchingFontRenderer calls getYScaleMultiplier() to calculate
     * the Y position for each character before calling pushTexRect.
     * Must be per-instance safe since rendering is single-threaded.
     */
    public static float cachedGlyphScale = 1.0F;

    private FontProviderGlyph() {}

    private static CustomGlyphs glyph(char chr) {
        return GlyphsRegistry.getCandidate(chr);
    }

    @Override
    public char getRandomReplacement(char chr) {
        return chr;
    }

    @Override
    public boolean isGlyphAvailable(char chr) {
        return glyph(chr) != null || ModernFontSupport.getCandidate(chr) != null;
    }

    @Override
    public float getUStart(char chr) {
        return 0.0F;
    }

    @Override
    public float getVStart(char chr) {
        return 0.0F;
    }

    @Override
    public float getXAdvance(char chr) {
        final CustomGlyphs g = glyph(chr);
        if (g != null) return g.getFinalCharacterWidth();
        final ModernFontEntry e = ModernFontSupport.getCandidate(chr);
        if (e != null) return e.width + 1;
        return 0;
    }

    @Override
    public float getGlyphW(char chr) {
        final CustomGlyphs g = glyph(chr);
        if (g != null) {
            switch (g.getFitMode()) {
                case CONTAINS:
                    return 9;
                case VERTICALLY:
                    return (float) Math.ceil(MathUtils.calculateWidth(g.getWidth(), g.getHeight(), 9));
                default:
                    return g.getWidth();
            }
        }
        final ModernFontEntry e = ModernFontSupport.getCandidate(chr);
        if (e != null) return e.width;
        return 0;
    }

    @Override
    public float getUSize(char chr) {
        return 1.0F;
    }

    @Override
    public float getVSize(char chr) {
        return 1.0F;
    }

    @Override
    public float getShadowOffset() {
        return 0.0F;
    }

    @Override
    public ResourceLocation getTexture(char chr) {
        final CustomGlyphs g = glyph(chr);
        if (g != null) return g.getResource();
        final ModernFontEntry e = ModernFontSupport.getCandidate(chr);
        if (e != null) return e.location;
        return null;
    }

    @Override
    public float getYScaleMultiplier() {
        return cachedGlyphScale;
    }
}
