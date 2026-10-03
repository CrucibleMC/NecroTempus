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

import com.mojang.authlib.GameProfile;

import io.github.cruciblemc.necrotempus.NecroTempusConfig;
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
            .findIncomingHeads(decoded, Optional.empty(), null, true, Collections.emptyMap());
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
                    true,
                    Collections.emptyMap())
                .size());
        NecroTempusConfig.ChatHeadsSenderDetection = "UUID_ONLY";
        assertEquals(
            0,
            ChatHeadRenderer.findIncomingHeads(decoded, Optional.empty(), null, true, Collections.emptyMap())
                .size());
    }

    @Test
    void systemMessagesKeepOnlyFirstPlayerAfterServerIdentityWasSeen() {
        ChatHeadRenderer
            .findIncomingHeads(new ChatComponentText("Alice: hi"), Optional.of(alice.getId()), "Alice", true, names());
        List<ChatHead> heads = ChatHeadRenderer
            .findIncomingHeads(new ChatComponentText("Alice killed Bob"), Optional.empty(), null, true, names());
        assertEquals(1, heads.size());
        assertEquals(alice, heads.get(0).profile);

        NecroTempusConfig.ChatHeadsHandleSystemMessages = false;
        assertEquals(
            0,
            ChatHeadRenderer
                .findIncomingHeads(new ChatComponentText("Alice killed Bob"), Optional.empty(), null, true, names())
                .size());
    }

    @Test
    void explicitVictimDoesNotAddASecondHead() {
        List<ChatHead> heads = ChatHeadRenderer.findIncomingHeads(
            new ChatComponentText("Alice killed Bob"),
            Optional.of(bob.getId()),
            "Bob",
            true,
            names());
        assertEquals(1, heads.size());
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
            ChatHeadRenderer.findIncomingHeads(decoded, Optional.empty(), null, true, names())
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
}
