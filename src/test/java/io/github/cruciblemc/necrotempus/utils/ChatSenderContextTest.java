package io.github.cruciblemc.necrotempus.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import net.minecraft.util.ChatComponentText;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.mojang.authlib.GameProfile;

class ChatSenderContextTest {

    @AfterEach
    void clearContext() {
        ChatSenderContext.clear();
    }

    @Test
    void restoresOuterSenderAfterNestedSend() {
        UUID outer = UUID.randomUUID();
        UUID inner = UUID.randomUUID();
        ChatSenderContext.setSender(outer, "outer", "OuterNick");
        ChatSenderContext.Snapshot previous = ChatSenderContext.snapshot();

        ChatSenderContext.setSender(inner, "inner", "InnerNick");
        ChatSenderContext.setHeads(java.util.Collections.emptyList());
        assertEquals(Optional.of(inner), ChatSenderContext.currentSenderUuid());
        assertEquals("InnerNick", ChatSenderContext.currentSenderDisplayName());

        ChatSenderContext.restore(previous);
        assertEquals(Optional.of(outer), ChatSenderContext.currentSenderUuid());
        assertEquals("outer", ChatSenderContext.currentSenderName());
        assertEquals("OuterNick", ChatSenderContext.currentSenderDisplayName());
    }

    @Test
    void restoresDisplayNameAfterAnExceptionInANestedSend() {
        UUID outer = UUID.randomUUID();
        ChatSenderContext.setSender(outer, "outer", "OuterNick");
        assertThrows(IllegalStateException.class, () -> ChatSenderContext.withSender(null, null, () -> {
            assertEquals(null, ChatSenderContext.currentSenderUuid());
            assertEquals(null, ChatSenderContext.currentSenderDisplayName());
            throw new IllegalStateException("send failed");
        }));
        assertEquals(Optional.of(outer), ChatSenderContext.currentSenderUuid());
        assertEquals("OuterNick", ChatSenderContext.currentSenderDisplayName());
    }

    @Test
    void countsTheFirstRenderedLineEvenWhenItIsTheSourceComponent() {
        ChatComponentText source = new ChatComponentText("Nick says hi");
        ChatSenderContext.setSender(UUID.randomUUID(), "Nick");
        ChatSenderContext
            .setHeads(java.util.Collections.singletonList(new ChatHead(new GameProfile(UUID.randomUUID(), "Nick"), 5)));

        assertEquals(
            5,
            ChatSenderContext.getLineHeads(source)
                .get(0).offset);
    }

    @Test
    void mapsEachHeadToItsWrappedLineAndRestoresHistoryPositions() {
        GameProfile alice = new GameProfile(UUID.randomUUID(), "Alice");
        GameProfile bob = new GameProfile(UUID.randomUUID(), "Bob");
        List<ChatHead> heads = Arrays.asList(new ChatHead(alice, 0), new ChatHead(bob, 13));
        ChatSenderContext.setHeads(heads);
        assertEquals(
            alice,
            ChatSenderContext.getLineHeads(new ChatComponentText("Alice killed "))
                .get(0).profile);
        List<ChatHead> second = ChatSenderContext.getLineHeads(new ChatComponentText("Bob"));
        assertEquals(1, second.size());
        assertEquals(0, second.get(0).offset);
        assertEquals(bob, second.get(0).profile);

        ChatSenderContext.setHeads(heads);
        assertEquals(
            2,
            ChatSenderContext.getLineHeads(new ChatComponentText("Alice killed Bob"))
                .size());
    }

    @Test
    void wrappingCountsOnlyHeadsInTheCurrentFragment() {
        GameProfile alice = new GameProfile(UUID.randomUUID(), "Alice");
        GameProfile bob = new GameProfile(UUID.randomUUID(), "Bob");
        ChatSenderContext.setHeads(Arrays.asList(new ChatHead(alice, 0), new ChatHead(bob, 13)));
        assertEquals(2, ChatSenderContext.wrappingHeadCount(16));
        assertEquals(1, ChatSenderContext.wrappingHeadCount(13));
        ChatSenderContext.advanceWrapping(13);
        assertEquals(1, ChatSenderContext.wrappingHeadCount(3));
        ChatSenderContext.advanceWrapping(3);
        assertEquals(0, ChatSenderContext.wrappingHeadCount(10));
    }
}
