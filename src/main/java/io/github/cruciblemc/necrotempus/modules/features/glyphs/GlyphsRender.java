package io.github.cruciblemc.necrotempus.modules.features.glyphs;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureManager;

import org.lwjgl.opengl.GL11;

import io.github.cruciblemc.necrotempus.modules.features.modernfonts.ModernFontEntry;
import io.github.cruciblemc.necrotempus.utils.MathUtils;
import lombok.SneakyThrows;

public class GlyphsRender {

    @SneakyThrows
    public static float renderGlyph(TextureManager textureManager, ModernFontEntry entry, float posX, float posY,
        float itOff, boolean flipV, float r, float g, float b, float a) {

        textureManager.bindTexture(entry.location);
        drawGlyphAtlas(posX, posY, entry, itOff, flipV, r, g, b, a);

        return entry.width + 1;

    }

    @SneakyThrows
    public static float renderGlyph(TextureManager textureManager, CustomGlyphs customGlyphs, float posX, float posY,
        boolean shadow, float alpha) {

        if (!shadow) {
            GL11.glColor4f(1, 1, 1, alpha);

            textureManager.bindTexture(customGlyphs.getResource());

            float realX = posX + (customGlyphs.getHorizontalPadding() * -1);
            float realY = posY + (customGlyphs.getVerticalPadding() * -1);

            if (customGlyphs.getFitMode() != CustomGlyphs.FitMode.NONE) drawGlyphContains(realX, realY, customGlyphs);
            else drawGlyph(realX, realY, customGlyphs.getWidth(), customGlyphs.getHeight());

        }

        return customGlyphs.getFinalCharacterWidth();
    }

    private static void drawGlyph(float x, float y, int width, int height) {

        Tessellator ts = Tessellator.instance;

        ts.startDrawingQuads();

        add(ts, (x), (y + height), 0, 1);
        add(ts, (x + width), (y + height), 1, 1);
        add(ts, (x + width), (y), 1, 0);
        add(ts, (x), (y), 0, 0);

        ts.draw();

    }

    /**
     * Draws a glyph quad using Minecraft's Tessellator (GL_COLOR_ARRAY + glDrawArrays),
     * matching the approach used by CustomGlyphs which renders correctly regardless of
     * GL_LIGHTING state. The caller must set glColor4f before calling this method.
     */
    private static void drawGlyphAtlas(float x, float y, ModernFontEntry entry, float itOff, boolean flipV, float r,
        float g, float b, float a) {

        float glyphPixelX = entry.atlasX * entry.frameWidth;
        float glyphPixelY = entry.atlasY * entry.frameHeight;

        float u0 = glyphPixelX / entry.totalWidth;
        float v0 = glyphPixelY / entry.totalHeight;
        float u1 = (glyphPixelX + entry.width) / entry.totalWidth;
        float v1 = (glyphPixelY + entry.height) / entry.totalHeight;

        y += (7.0F - entry.ascent);

        // Set current color so the Tessellator (which does not use GL_COLOR_ARRAY
        // when hasColor=false) picks it up from the current GL state.
        GL11.glColor4f(r, g, b, a);

        Tessellator ts = Tessellator.instance;
        ts.startDrawingQuads();

        if (flipV) {
            ts.addVertexWithUV(x - itOff, y + entry.height, 0.0, u0, v0);
            ts.addVertexWithUV(x + entry.width - itOff, y + entry.height, 0.0, u1, v0);
            ts.addVertexWithUV(x + entry.width + itOff, y, 0.0, u1, v1);
            ts.addVertexWithUV(x + itOff, y, 0.0, u0, v1);
        } else {
            ts.addVertexWithUV(x - itOff, y + entry.height, 0.0, u0, v1);
            ts.addVertexWithUV(x + entry.width - itOff, y + entry.height, 0.0, u1, v1);
            ts.addVertexWithUV(x + entry.width + itOff, y, 0.0, u1, v0);
            ts.addVertexWithUV(x + itOff, y, 0.0, u0, v0);
        }

        ts.draw();
    }

    private static void drawGlyphContains(float x, float y, CustomGlyphs customGlyphs) {

        CustomGlyphs.FitMode fitMode = customGlyphs.getFitMode();

        y--;

        float height = 9;

        float width = (fitMode == CustomGlyphs.FitMode.CONTAINS) ? 9
            : MathUtils.calculateWidth(customGlyphs.getWidth(), customGlyphs.getHeight(), 9);

        float nW = 1F / width;
        float nH = 1F / height;

        Tessellator ts = Tessellator.instance;

        ts.startDrawingQuads();

        add(ts, (x), (y + height), 0, (height) * nH);
        add(ts, (x + width), (y + height), (width) * nW, (height) * nH);
        add(ts, (x + width), (y), (width) * nW, 0);
        add(ts, (x), (y), 0, 0);

        ts.draw();
    }

    private static void add(Tessellator tessellator, float x, float y, float textureX, float textureY) {
        tessellator.addVertexWithUV(x, y, 0F, textureX, textureY);
    }

}
