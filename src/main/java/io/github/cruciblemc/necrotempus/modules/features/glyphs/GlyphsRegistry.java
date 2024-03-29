package io.github.cruciblemc.necrotempus.modules.features.glyphs;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.cruciblemc.necrotempus.NecroTempus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import net.minecraft.util.ResourceLocation;
import org.apache.logging.log4j.Logger;

import java.io.InputStreamReader;
import java.util.concurrent.ConcurrentHashMap;

public class GlyphsRegistry implements IResourceManagerReloadListener {

    public static void init() {
        ((SimpleReloadableResourceManager) Minecraft.getMinecraft().getResourceManager()).registerReloadListener(new GlyphsRegistry());
    }

    private static final ConcurrentHashMap<Character, CustomGlyphs> GLYPHS_REGISTRY = new ConcurrentHashMap<>();

    public static CustomGlyphs getCandidate(char key) {
        return GLYPHS_REGISTRY.get(key);
    }

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager) {

        Logger logger = NecroTempus.getInstance().getLogger();
        JsonParser jsonParser = new JsonParser();

        GLYPHS_REGISTRY.clear();

        for (Object domain : resourceManager.getResourceDomains()) {

            if (domain instanceof String dom) {

                try {

                    IResource resourceFile = resourceManager.getResource(new ResourceLocation(dom, "glyphs/glyphs.json"));
                    JsonElement jsonElement = jsonParser.parse(new InputStreamReader(resourceFile.getInputStream()));
                    JsonArray jsonArray = jsonElement.getAsJsonObject().getAsJsonArray("glyphs");

                    if (jsonArray.size() == 0) {
                        logger.info(String.format("Resource domain (%s) has glyphs.json file, but this file is empty.", domain));
                        continue;
                    }

                    int loaded = 0;
                    for (JsonElement entry : jsonArray) {

                        try {

                            JsonObject jsonObject = entry.getAsJsonObject();

                            char target = jsonObject.get("target").getAsCharacter();
                            String resource = jsonObject.get("resource").getAsString();
                            int horizontalPadding = jsonObject.get("horizontalPadding").getAsInt();
                            int verticalPadding = jsonObject.get("verticalPadding").getAsInt();

                            int width = jsonObject.get("width").getAsInt();
                            int height = jsonObject.get("height").getAsInt();

                            CustomGlyphs.FitMode fitMode = CustomGlyphs.FitMode.parse(jsonObject.get("fitMode").getAsString());

                            int charWidth = -1;

                            if (jsonObject.has("charWidth")) {
                                charWidth = jsonObject.get("charWidth").getAsInt();
                            }

                            CustomGlyphs customGlyphs = new CustomGlyphs(
                                    target,
                                    new ResourceLocation(resource),
                                    horizontalPadding,
                                    verticalPadding,
                                    width,
                                    height
                            );

                            customGlyphs.setFitMode(fitMode);
                            customGlyphs.setCharWidth(charWidth);

                            GLYPHS_REGISTRY.put(target, customGlyphs);
                            loaded++;

                        } catch (Exception ignored) {
                            logger.error(String.format("Fail to parse a glyph {%s}", entry.toString()));
                        }

                    }

                    logger.info(String.format("Resource domain (%s) has glyphs.json file, registered %d elements", domain, loaded));

                } catch (Exception ignored) {
                }
            }
        }
    }
}
