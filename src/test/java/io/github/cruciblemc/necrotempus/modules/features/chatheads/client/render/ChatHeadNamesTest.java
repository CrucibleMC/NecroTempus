package io.github.cruciblemc.necrotempus.modules.features.chatheads.client.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import net.minecraft.event.ClickEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.gtnewhorizons.angelica.config.AngelicaConfig;
import com.mojang.authlib.GameProfile;

import io.github.cruciblemc.necrotempus.NecroTempusConfig;
import io.github.cruciblemc.necrotempus.utils.ChatFormattingUtils;
import io.github.cruciblemc.necrotempus.utils.ChatHead;

class ChatHeadNamesTest {

    private final GameProfile alice = new GameProfile(UUID.randomUUID(), "Alice");
    private final GameProfile bob = new GameProfile(UUID.randomUUID(), "Bob");
    private final String previousMode = NecroTempusConfig.ChatHeadsSenderDetection;
    private final boolean previousSystem = NecroTempusConfig.ChatHeadsHandleSystemMessages;
    private final boolean previousSmart = NecroTempusConfig.ChatHeadsSmartHeuristics;

    @BeforeEach
    void enableSystemDetection() {
        NecroTempusConfig.ChatHeadsSenderDetection = "UUID_AND_HEURISTIC";
        NecroTempusConfig.ChatHeadsHandleSystemMessages = true;
        NecroTempusConfig.ChatHeadsSmartHeuristics = true;
        ChatHeadRenderer.resetSenderDetection();
    }

    @AfterEach
    void restoreSettings() {
        NecroTempusConfig.ChatHeadsSenderDetection = previousMode;
        NecroTempusConfig.ChatHeadsHandleSystemMessages = previousSystem;
        NecroTempusConfig.ChatHeadsSmartHeuristics = previousSmart;
        ChatHeadRenderer.resetSenderDetection();
    }

    @Test
    void vanillaJoinUsesDeclaredNameBeforePlayerListArrivesAfterJson() {
        IChatComponent join = new ChatComponentTranslation("multiplayer.player.joined", new ChatComponentText("Bob"));
        IChatComponent decoded = IChatComponent.Serializer.func_150699_a(IChatComponent.Serializer.func_150696_a(join));
        List<ChatHead> heads = ChatHeadRenderer
            .findIncomingHeads(decoded, Optional.empty(), null, null, true, Collections.emptyMap());
        assertEquals(1, heads.size());
        assertEquals("Bob", heads.get(0).profile.getName());
        assertEquals(null, heads.get(0).profile.getId());
        assertEquals(0, heads.get(0).offset);
        assertEquals(
            0,
            ChatHeadRenderer
                .findIncomingHeads(
                    new ChatComponentText("Bob joined the game"),
                    Optional.empty(),
                    null,
                    null,
                    true,
                    Collections.emptyMap())
                .size());
        NecroTempusConfig.ChatHeadsSenderDetection = "UUID_ONLY";
        assertEquals(
            0,
            ChatHeadRenderer.findIncomingHeads(decoded, Optional.empty(), null, null, true, Collections.emptyMap())
                .size());
    }

    @Test
    void systemMessagesKeepOnlyFirstPlayerAfterServerIdentityWasSeen() {
        ChatHeadRenderer.findIncomingHeads(
            new ChatComponentText("Alice: hi"),
            Optional.of(alice.getId()),
            "Alice",
            null,
            true,
            names());
        List<ChatHead> heads = ChatHeadRenderer
            .findIncomingHeads(new ChatComponentText("Alice killed Bob"), Optional.empty(), null, null, true, names());
        assertEquals(1, heads.size());
        assertEquals(alice, heads.get(0).profile);

        NecroTempusConfig.ChatHeadsHandleSystemMessages = false;
        assertEquals(
            0,
            ChatHeadRenderer
                .findIncomingHeads(
                    new ChatComponentText("Alice killed Bob"),
                    Optional.empty(),
                    null,
                    null,
                    true,
                    names())
                .size());
    }

    @Test
    void explicitServerIdentityWinsOverAnotherPlayerMention() {
        List<ChatHead> heads = ChatHeadRenderer.findIncomingHeads(
            new ChatComponentText("Alice killed Bob"),
            Optional.of(bob.getId()),
            "Bob",
            null,
            true,
            names());
        assertEquals(1, heads.size());
        assertEquals(bob, heads.get(0).profile);
        assertEquals(13, heads.get(0).offset);
    }

    @Test
    void ambiguousServerNameFallsBackToTheBeginning() {
        List<ChatHead> heads = ChatHeadRenderer.findIncomingHeads(
            new ChatComponentText("Notice: Bob mentioned Bob"),
            Optional.of(bob.getId()),
            "Bob",
            null,
            true,
            names());
        assertEquals(bob, heads.get(0).profile);
        assertEquals(0, heads.get(0).offset);
    }

    @Test
    void serverDisplayNamePositionsTheUuidHeadWithoutChangingTheComponent() {
        IChatComponent nickname = new ChatComponentText("§aSuperBruno");
        nickname.getChatStyle()
            .setChatClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/msg Bob "));
        IChatComponent component = new ChatComponentText("[Global] ").appendSibling(nickname)
            .appendSibling(new ChatComponentText(": hello Alice"));
        String before = IChatComponent.Serializer.func_150696_a(component);
        List<ChatHead> heads = ChatHeadRenderer
            .findIncomingHeads(component, Optional.of(bob.getId()), "Bob", "§6SuperBruno", true, names());
        assertEquals(1, heads.size());
        assertEquals(bob, heads.get(0).profile);
        assertEquals(9, heads.get(0).offset);
        assertEquals(before, IChatComponent.Serializer.func_150696_a(component));
    }

    @Test
    void missingOrRepeatedDisplayNameKeepsTheUuidHeadAtTheBeginning() {
        for (String text : new String[] { "Alice: hello", "[Global] SuperBruno quoted SuperBruno",
            "SuperBruno123: hello Alice" }) {
            List<ChatHead> heads = ChatHeadRenderer.findIncomingHeads(
                new ChatComponentText(text),
                Optional.of(bob.getId()),
                "Bob",
                "SuperBruno",
                true,
                names());
            assertEquals(bob, heads.get(0).profile);
            assertEquals(0, heads.get(0).offset);
        }
    }

    @Test
    void emptyDisplayNameDoesNotUseARealNameMentionAsThePosition() {
        List<ChatHead> heads = ChatHeadRenderer.findIncomingHeads(
            new ChatComponentText("Notice: Bob is mentioned"),
            Optional.of(bob.getId()),
            "Bob",
            "",
            true,
            names());
        assertEquals(bob, heads.get(0).profile);
        assertEquals(0, heads.get(0).offset);
    }

    @Test
    void absentClientProfileKeepsTheRealNameSeparateFromTheNickname() {
        List<ChatHead> heads = ChatHeadRenderer.findIncomingHeads(
            new ChatComponentText("[Global] SuperBruno: hello"),
            Optional.of(bob.getId()),
            "Bob",
            "SuperBruno",
            true,
            Collections.emptyMap());
        assertEquals(bob.getId(), heads.get(0).profile.getId());
        assertEquals("Bob", heads.get(0).profile.getName());
        assertEquals(9, heads.get(0).offset);
    }

    @Test
    void uuidOnlyUsesTheDisplayNameButHeuristicOnlyIgnoresServerMetadata() {
        NecroTempusConfig.ChatHeadsSenderDetection = "UUID_ONLY";
        List<ChatHead> heads = ChatHeadRenderer.findIncomingHeads(
            new ChatComponentText("[Global] SuperBruno: hello Alice"),
            Optional.of(bob.getId()),
            "Bob",
            "SuperBruno",
            true,
            names());
        assertEquals(bob, heads.get(0).profile);
        assertEquals(9, heads.get(0).offset);
        NecroTempusConfig.ChatHeadsSenderDetection = "HEURISTIC_ONLY";
        heads = ChatHeadRenderer.findIncomingHeads(
            new ChatComponentText("[Global] SuperBruno: hello Alice"),
            Optional.of(bob.getId()),
            "Bob",
            "SuperBruno",
            true,
            names());
        assertEquals(alice, heads.get(0).profile);
    }

    @Test
    void scansNestedNamesAfterJsonWithoutChangingClickEvents() {
        IChatComponent name = new ChatComponentText("Bob");
        ClickEvent click = new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/tell Bob ");
        name.getChatStyle()
            .setChatClickEvent(click);
        IChatComponent message = new ChatComponentText("").appendSibling(new ChatComponentText("Alice killed "))
            .appendSibling(name);
        IChatComponent decoded = IChatComponent.Serializer
            .func_150699_a(IChatComponent.Serializer.func_150696_a(message));
        String before = IChatComponent.Serializer.func_150696_a(decoded);
        assertEquals(
            1,
            ChatHeadRenderer.findIncomingHeads(decoded, Optional.empty(), null, null, true, names())
                .size());
        assertEquals(before, IChatComponent.Serializer.func_150696_a(decoded));
        assertEquals(
            click,
            decoded.getSiblings()
                .get(1)
                .getChatStyle()
                .getChatClickEvent());
    }

    @Test
    void findsFirstPlayerInDeathAndTeleportMessages() {
        Map<String, GameProfile> names = names();
        List<ChatHead> death = ChatHeadRenderer.findNamedPlayers("Alice killed Bob", names);
        assertEquals(1, death.size());
        assertEquals(alice, death.get(0).profile);
        assertEquals(0, death.get(0).offset);

        List<ChatHead> teleport = ChatHeadRenderer.findNamedPlayers("Bob teleported to Alice", names);
        assertEquals(1, teleport.size());
        assertEquals(bob, teleport.get(0).profile);
    }

    @Test
    void keepsOnlyFirstMentionWithoutMatchingPartialNames() {
        Map<String, GameProfile> names = names();
        names.put("Bobby", bob);
        List<ChatHead> heads = ChatHeadRenderer.findNamedPlayers("§aAlice: Bobby and Alice, not Bobcat", names);
        assertEquals(1, heads.size());
        assertEquals(0, heads.get(0).offset);
        assertEquals(
            0,
            ChatHeadRenderer.findNamedPlayers("Teleported successfully", names)
                .size());
    }

    @Test
    void prefersTheCompleteDisplayNameWhenNamesOverlap() {
        Map<String, GameProfile> names = names();
        names.put("Alice Smith", bob);
        List<ChatHead> heads = ChatHeadRenderer.findNamedPlayers("Alice Smith killed Alice", names);
        assertEquals(1, heads.size());
        assertEquals(bob, heads.get(0).profile);
    }

    @Test
    void uuidOnlyKeepsTheExplicitAssociationWithoutGuessingOtherPlayers() {
        NecroTempusConfig.ChatHeadsSenderDetection = "UUID_ONLY";
        List<ChatHead> heads = ChatHeadRenderer.findIncomingHeads(
            new ChatComponentText("Alice killed Bob"),
            Optional.of(bob.getId()),
            "Bob",
            null,
            true,
            names());
        assertEquals(1, heads.size());
        assertEquals(bob, heads.get(0).profile);
        assertEquals(13, heads.get(0).offset);
    }

    private Map<String, GameProfile> names() {
        Map<String, GameProfile> names = new HashMap<>();
        names.put("Alice", alice);
        names.put("Bob", bob);
        return names;
    }

    @Test
    void angelicaEffectsKeepNicknameIdentityPositionAndComponentEvents() {
        boolean originalRenderer = AngelicaConfig.enableFontRenderer;
        boolean originalConversion = AngelicaConfig.enableAmpersandConversion;
        AngelicaConfig.enableFontRenderer = true;
        AngelicaConfig.enableAmpersandConversion = true;
        ChatFormattingUtils.setAngelicaPresent(true);
        try {
            for (String effect : new String[] { "&q&z", "&#FF0000", "&g&#FF0000&#0000FF", "&u&#FF0000" }) {
                IChatComponent nickname = new ChatComponentText(effect + "SuperBruno");
                nickname.getChatStyle()
                    .setChatClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/msg Bob "));
                IChatComponent component = new ChatComponentText("[VIP] ").appendSibling(nickname)
                    .appendText(": hello Alice");
                String before = IChatComponent.Serializer.func_150696_a(component);
                List<ChatHead> heads = ChatHeadRenderer.findIncomingHeads(
                    component,
                    Optional.of(bob.getId()),
                    "Bob",
                    effect + "SuperBruno",
                    true,
                    names());
                assertEquals(1, heads.size());
                assertEquals(bob, heads.get(0).profile);
                assertEquals(6, heads.get(0).offset);
                assertEquals(before, IChatComponent.Serializer.func_150696_a(component));
                List<ChatHead> heuristic = ChatHeadRenderer.findIncomingHeads(
                    new ChatComponentText("[VIP] " + effect + "Bob: hello"),
                    Optional.empty(),
                    null,
                    null,
                    true,
                    names());
                assertEquals(
                    1,
                    heuristic.size(),
                    effect + " => " + ChatFormattingUtils.stripFormatting("[VIP] " + effect + "Bob: hello"));
                assertEquals(bob, heuristic.get(0).profile);
                assertEquals(6, heuristic.get(0).offset);
            }
            for (String escaped : new String[] { "\\&q", "\\&a" }) {
                ChatComponentText component = new ChatComponentText(escaped + "[VIP] Bob: hello");
                for (Optional<UUID> sender : java.util.Arrays
                    .asList(Optional.of(bob.getId()), Optional.<UUID>empty())) {
                    List<ChatHead> heads = ChatHeadRenderer
                        .findIncomingHeads(component, sender, "Bob", "Bob", true, names());
                    assertEquals(8, heads.get(0).offset);
                }
            }
        } finally {
            ChatFormattingUtils.setAngelicaPresent(false);
            AngelicaConfig.enableFontRenderer = originalRenderer;
            AngelicaConfig.enableAmpersandConversion = originalConversion;
        }
    }
}
