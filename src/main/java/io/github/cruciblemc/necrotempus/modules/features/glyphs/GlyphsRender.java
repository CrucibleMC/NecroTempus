package io.github.cruciblemc.necrotempus.modules.features.glyphs;

import io.github.cruciblemc.necrotempus.modules.features.modernfonts.ModernFontEntry;
import io.github.cruciblemc.necrotempus.utils.MathUtils;
import lombok.SneakyThrows;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureManager;
import org.lwjgl.opengl.GL11;

public class GlyphsRender {

    @SneakyThrows
    public static float renderGlyph(TextureManager textureManager, ModernFontEntry entry,
                                    float posX, float posY, boolean shadow) {

        GL11.glPushMatrix();
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);

        textureManager.bindTexture(entry.location);

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_DEPTH_TEST);

        drawGlyphAtlas(posX, posY, entry, shadow);

        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopAttrib();
        GL11.glPopMatrix();

        return entry.width + 1;

    }


    @SneakyThrows
    public static float renderGlyph(TextureManager textureManager, CustomGlyphs customGlyphs, float posX, float posY, boolean shadow, float alpha) {

        if (!shadow) {
            GL11.glPushMatrix();
            GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
            GL11.glColor4f(1, 1, 1, alpha);

            textureManager.bindTexture(customGlyphs.getResource());

            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glEnable(GL11.GL_DEPTH_TEST);

            float realX = posX + (customGlyphs.getHorizontalPadding() * -1);
            float realY = posY + (customGlyphs.getVerticalPadding() * -1);

            if (customGlyphs.getFitMode() != CustomGlyphs.FitMode.NONE) {
                drawGlyphContains(realX, realY, customGlyphs);
            } else
                drawGlyph(realX, realY, customGlyphs.getWidth(), customGlyphs.getHeight());

            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glPopAttrib();
            GL11.glPopMatrix();


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

    private static void drawGlyphAtlas(float x, float y, ModernFontEntry entry, boolean shadow) {

        Tessellator ts = Tessellator.instance;

        float glyphPixelX = entry.atlasX * entry.frameWidth;
        float glyphPixelY = entry.atlasY * entry.frameHeight;

        float u0 = glyphPixelX / entry.totalWidth;
        float v0 = glyphPixelY / entry.totalHeight;
        float u1 = (glyphPixelX + entry.width) / entry.totalWidth;
        float v1 = (glyphPixelY + entry.height) / entry.totalHeight;

        float offset = shadow ? 1.0F : 0.0F;

        y += (7.0F - entry.ascent);

        ts.startDrawingQuads();

        addToZero(ts, x - offset, y + entry.height, u0, v1);            // Bottom-left
        addToZero(ts, x + entry.width - offset, y + entry.height, u1, v1); // Bottom-right
        addToZero(ts, x + entry.width + offset, y, u1, v0);                // Top-right
        addToZero(ts, x + offset, y, u0, v0);                              // Top-left

        ts.draw();

    }


    private static void drawGlyphContains(float x, float y, CustomGlyphs customGlyphs) {

        CustomGlyphs.FitMode fitMode = customGlyphs.getFitMode();

        y--;

        float height = 9;

        float width = (fitMode == CustomGlyphs.FitMode.CONTAINS) ? 9 : MathUtils.calculateWidth(
                customGlyphs.getWidth(),
                customGlyphs.getHeight(),
                9
        );

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
        tessellator.addVertexWithUV(x, y, 50F, textureX, textureY);
    }

    private static void addToZero(Tessellator tessellator, float x, float y, float textureX, float textureY) {
        tessellator.addVertexWithUV(x, y, 0F, textureX, textureY);
    }

}
