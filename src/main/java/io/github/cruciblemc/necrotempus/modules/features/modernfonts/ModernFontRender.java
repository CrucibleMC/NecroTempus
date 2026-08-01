package io.github.cruciblemc.necrotempus.modules.features.modernfonts;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureManager;

import org.lwjgl.opengl.GL11;

public class ModernFontRender {

    public static float renderGlyph(TextureManager textureManager, ModernFontEntry entry, float posX, float posY,
        float itOff, boolean flipV, float r, float g, float b, float a) {

        textureManager.bindTexture(entry.location);
        drawGlyphAtlas(posX, posY, entry, itOff, flipV, r, g, b, a);

        return entry.width + 1;

    }

    private static void drawGlyphAtlas(float x, float y, ModernFontEntry entry, float itOff, boolean flipV, float r,
        float g, float b, float a) {

        float glyphPixelX = entry.atlasX * entry.frameWidth;
        float glyphPixelY = entry.atlasY * entry.frameHeight;

        float u0 = glyphPixelX / entry.totalWidth;
        float v0 = glyphPixelY / entry.totalHeight;
        float u1 = (glyphPixelX + entry.width) / entry.totalWidth;
        float v1 = (glyphPixelY + entry.height) / entry.totalHeight;

        y += (7.0F - entry.ascent);

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
}
