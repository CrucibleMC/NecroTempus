package io.github.cruciblemc.necrotempus.modules.features.chatheads.client.render;

import java.util.*;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.GuiPlayerInfo;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.event.ClickEvent;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import com.mojang.authlib.GameProfile;

import io.github.cruciblemc.necrotempus.NecroTempusConfig;
import io.github.cruciblemc.necrotempus.api.playertab.TabCell;
import io.github.cruciblemc.necrotempus.modules.features.playertab.client.DefaultPlayerTab;
import io.github.cruciblemc.necrotempus.modules.features.playertab.client.PlayerSkinTextures;
import io.github.cruciblemc.necrotempus.modules.features.playertab.client.render.PlayerTabGui;
import io.github.cruciblemc.necrotempus.utils.ChatFormattingUtils;
import io.github.cruciblemc.necrotempus.utils.ChatHead;
import io.github.cruciblemc.necrotempus.utils.NetHandlerPlayClientNT;

public class ChatHeadRenderer {

    public static final int CHAT_HEAD_WIDTH = 9;

    private static final Map<ChatLine, SenderMetadata> CHAT_LINE_SENDERS = Collections
        .synchronizedMap(new WeakHashMap<>());
    private static final Map<String, String> DETECTED_ALIASES = new HashMap<>();
    private static boolean serverSentSenderUuid;

    private ChatHeadRenderer() {}

    public static void rememberHeads(ChatLine line, List<ChatHead> messageHeads, List<ChatHead> lineHeads) {
        CHAT_LINE_SENDERS.put(line, new SenderMetadata(messageHeads, lineHeads));
    }

    public static List<ChatHead> getMessageHeads(ChatLine line) {
        SenderMetadata metadata = CHAT_LINE_SENDERS.get(line);
        return metadata == null ? Collections.emptyList() : metadata.messageHeads;
    }

    public static List<ChatHead> getLineHeads(ChatLine line) {
        SenderMetadata metadata = CHAT_LINE_SENDERS.get(line);
        return metadata == null ? Collections.emptyList() : metadata.lineHeads;
    }

    public static List<ChatHead> findIncomingHeads(IChatComponent component, Optional<UUID> packetSenderUuid,
        String packetSenderName, String displayName, boolean chatMessage) {
        return findIncomingHeads(
            component,
            packetSenderUuid,
            packetSenderName,
            displayName,
            chatMessage,
            collectPlayerNames());
    }

    static List<ChatHead> findIncomingHeads(IChatComponent component, Optional<UUID> packetSenderUuid,
        String packetSenderName, String displayName, boolean chatMessage, Map<String, GameProfile> playerNames) {
        if (component == null) return Collections.emptyList();
        String mode = NecroTempusConfig.ChatHeadsSenderDetection == null ? "UUID_AND_HEURISTIC"
            : NecroTempusConfig.ChatHeadsSenderDetection.trim()
                .toUpperCase(Locale.ROOT);
        boolean heuristicOnly = "HEURISTIC_ONLY".equals(mode);
        boolean clickOnly = "CLICK_EVENTS".equals(mode);
        boolean uuidOnly = "UUID_ONLY".equals(mode) || "SERVER_ONLY".equals(mode);
        boolean explicitSender = packetSenderUuid != null && packetSenderUuid.isPresent()
            && !heuristicOnly
            && !clickOnly;
        String text = stripFormatting(component.getUnformattedText());
        Map<String, GameProfile> names = new HashMap<>(playerNames);

        String associatedName = cleanName(packetSenderName);
        if (explicitSender) {
            serverSentSenderUuid = true;
            UUID uuid = packetSenderUuid.get();
            GameProfile associated = names.values()
                .stream()
                .filter(profile -> uuid.equals(profile.getId()))
                .findFirst()
                .orElse(new GameProfile(uuid, associatedName.isEmpty() ? null : associatedName));
            String positionName = displayName == null ? associatedName : cleanName(displayName);
            if (displayName == null && positionName.isEmpty()) positionName = cleanName(associated.getName());
            int offset = ChatFormattingUtils.findUniqueNameInVisibleText(text, positionName);
            return Collections.singletonList(new ChatHead(associated, Math.max(0, offset)));
        }
        if (uuidOnly) return Collections.emptyList();
        if (!NecroTempusConfig.ChatHeadsHandleSystemMessages
            && (!chatMessage || !heuristicOnly && NecroTempusConfig.ChatHeadsSmartHeuristics && serverSentSenderUuid)) {
            return Collections.emptyList();
        }

        if (NecroTempusConfig.ChatHeadsDetectNameAliases) learnRealNameAlias(component, names);
        ChatHead clicked = findClickableSender(component, text, names, 0);
        if (clickOnly) return clicked == null ? Collections.emptyList() : Collections.singletonList(clicked);
        if (NecroTempusConfig.ChatHeadsHandleSystemMessages && component instanceof ChatComponentTranslation) {
            ChatComponentTranslation translation = (ChatComponentTranslation) component;
            String key = translation.getKey();
            Object[] args = translation.getFormatArgs();
            if (("multiplayer.player.joined".equals(key) || "multiplayer.player.joined.renamed".equals(key))
                && args.length > 0) {
                String name = args[0] instanceof IChatComponent
                    ? cleanName(((IChatComponent) args[0]).getUnformattedText())
                    : args[0] instanceof String ? cleanName((String) args[0]) : "";
                if (name.matches("[A-Za-z0-9_]{1,16}")) {
                    names.putIfAbsent(normalizeName(name), new GameProfile(null, name));
                }
            }
        }
        List<ChatHead> heads = findNamedPlayersInVisibleText(text, names);
        if (clicked != null && heads.stream()
            .noneMatch(head -> head.offset == clicked.offset)) heads.add(clicked);
        heads.sort(Comparator.comparingInt(head -> head.offset));
        return heads.isEmpty() ? Collections.emptyList() : Collections.singletonList(heads.get(0));
    }

    static List<ChatHead> findNamedPlayers(String text, Map<String, GameProfile> names) {
        return findNamedPlayersInVisibleText(stripFormatting(text), names);
    }

    private static List<ChatHead> findNamedPlayersInVisibleText(String text, Map<String, GameProfile> names) {
        List<ChatHead> heads = new ArrayList<>();
        int cursor = 0;
        while (cursor < text.length()) {
            String matchedName = null;
            GameProfile matchedProfile = null;
            for (Map.Entry<String, GameProfile> entry : names.entrySet()) {
                if (!ChatFormattingUtils.matchesVisibleNameAt(text, entry.getKey(), cursor)) continue;
                if (matchedName == null || entry.getKey()
                    .length() > matchedName.length()) {
                    matchedName = entry.getKey();
                    matchedProfile = entry.getValue();
                }
            }
            if (matchedName == null) cursor++;
            else {
                heads.add(new ChatHead(matchedProfile, cursor));
                break;
            }
        }
        return heads;
    }

    public static void resetSenderDetection() {
        serverSentSenderUuid = false;
        DETECTED_ALIASES.clear();
    }

    private static Map<String, GameProfile> collectPlayerNames() {
        Map<String, GameProfile> names = new HashMap<>();
        Set<String> ambiguous = new HashSet<>();
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.thePlayer == null || minecraft.theWorld == null) return names;
        for (GuiPlayerInfo player : NetHandlerPlayClientNT.of(minecraft.thePlayer.sendQueue)
            .getServerPlayers()) {
            if (cleanName(player.name).isEmpty()) continue;
            EntityPlayer entity = minecraft.theWorld.getPlayerEntityByName(player.name);
            GameProfile profile = entity == null ? new GameProfile(null, player.name) : entity.getGameProfile();
            addName(names, ambiguous, profile, player.name);
        }
        for (TabCell cell : DefaultPlayerTab.getInstance()
            .getCellList()) {
            GameProfile profile = cell.getSkullProfile();
            if (profile == null && cell.getLinkedUserName() != null)
                profile = new GameProfile(null, cell.getLinkedUserName());
            if (profile == null) continue;
            addName(names, ambiguous, profile, profile.getName());
            addName(names, ambiguous, profile, cell.getLinkedUserName());
            if (cell.getDisplayName() != null) addName(
                names,
                ambiguous,
                profile,
                cell.getDisplayName()
                    .getUnformattedText());
        }
        for (Object player : minecraft.theWorld.playerEntities) {
            if (!(player instanceof EntityPlayer)) continue;
            EntityPlayer entityPlayer = (EntityPlayer) player;
            GameProfile profile = entityPlayer.getGameProfile();
            addName(names, ambiguous, profile, profile.getName());
            addName(
                names,
                ambiguous,
                profile,
                entityPlayer.func_145748_c_()
                    .getUnformattedText());
        }
        if (NecroTempusConfig.ChatHeadsDetectNameAliases) {
            Map<String, String> aliases = configuredAliases();
            for (Map.Entry<String, String> alias : aliases.entrySet()) {
                GameProfile profile = names.get(normalizeName(alias.getValue()));
                if (profile != null) addName(names, ambiguous, profile, alias.getKey());
            }
            for (Map.Entry<String, String> alias : DETECTED_ALIASES.entrySet()) {
                GameProfile profile = names.get(
                    alias.getValue()
                        .toLowerCase(Locale.ROOT));
                if (profile != null) addVisibleName(names, ambiguous, profile, alias.getKey());
            }
        }
        for (String name : ambiguous) names.remove(name);
        return names;
    }

    private static Map<String, String> configuredAliases() {
        Map<String, String> aliases = new HashMap<>();
        String value = NecroTempusConfig.ChatHeadsNameAliases;
        if (value == null) return aliases;
        for (String entry : value.split(";")) {
            int separator = entry.indexOf('=');
            if (separator <= 0 || separator == entry.length() - 1) continue;
            aliases.put(
                entry.substring(0, separator)
                    .trim(),
                entry.substring(separator + 1)
                    .trim());
        }
        return aliases;
    }

    private static void addName(Map<String, GameProfile> names, Set<String> ambiguous, GameProfile profile,
        String name) {
        addVisibleName(names, ambiguous, profile, cleanName(name));
    }

    private static void addVisibleName(Map<String, GameProfile> names, Set<String> ambiguous, GameProfile profile,
        String cleaned) {
        if (profile == null || cleaned.isEmpty()) return;
        String key = cleaned.toLowerCase(Locale.ROOT);
        GameProfile existing = names.get(key);
        if (existing != null && !sameProfile(existing, profile)) {
            ambiguous.add(key);
            names.remove(key);
        } else if (!ambiguous.contains(key)) {
            if (existing == null || existing.getId() == null && profile.getId() != null
                || existing.getProperties()
                    .isEmpty()
                    && !profile.getProperties()
                        .isEmpty())
                names.put(key, profile);
        }
    }

    private static boolean sameProfile(GameProfile first, GameProfile second) {
        if (first.getId() != null && second.getId() != null) return first.getId()
            .equals(second.getId());
        return first.getName() != null && first.getName()
            .equalsIgnoreCase(second.getName());
    }

    private static ChatHead findClickableSender(IChatComponent component, String text, Map<String, GameProfile> names,
        int depth) {
        if (component == null || depth > 64) return null;
        ClickEvent click = component.getChatStyle()
            .getChatClickEvent();
        if (click != null && (click.getAction() == ClickEvent.Action.SUGGEST_COMMAND
            || click.getAction() == ClickEvent.Action.RUN_COMMAND)) {
            String visible = cleanName(component.getUnformattedText());
            GameProfile profile = names.get(visible.toLowerCase(Locale.ROOT));
            int offset = ChatFormattingUtils.findNameInVisibleText(text, visible);
            if (profile != null && offset >= 0) return new ChatHead(profile, offset);
        }
        if (component instanceof ChatComponentTranslation) {
            for (Object argument : ((ChatComponentTranslation) component).getFormatArgs()) {
                if (argument instanceof IChatComponent) {
                    ChatHead head = findClickableSender((IChatComponent) argument, text, names, depth + 1);
                    if (head != null) return head;
                }
            }
        }
        for (IChatComponent sibling : component.getSiblings()) {
            ChatHead head = findClickableSender(sibling, text, names, depth + 1);
            if (head != null) return head;
        }
        return null;
    }

    private static String cleanName(String name) {
        return name == null ? "" : stripFormatting(name).trim();
    }

    private static String stripFormatting(String text) {
        return ChatFormattingUtils.stripFormatting(text);
    }

    private static String normalizeName(String name) {
        return cleanName(name).toLowerCase(Locale.ROOT);
    }

    private static void learnRealNameAlias(IChatComponent component, Map<String, GameProfile> candidates) {
        if (!NecroTempusConfig.ChatHeadsDetectNameAliases) return;
        String message = stripFormatting(component.getUnformattedText());
        int firstSpace = message.indexOf(' ');
        if (firstSpace <= 0 || !message.startsWith(" is ", firstSpace) || message.indexOf(' ', firstSpace + 4) >= 0)
            return;
        String nickname = message.substring(0, firstSpace);
        String profileName = message.substring(firstSpace + 4);
        if (candidates.containsKey(profileName.toLowerCase(Locale.ROOT))) DETECTED_ALIASES.put(nickname, profileName);
    }

    public static int getFormattedIndexForVisibleIndex(String text, int visibleIndex) {
        return ChatFormattingUtils.getFormattedIndexForVisibleIndex(text, visibleIndex);
    }

    public static String getActiveFormatting(String text, int endIndex) {
        return ChatFormattingUtils.getActiveFormatting(text, endIndex);
    }

    public static void drawChatHead(GameProfile gameProfile, int x, int y, int alpha) {
        if (gameProfile == null || alpha <= 3) return;

        Minecraft minecraft = Minecraft.getMinecraft();
        ResourceLocation texture = PlayerTabGui.getInstance()
            .getPlayerSkin(gameProfile);

        GL11.glPushMatrix();
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_CURRENT_BIT);

        try {
            GL11.glColor4f(1.0F, 1.0F, 1.0F, alpha / 255.0F);
            Minecraft.getMinecraft()
                .getTextureManager()
                .bindTexture(texture);

            GL11.glEnable(GL11.GL_ALPHA_TEST);
            GL11.glEnable(GL11.GL_BLEND);
            OpenGlHelper.glBlendFunc(770, 771, 1, 0);

            GL11.glColor4f(0.25F, 0.25F, 0.25F, alpha / 255.0F);
            PlayerSkinTextures.drawBoundHead(x + 1, y);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, alpha / 255.0F);
            PlayerSkinTextures.drawBoundHead(x, y - 1);
        } finally {
            GL11.glPopAttrib();
            GL11.glPopMatrix();
        }
    }

    private static final class SenderMetadata {

        private final List<ChatHead> messageHeads;
        private final List<ChatHead> lineHeads;

        private SenderMetadata(List<ChatHead> messageHeads, List<ChatHead> lineHeads) {
            this.messageHeads = messageHeads;
            this.lineHeads = lineHeads;
        }
    }
}
