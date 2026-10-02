package io.github.cruciblemc.necrotempus.modules.features.chatheads.client.render;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiPlayerInfo;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import com.mojang.authlib.GameProfile;

import io.github.cruciblemc.necrotempus.modules.features.playertab.client.render.PlayerTabGui;

public class ChatHeadRenderer {

    public static final int CHAT_HEAD_WIDTH = 10;

    private static final int PLAYER_NAME_CACHE_MILLIS = 1_000;
    private static final int SKIN_HEAD_SIZE = 8;
    private static final float SKIN_TEXTURE_WIDTH = 64.0F;
    private static final float SKIN_HEAD_U = 8.0F;
    private static final float SKIN_HEAD_V = 8.0F;
    private static final float SKIN_HEAD_OVERLAY_U = 40.0F;

    private static final Minecraft MINECRAFT = Minecraft.getMinecraft();
    private static Map<Character, List<String>> cachedNamesByFirstCharacter = new HashMap<>();
    private static final Map<ChatLine, FoundSender> CHAT_LINE_SENDERS = Collections
        .synchronizedMap(new WeakHashMap<>());
    private static Object cachedWorld;
    private static int cachedWorldPlayerCount = -1;
    private static int cachedTabPlayerCount = -1;
    private static long nextKnownPlayerNameRefresh;

    private ChatHeadRenderer() {}

    public static FoundSender findSender(ChatLine chatLine) {
        if (chatLine == null || chatLine.func_151461_a() == null) return null;

        FoundSender cachedSender = CHAT_LINE_SENDERS.get(chatLine);

        if (cachedSender != null) return cachedSender;

        FoundSender sender = findSender(
            chatLine.func_151461_a()
                .getUnformattedText());

        if (sender != null) CHAT_LINE_SENDERS.put(chatLine, sender);

        return sender;
    }

    public static FoundSender findSender(String message) {
        String strippedMessage = stripFormatting(message);

        if (strippedMessage == null || strippedMessage.isEmpty()) return null;

        Map<Character, List<String>> namesByFirstCharacter = getKnownPlayerNamesByFirstCharacter();
        boolean insideWord = false;

        for (int i = 0; i < strippedMessage.length(); i++) {
            char character = strippedMessage.charAt(i);

            if (insideWord && isWordCharacter(character)) continue;

            List<String> names = namesByFirstCharacter.get(Character.toLowerCase(character));

            if (names != null) {
                for (String name : names) {
                    if (matchesNameAt(strippedMessage, i, name)) {
                        GameProfile profile = getGameProfile(name);

                        if (profile != null) return new FoundSender(profile, i);
                    }
                }
            }

            insideWord = isWordCharacter(character);
        }

        return null;
    }

    public static void drawChatHead(GameProfile gameProfile, int x, int y, int alpha) {
        if (gameProfile == null || alpha <= 3) return;

        ResourceLocation texture = PlayerTabGui.getInstance()
            .getPlayerSkin(gameProfile);

        GL11.glPushMatrix();
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_CURRENT_BIT);

        try {
            GL11.glColor4f(1.0F, 1.0F, 1.0F, alpha / 255.0F);
            MINECRAFT.getTextureManager()
                .bindTexture(texture);

            GL11.glEnable(GL11.GL_ALPHA_TEST);
            GL11.glEnable(GL11.GL_BLEND);
            OpenGlHelper.glBlendFunc(770, 771, 1, 0);

            float skinTextureHeight = getBoundSkinTextureHeight();
            Gui.func_152125_a(
                x,
                y,
                SKIN_HEAD_U,
                SKIN_HEAD_V,
                SKIN_HEAD_SIZE,
                SKIN_HEAD_SIZE,
                SKIN_HEAD_SIZE,
                SKIN_HEAD_SIZE,
                SKIN_TEXTURE_WIDTH,
                skinTextureHeight);
            Gui.func_152125_a(
                x,
                y,
                SKIN_HEAD_OVERLAY_U,
                SKIN_HEAD_V,
                SKIN_HEAD_SIZE,
                SKIN_HEAD_SIZE,
                SKIN_HEAD_SIZE,
                SKIN_HEAD_SIZE,
                SKIN_TEXTURE_WIDTH,
                skinTextureHeight);
        } finally {
            GL11.glPopAttrib();
            GL11.glPopMatrix();
        }
    }

    public static int getFormattedIndexForUnformattedIndex(String formattedMessage, int unformattedIndex) {
        int visibleCharacters = 0;

        for (int i = 0; i < formattedMessage.length(); i++) {
            char character = formattedMessage.charAt(i);

            if (character == '\u00a7' && i + 1 < formattedMessage.length()) {
                i++;
                continue;
            }

            if (visibleCharacters == unformattedIndex) return i;

            visibleCharacters++;
        }

        return formattedMessage.length();
    }

    public static String getActiveFormatting(String formattedMessage) {
        String activeColor = "";
        StringBuilder activeFormatting = new StringBuilder();

        for (int i = 0; i + 1 < formattedMessage.length(); i++) {
            if (formattedMessage.charAt(i) != '\u00a7') continue;

            char formatCode = Character.toLowerCase(formattedMessage.charAt(i + 1));

            if (isColorCode(formatCode) || formatCode == 'r') {
                activeColor = formatCode == 'r' ? "" : "\u00a7" + formatCode;
                activeFormatting.setLength(0);
            } else if (isStyleCode(formatCode)) {
                activeFormatting.append('\u00a7')
                    .append(formatCode);
            }

            i++;
        }

        return activeColor + activeFormatting;
    }

    private static String stripFormatting(String message) {
        return EnumChatFormatting.getTextWithoutFormattingCodes(message);
    }

    private static boolean isColorCode(char formatCode) {
        return (formatCode >= '0' && formatCode <= '9') || (formatCode >= 'a' && formatCode <= 'f');
    }

    private static boolean isStyleCode(char formatCode) {
        return formatCode >= 'k' && formatCode <= 'o';
    }

    private static boolean isWordCharacter(char character) {
        return (character >= 'A' && character <= 'Z') || (character >= 'a' && character <= 'z')
            || (character >= '0' && character <= '9')
            || character == '_';
    }

    private static Map<Character, List<String>> getKnownPlayerNamesByFirstCharacter() {
        int worldPlayerCount = MINECRAFT.theWorld == null ? 0 : MINECRAFT.theWorld.playerEntities.size();
        int tabPlayerCount = MINECRAFT.thePlayer == null || MINECRAFT.thePlayer.sendQueue == null ? 0
            : MINECRAFT.thePlayer.sendQueue.playerInfoList.size();
        long now = System.currentTimeMillis();

        if (now < nextKnownPlayerNameRefresh && MINECRAFT.theWorld == cachedWorld
            && worldPlayerCount == cachedWorldPlayerCount
            && tabPlayerCount == cachedTabPlayerCount) {
            return cachedNamesByFirstCharacter;
        }

        Map<Character, List<String>> namesByFirstCharacter = new HashMap<>();

        if (MINECRAFT.theWorld != null) {
            for (Object player : MINECRAFT.theWorld.playerEntities) {
                if (player instanceof EntityPlayer) addKnownName(
                    namesByFirstCharacter,
                    ((EntityPlayer) player).getGameProfile()
                        .getName());
            }
        }

        if (MINECRAFT.thePlayer != null && MINECRAFT.thePlayer.sendQueue != null) {
            for (Object playerInfo : MINECRAFT.thePlayer.sendQueue.playerInfoList) {
                if (playerInfo instanceof GuiPlayerInfo)
                    addKnownName(namesByFirstCharacter, ((GuiPlayerInfo) playerInfo).name);
            }
        }

        cachedWorld = MINECRAFT.theWorld;
        cachedWorldPlayerCount = worldPlayerCount;
        cachedTabPlayerCount = tabPlayerCount;
        cachedNamesByFirstCharacter = namesByFirstCharacter;
        nextKnownPlayerNameRefresh = now + PLAYER_NAME_CACHE_MILLIS;

        return namesByFirstCharacter;
    }

    private static void addKnownName(Map<Character, List<String>> namesByFirstCharacter, String name) {
        if (!isValidPlayerName(name)) return;

        char firstCharacter = Character.toLowerCase(name.charAt(0));
        List<String> names = namesByFirstCharacter.computeIfAbsent(firstCharacter, ignored -> new ArrayList<>());

        if (!names.contains(name)) names.add(name);
    }

    private static boolean matchesNameAt(String message, int startIndex, String name) {
        if (startIndex + name.length() > message.length()) return false;

        if (!message.regionMatches(true, startIndex, name, 0, name.length())) return false;

        char lastCharacter = name.charAt(name.length() - 1);

        return !isWordCharacter(lastCharacter) || startIndex + name.length() >= message.length()
            || !isWordCharacter(message.charAt(startIndex + name.length()));
    }

    private static GameProfile getGameProfile(String name) {
        if (MINECRAFT.theWorld != null) {
            EntityPlayer player = MINECRAFT.theWorld.getPlayerEntityByName(name);

            if (player != null) return player.getGameProfile();
        }

        return new GameProfile((UUID) null, name);
    }

    private static boolean isValidPlayerName(String name) {
        if (name == null || name.isEmpty() || name.length() > 16) return false;

        for (int i = 0; i < name.length(); i++) {
            char character = name.charAt(i);

            if (!((character >= 'A' && character <= 'Z') || (character >= 'a' && character <= 'z')
                || (character >= '0' && character <= '9')
                || character == '_')) {
                return false;
            }
        }

        return true;
    }

    private static float getBoundSkinTextureHeight() {
        try {
            int width = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
            int height = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);

            if (width > 0 && height >= width) return 64.0F;
        } catch (Throwable ignored) {}

        return 32.0F;
    }

    public static class FoundSender {

        private final GameProfile profile;
        private final int unformattedIndex;

        private FoundSender(GameProfile profile, int unformattedIndex) {
            this.profile = profile;
            this.unformattedIndex = unformattedIndex;
        }

        public String getName() {
            return profile.getName();
        }

        public GameProfile getProfile() {
            return profile;
        }

        public int getUnformattedIndex() {
            return unformattedIndex;
        }
    }

}
