package io.github.cruciblemc.necrotempus.modules.features.modernfonts;

import java.io.InputStreamReader;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import net.minecraft.util.ResourceLocation;

import org.apache.logging.log4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.github.cruciblemc.necrotempus.NecroTempus;
import io.github.cruciblemc.necrotempus.NecroTempusConfig;
import it.unimi.dsi.fastutil.chars.Char2ObjectOpenHashMap;

public class ModernFontSupport implements IResourceManagerReloadListener {

    public static void init() {

        if (!NecroTempusConfig.modernFonts) return;

        ((SimpleReloadableResourceManager) Minecraft.getMinecraft()
            .getResourceManager()).registerReloadListener(new ModernFontSupport());

    }

    private static Char2ObjectOpenHashMap<ModernFontEntry> MODERN_FONT_CHARACTERS = new Char2ObjectOpenHashMap<>();

    public static ModernFontEntry getCandidate(char character) {
        return MODERN_FONT_CHARACTERS.get(character);
    }

    public static boolean hasCandidate(char character) {
        return MODERN_FONT_CHARACTERS.containsKey(character);
    }

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager) {

        Logger logger = NecroTempus.getInstance()
            .getLogger();

        if (!MODERN_FONT_CHARACTERS.isEmpty()) {
            logger.info(
                "Modern Font has detected a texture reload; this modification will be ignored. To reload this system, you need to reload the game.");
            return;
        }

        Char2ObjectOpenHashMap<ModernFontEntry> fontMap = new Char2ObjectOpenHashMap<>();

        JsonParser jsonParser = new JsonParser();

        for (Object domain : resourceManager.getResourceDomains()) {

            if (domain instanceof String dom) {

                try {

                    IResource resourceFile = resourceManager
                        .getResource(new ResourceLocation(dom, "glyphs/modern_fonts.json"));

                    JsonElement jsonElement = jsonParser.parse(new InputStreamReader(resourceFile.getInputStream()));
                    JsonArray providers = jsonElement.getAsJsonObject()
                        .getAsJsonArray("providers");

                    if (providers.size() == 0) {
                        logger.info("Resource domain ({}) has modern_fonts.json file, but this file is empty.", domain);
                        continue;
                    }

                    int loaded = 0;

                    for (JsonElement entry : providers) {

                        JsonObject jsonObject = entry.getAsJsonObject();

                        String resource = jsonObject.get("file")
                            .getAsString();
                        JsonArray chars = jsonObject.get("chars")
                            .getAsJsonArray();

                        int height = 8;
                        int ascent = 7;

                        if (jsonObject.has("height")) height = jsonObject.get("height")
                            .getAsInt();

                        if (jsonObject.has("ascent")) ascent = jsonObject.get("ascent")
                            .getAsInt();

                        int[][] data = new int[chars.size()][];

                        for (int i = 0; i < chars.size(); i++) data[i] = chars.get(i)
                            .getAsString()
                            .codePoints()
                            .toArray();

                        ModernFontProcessor.process(fontMap, resource, data, height, ascent);

                        loaded++;

                    }

                    logger.info(
                        "Resource domain ({}) has modern_fonts.json file, registered {} elements.",
                        domain,
                        loaded);

                } catch (Exception e) {
                    logger.error("Failed to read modern fonts from domain {}: {}", domain, e.getMessage());
                }
            }
        }

        if (!fontMap.isEmpty()) {
            MODERN_FONT_CHARACTERS = fontMap;
        } else {
            logger.info(
                "ModernFont not have detected any characters, maybe restart the game should help. Any modification was not applied on already registered characters");
        }

        logger.info("ModernFont have {} unique characters registered.", MODERN_FONT_CHARACTERS.size());

    }

}
