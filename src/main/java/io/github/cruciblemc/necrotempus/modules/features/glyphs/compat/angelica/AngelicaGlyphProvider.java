package io.github.cruciblemc.necrotempus.modules.features.glyphs.compat.angelica;

import net.minecraft.util.ResourceLocation;

import com.gtnewhorizons.angelica.client.font.FontProvider;

import io.github.cruciblemc.necrotempus.modules.features.glyphs.CustomGlyphs;
import io.github.cruciblemc.necrotempus.modules.features.glyphs.GlyphsRegistry;
import io.github.cruciblemc.necrotempus.utils.MathUtils;

public class AngelicaGlyphProvider implements FontProvider {

    private final float glyphScale;

    private AngelicaGlyphProvider(float glyphScale) {
        this.glyphScale = glyphScale;
    }

    public static AngelicaGlyphProvider forScale(float glyphScale) {
        return new AngelicaGlyphProvider(glyphScale);
    }

    private static CustomGlyphs glyph(char chr) {
        return GlyphsRegistry.getCandidate(chr);
    }

    @Override
    public char getRandomReplacement(char chr) {
        return chr;
    }

    @Override
    public boolean isGlyphAvailable(char chr) {
        return glyph(chr) != null;
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
        return g != null ? g.getFinalCharacterWidth() : 0;
    }

    @Override
    public float getGlyphW(char chr) {
        final CustomGlyphs g = glyph(chr);
        if (g == null) return 0;

        switch (g.getFitMode()) {
            case CONTAINS:
                return 9;
            case VERTICALLY:
                return (float) Math.ceil(MathUtils.calculateWidth(g.getWidth(), g.getHeight(), 9));
            default:
                return g.getWidth();
        }
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
        return g != null ? g.getResource() : null;
    }

    @Override
    public float getYScaleMultiplier() {
        return this.glyphScale;
    }
}
