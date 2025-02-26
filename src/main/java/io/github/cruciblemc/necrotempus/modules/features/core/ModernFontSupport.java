package io.github.cruciblemc.necrotempus.modules.features.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.cruciblemc.necrotempus.NecroTempus;
import io.github.cruciblemc.necrotempus.Tags;
import io.github.cruciblemc.necrotempus.utils.TextureUtils;
import lombok.AllArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import net.minecraft.util.ResourceLocation;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ModernFontSupport implements IResourceManagerReloadListener {

    public static void init() {
        ((SimpleReloadableResourceManager) Minecraft.getMinecraft().getResourceManager()).registerReloadListener(new ModernFontSupport());
    }

    private static final ResourceLocation ACCENTED = new ResourceLocation(Tags.MODID, "textures/fonts/accented.png");
    private static final ResourceLocation NON_LATIN_EUROPEAN = new ResourceLocation(Tags.MODID, "textures/fonts/nonlatin_european.png");

    private static final ConcurrentHashMap<Character, ModernFontEntry> MODERN_FONT_CHARACTERS = new ConcurrentHashMap<>();

    public static ModernFontEntry getCandidate(Character character) {
        return MODERN_FONT_CHARACTERS.get(character);
    }

    @Override

    public void onResourceManagerReload(IResourceManager resourceManager) {

        Logger logger = NecroTempus.getInstance().getLogger();
        JsonParser jsonParser = new JsonParser();

        ArrayList<BakedModernFont> fonts = new ArrayList<>();

        for (Object domain : resourceManager.getResourceDomains()) {

            if (domain instanceof String dom) {

                try {

                    IResource resourceFile = resourceManager.getResource(new ResourceLocation(dom, "glyphs/modern_fonts.json"));

                    JsonElement jsonElement = jsonParser.parse(new InputStreamReader(resourceFile.getInputStream()));
                    JsonArray jsonArray = jsonElement.getAsJsonObject().getAsJsonArray("providers");

                    if (jsonArray.size() == 0) {
                        logger.info("Resource domain ({}) has modern_fonts.json file, but this file is empty.", domain);
                        continue;
                    }

                    int loaded = 0;

                    for (JsonElement entry : jsonArray) {

                        try {

                            JsonObject jsonObject = entry.getAsJsonObject();

                            String file = jsonObject.get("file").getAsString();

                            int height = 8;
                            int ascent = 7;

                            if (jsonObject.has("height"))
                                height = jsonObject.get("height").getAsInt();

                            if (jsonObject.has("ascent"))
                                ascent = jsonObject.get("ascent").getAsInt();

                            ArrayList<char[]> chars = new ArrayList<>();

                            for (JsonElement element : jsonObject.get("chars").getAsJsonArray()) {

                                String line = element.getAsString();

                                if (line.indexOf('\ud800') != -1)
                                    line = line.replaceAll("\ud800", "");

                                chars.add(line.toCharArray());
                            }

                            char[][] data = new char[chars.size()][];

                            for (int i = 0; i < chars.size(); i++)
                                data[i] = chars.get(i);

                            ModernFont modernFont = new ModernFont(height, ascent, data, file);
                            BakedModernFont bakedModernFont = FontBaker.cook(modernFont);

                            fonts.add(bakedModernFont);

                            loaded++;

                        } catch (Exception ignored) {
                            logger.error("Fail to parse a modern_fonts.json {{}}", entry.toString());
                            ignored.printStackTrace();
                        }

                    }

                    logger.info("Resource domain ({}) has modern_fonts.json file, registered {} elements.", domain, loaded);

                } catch (Exception ignored) {

                }
            }
        }

        logger.info("ModernFont baked {} characters.", fonts.stream().mapToInt(el -> el.elements.size()).sum());

        for (BakedModernFont bakedModernFont : fonts) {
            for (Map.Entry<Character, int[]> element : bakedModernFont.elements.entrySet()) {
                int[] values = element.getValue();
                MODERN_FONT_CHARACTERS.put(element.getKey(), new ModernFontEntry(values[1], values[0], values[2], bakedModernFont.height, bakedModernFont.ascent, values[3], values[4], values[5], values[6], bakedModernFont.resourceLocation));
            }
        }

        logger.info("ModernFont have {} unique characters registered.", MODERN_FONT_CHARACTERS.size());
        MODERN_FONT_CHARACTERS.entrySet().forEach(logger::info);

    }


    @AllArgsConstructor
    public static class ModernFont {

        public int height;
        public int ascent;

        public char[][] chars;

        public String source;

    }

    @AllArgsConstructor
    public static class BakedModernFont {

        public int height;
        public int ascent;

        HashMap<Character, int[]> elements;
        public ResourceLocation resourceLocation;

    }

    @AllArgsConstructor
    public static class ModernFontEntry {

        public int atlasX;
        public int atlasY;

        public int width;
        public int height;

        public int ascent;

        public int frameWidth;
        public int frameHeight;

        public int totalWidth;
        public int totalHeight;

        public ResourceLocation location;

        @Override
        public String toString() {
            return "ModernFontEntry{" +
                    "atlasX=" + atlasX +
                    ", atlasY=" + atlasY +
                    ", width=" + width +
                    ", height=" + height +
                    ", ascent=" + ascent +
                    ", frameWidth=" + frameWidth +
                    ", frameHeight=" + frameHeight +
                    ", totalWidth=" + totalWidth +
                    ", totalHeight=" + totalHeight +
                    ", location=" + location +
                    '}';
        }
    }

    public static class FontBaker {

        public static BakedModernFont cook(ModernFont modernFont) {

            ResourceLocation resourceLocation =
                    modernFont.source.equals("ACCENTED") ? ACCENTED :
                            modernFont.source.equals("NON_LATIN_EUROPEAN") ? NON_LATIN_EUROPEAN : null;

            if (resourceLocation == null)
                return null;

            HashMap<Character, int[]> elements = new HashMap<>();
            BufferedImage bufferedImage = TextureUtils.getBufferedImageFromResource(resourceLocation);

            if (bufferedImage == null)
                return null;

            Rectangle rectangle = bufferedImage.getData().getBounds();

            int pieceWidth = rectangle.width / 16;
            int pieceHeight = rectangle.height / modernFont.chars.length;

            for (int y = 0; y < modernFont.chars.length; y++) {
                for (int x = 0; x < Math.min(modernFont.chars[y].length, 16); x++) {

                    char _char = modernFont.chars[y][x];

                    if (_char == '\u0000' || _char == '\ud800')
                        continue;

                    int glyphX = x * pieceWidth;
                    int glyphY = y * pieceHeight;

                    int calculatedWidth = getGlyphWidth(bufferedImage, pieceWidth, pieceHeight, glyphX, glyphY);

                    elements.put(_char, new int[]{y, x, calculatedWidth, pieceWidth, pieceHeight, rectangle.width, rectangle.height});
                }
            }

            return new BakedModernFont(modernFont.height, modernFont.ascent, elements, resourceLocation);
        }

        public static int getGlyphWidth(BufferedImage image, int glyphWidth, int glyphHeight, int glyphX, int glyphY) {
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

}
