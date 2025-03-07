package io.github.cruciblemc.necrotempus.modules.features.modernfonts;

import io.github.cruciblemc.necrotempus.utils.TextureUtils;
import it.unimi.dsi.fastutil.chars.Char2ObjectArrayMap;
import net.minecraft.util.ResourceLocation;

import java.awt.*;
import java.awt.image.BufferedImage;

public class ModernFontProcessor {

    public static void process(Char2ObjectArrayMap<ModernFontEntry> map, String source, int[][] chars, int height, int ascent) {

        ResourceLocation resourceLocation = new ResourceLocation(source);

        BufferedImage bufferedImage = TextureUtils.getBufferedImageFromResource(resourceLocation);

        if (bufferedImage == null)
            return;

        Rectangle rectangle = bufferedImage.getData().getBounds();

        int maxRow = 0;
        for (int[] rows : chars) {
            maxRow = Math.max(maxRow, rows.length);
        }

        int pieceWidth = rectangle.width / maxRow;
        int pieceHeight = rectangle.height / chars.length;

        for (int y = 0; y < chars.length; y++) {
            for (int x = 0; x < chars[y].length; x++) {

                int character = chars[y][x];

                if (character == '\u0000')
                    continue;

                int glyphX = x * pieceWidth;
                int glyphY = y * pieceHeight;

                int calculatedWidth = calculateGlyphWidth(bufferedImage, pieceWidth, pieceHeight, glyphX, glyphY);

                ModernFontEntry entry = ModernFontEntry.builder()
                        .location(resourceLocation)
                        .atlasX(x)
                        .atlasY(y)
                        .width(calculatedWidth)
                        .height(height)
                        .ascent(ascent)
                        .frameWidth(pieceWidth)
                        .frameHeight(pieceHeight)
                        .totalWidth(rectangle.width)
                        .totalHeight(rectangle.height)
                        .build();

                map.put((char) character, entry);

            }
        }

    }

    public static int calculateGlyphWidth(BufferedImage image, int glyphWidth, int glyphHeight, int glyphX, int glyphY) {
        int effectiveWidth = 0;
        for (int x = glyphX; x < glyphX + glyphWidth; x++) {
            boolean hasVisiblePixel = false;
            for (int y = glyphY; y < glyphY + glyphHeight; y++) {
                int pixel = image.getRGB(x, y);
                int alpha = (pixel >> 24) & 0xFF;
                if (alpha > 0) {
                    hasVisiblePixel = true;
                    break;
                }
            }
            if (hasVisiblePixel) {
                effectiveWidth = (x - glyphX) + 1;
            }
        }
        return effectiveWidth;
    }


}
