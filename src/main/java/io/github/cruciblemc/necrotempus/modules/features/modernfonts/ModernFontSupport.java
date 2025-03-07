package io.github.cruciblemc.necrotempus.modules.features.modernfonts;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.cruciblemc.necrotempus.NecroTempus;
import it.unimi.dsi.fastutil.chars.Char2ObjectArrayMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import net.minecraft.util.ResourceLocation;
import org.apache.logging.log4j.Logger;

import java.io.InputStreamReader;

public class ModernFontSupport implements IResourceManagerReloadListener {

    public static void init() {
        ((SimpleReloadableResourceManager) Minecraft.getMinecraft().getResourceManager()).registerReloadListener(new ModernFontSupport());
    }

    private static Char2ObjectArrayMap<ModernFontEntry> MODERN_FONT_CHARACTERS = new Char2ObjectArrayMap<>();

    public static ModernFontEntry getCandidate(char character) {
        return MODERN_FONT_CHARACTERS.get(character);
    }

    public static boolean hasCandidate(char character) {
        return MODERN_FONT_CHARACTERS.containsKey(character);
    }

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager) {

        Char2ObjectArrayMap<ModernFontEntry> fontMap = new Char2ObjectArrayMap<>();

        Logger logger = NecroTempus.getInstance().getLogger();

        JsonParser jsonParser = new JsonParser();

        for (Object domain : resourceManager.getResourceDomains()) {

            if (domain instanceof String dom) {

                try {

                    IResource resourceFile = resourceManager.getResource(new ResourceLocation(dom, "glyphs/modern_fonts.json"));

                    JsonElement jsonElement = jsonParser.parse(new InputStreamReader(resourceFile.getInputStream()));
                    JsonArray providers = jsonElement.getAsJsonObject().getAsJsonArray("providers");

                    if (providers.size() == 0) {
                        logger.info("Resource domain ({}) has modern_fonts.json file, but this file is empty.", domain);
                        continue;
                    }

                    int loaded = 0;

                    for (JsonElement entry : providers) {


                        JsonObject jsonObject = entry.getAsJsonObject();

                        String resource = jsonObject.get("file").getAsString();
                        JsonArray chars = jsonObject.get("chars").getAsJsonArray();

                        int height = 8;
                        int ascent = 7;

                        if (jsonObject.has("height"))
                            height = jsonObject.get("height").getAsInt();

                        if (jsonObject.has("ascent"))
                            ascent = jsonObject.get("ascent").getAsInt();


                        int[][] data = new int[chars.size()][];

                        for (int i = 0; i < chars.size(); i++)
                            data[i] = chars.get(i).getAsString().codePoints().toArray();

                        ModernFontProcessor.process(fontMap, resource, data, height, ascent);

                        loaded++;

                    }

                    logger.info("Resource domain ({}) has modern_fonts.json file, registered {} elements.", domain, loaded);

                } catch (Exception exception) {
                    logger.info("ModernFont throws an exception when loading files. {}", exception.getMessage());
                }
            }
        }

        MODERN_FONT_CHARACTERS = fontMap;
        logger.info("ModernFont have {} unique characters registered.", MODERN_FONT_CHARACTERS.size());

    }


}
