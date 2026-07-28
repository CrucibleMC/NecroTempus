package io.github.cruciblemc.necrotempus.modules.features.modernfonts.compat.angelica;

import net.minecraft.util.ResourceLocation;

import com.gtnewhorizons.angelica.client.font.FontProvider;

import io.github.cruciblemc.necrotempus.modules.features.modernfonts.ModernFontEntry;
import io.github.cruciblemc.necrotempus.modules.features.modernfonts.ModernFontSupport;

public class AngelicaModernFontProvider implements FontProvider {

    private final float fontScale;

    private AngelicaModernFontProvider(float fontScale) {
        this.fontScale = fontScale;
    }

    public static AngelicaModernFontProvider forScale(float fontScale) {
        return new AngelicaModernFontProvider(fontScale);
    }

    private static ModernFontEntry font(char chr) {
        return ModernFontSupport.getCandidate(chr);
    }

    @Override
    public char getRandomReplacement(char chr) {
        return chr;
    }

    @Override
    public boolean isGlyphAvailable(char chr) {
        return font(chr) != null;
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
        final ModernFontEntry entry = font(chr);
        return entry != null ? entry.width + 1 : 0;
    }

    @Override
    public float getGlyphW(char chr) {
        final ModernFontEntry entry = font(chr);
        return entry != null ? entry.width : 0;
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
        final ModernFontEntry entry = font(chr);
        return entry != null ? entry.location : null;
    }

    @Override
    public float getYScaleMultiplier() {
        return this.fontScale;
    }
}
