package io.github.cruciblemc.necrotempus.modules.features.playertab.client;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import cpw.mods.fml.common.Loader;
import io.github.cruciblemc.necrotempus.NecroTempus;
import io.github.cruciblemc.necrotempus.NecroTempusConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.IImageBuffer;
import net.minecraft.client.renderer.ImageBufferDownload;
import net.minecraft.client.renderer.ThreadDownloadImageData;
import net.minecraft.client.resources.SkinManager.SkinAvailableCallback;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.awt.image.BufferedImage;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

public final class PlayerSkinTextures {

    private static final long RETRY_MILLIS = 60_000L;
    private static final Pattern UNRESOLVED_PLACEHOLDER = Pattern.compile("%[^%]+%");
    private static final Map<String, Download> DOWNLOADS = new HashMap<>();
    private static boolean backportWarningLogged;

    private PlayerSkinTextures() {}

    public static ResourceLocation getEntitySkin(Minecraft minecraft, GameProfile profile) {
        if (minecraft.theWorld == null || profile == null) return null;

        for (Object player : minecraft.theWorld.playerEntities) {
            if (!(player instanceof AbstractClientPlayer)) continue;

            AbstractClientPlayer clientPlayer = (AbstractClientPlayer) player;
            GameProfile current = clientPlayer.getGameProfile();
            if (matchesProfile(profile, current)) return clientPlayer.getLocationSkin();
        }

        return null;
    }

    static boolean matchesProfile(GameProfile requested, GameProfile current) {
        if (requested == null || current == null) return false;
        UUID requestedId = requested.getId();
        if (requestedId != null) return requestedId.equals(current.getId());
        return requested.getName() != null && requested.getName().equalsIgnoreCase(current.getName());
    }

    public static ResourceLocation getProfileSkin(Minecraft minecraft, GameProfile profile) {
        if (profile == null) return null;

        try {
            Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> textures = minecraft.func_152342_ad()
                .func_152788_a(profile);
            MinecraftProfileTexture skin = textures == null ? null : textures.get(MinecraftProfileTexture.Type.SKIN);
            return skin == null ? null : minecraft.func_152342_ad()
                .func_152792_a(skin, MinecraftProfileTexture.Type.SKIN);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    public static ResourceLocation getFallbackSkin(GameProfile profile) {
        String url = buildFallbackSkinUrl(profile);
        if (url == null) return null;

        Minecraft minecraft = Minecraft.getMinecraft();
        Download download = DOWNLOADS.get(url);
        long now = System.currentTimeMillis();

        if (download != null && (download.imageLoaded || now - download.startedAt < RETRY_MILLIS))
            return download.location;

        ResourceLocation location = new ResourceLocation(
            "necrotempus",
            "skins/" + UUID.nameUUIDFromBytes(url.getBytes(StandardCharsets.UTF_8)));
        MinecraftProfileTexture texture = createProfileTexture(url);
        if (texture == null) return null;
        if (download != null) minecraft.getTextureManager()
            .deleteTexture(download.location);

        Download current = new Download(location, now);
        DOWNLOADS.put(url, current);

        IImageBuffer parser = createSkinParser(texture);
        IImageBuffer trackingParser = new IImageBuffer() {
            @Override
            public BufferedImage parseUserSkin(BufferedImage image) {
                BufferedImage parsed = parser.parseUserSkin(image);
                current.imageLoaded = parsed != null;
                return parsed;
            }

            @Override
            public void func_152634_a() {
                parser.func_152634_a();
            }
        };

        ThreadDownloadImageData image = new ThreadDownloadImageData(
            null,
            url,
            AbstractClientPlayer.locationStevePng,
            trackingParser);
        minecraft.getTextureManager()
            .loadTexture(location, image);
        return location;
    }

    private static MinecraftProfileTexture createProfileTexture(String url) {
        return new MinecraftProfileTexture(url, Collections.emptyMap());
    }

    private static IImageBuffer createSkinParser(MinecraftProfileTexture texture) {
        if (Loader.isModLoaded("simpleskinbackport")) {
            try {
                Class<?> parserClass = Class.forName(
                    "roadhog360.simpleskinbackport.client.ImageBufferDownloadPlayerSkin");
                Constructor<?> constructor = parserClass.getConstructor(
                    MinecraftProfileTexture.class,
                    SkinAvailableCallback.class);
                return (IImageBuffer) constructor.newInstance(texture, null);
            } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
                if (!backportWarningLogged) {
                    backportWarningLogged = true;
                    if (NecroTempus.getInstance() != null && NecroTempus.getInstance()
                        .getLogger() != null) {
                        NecroTempus.getInstance()
                            .getLogger()
                            .warn("SimpleSkinBackport parser unavailable; using the vanilla skin parser.", exception);
                    }
                }
            }
        }

        return new ImageBufferDownload();
    }

    public static void clear() {
        Minecraft minecraft = Minecraft.getMinecraft();
        Runnable cleanup = () -> {
            for (Download download : DOWNLOADS.values())
                minecraft.getTextureManager()
                    .deleteTexture(download.location);
            DOWNLOADS.clear();
        };

        if (minecraft.func_152345_ab()) cleanup.run();
        else minecraft.func_152344_a(cleanup);
    }

    public static void drawBoundHead(int x, int y) {
        int width = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
        int height = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
        int scale = getSkinScale(width, height);

        if (scale == 0) {
            Minecraft.getMinecraft()
                .getTextureManager()
                .bindTexture(AbstractClientPlayer.locationStevePng);
            width = 64;
            height = 32;
            scale = 1;
        }

        drawHeadLayer(x, y, 8 * scale, width, height);
        drawHeadLayer(x, y, 40 * scale, width, height);
    }

    private static void drawHeadLayer(int x, int y, int u, int width, int height) {
        Gui.func_152125_a(x, y, u, 8, 8, 8, 8, 8, width, height);
    }

    static String buildFallbackSkinUrl(GameProfile profile) {
        if (!NecroTempusConfig.enableHeadsFallback || profile == null) return null;

        String url = NecroTempusConfig.headsFallbackURL;
        if (url == null || url.isEmpty()) return null;

        if (url.contains("%name%")) {
            String name = profile.getName();
            if (!isValidSkinLookupName(name)) return null;
            url = url.replace("%name%", name);
        }

        UUID id = profile.getId();
        if (url.contains("%uuid%") || url.contains("%uuidTrim%")) {
            if (id == null) return null;
            String uuid = id.toString();
            url = url.replace("%uuid%", uuid)
                .replace("%uuidTrim%", uuid.replace("-", ""));
        }

        return UNRESOLVED_PLACEHOLDER.matcher(url)
            .find() ? null : url;
    }

    private static boolean isValidSkinLookupName(String name) {
        if (name == null || name.isEmpty() || name.length() > 16) return false;

        for (int i = 0; i < name.length(); i++) {
            char character = name.charAt(i);
            if (!((character >= 'A' && character <= 'Z') || (character >= 'a' && character <= 'z')
                || (character >= '0' && character <= '9') || character == '_')) return false;
        }

        return true;
    }

    static int getSkinScale(int width, int height) {
        if (width <= 0 || width % 64 != 0) return 0;
        return height == width || height * 2 == width ? width / 64 : 0;
    }

    private static final class Download {

        private final ResourceLocation location;
        private final long startedAt;
        private volatile boolean imageLoaded;

        private Download(ResourceLocation location, long startedAt) {
            this.location = location;
            this.startedAt = startedAt;
        }
    }
}
