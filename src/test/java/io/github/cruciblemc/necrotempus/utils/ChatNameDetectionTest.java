package io.github.cruciblemc.necrotempus.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ChatNameDetectionTest {

    @Test
    void findsSenderAfterFormattingAndInCustomMessageText() {
        assertEquals(6, ChatFormattingUtils.findNameInMessage("[VIP] §eNick: hello", "Nick"));
        assertEquals(14, ChatFormattingUtils.findNameInMessage("Server event: Nick entered", "Nick"));
    }

    @Test
    void matchesWholeNamesButNotPartialNames() {
        assertEquals(6, ChatFormattingUtils.findNameInMessage("Hello Nick", "Nick"));
        assertEquals(-1, ChatFormattingUtils.findNameInMessage("Nickname: hello", "Nick"));
    }

    @Test
    void visibleCharacterCountSkipsMinecraftAndAlternateColorCodes() {
        assertEquals(12, ChatFormattingUtils.visibleCharacterCount("\u00a7e[Admin] &aNick"));
    }
}
