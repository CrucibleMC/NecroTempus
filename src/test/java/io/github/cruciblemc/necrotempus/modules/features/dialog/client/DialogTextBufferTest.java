package io.github.cruciblemc.necrotempus.modules.features.dialog.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DialogTextBufferTest {

    @Test
    void normalizesPastedLineEndingsAndKeepsTheCaretAfterUnicode() {
        DialogTextBuffer text = new DialogTextBuffer("ab", 32, 3);
        text.setCursorPosition(1, false);

        assertTrue(text.insert("ç\r\n🙂"));
        assertEquals("aç\n🙂b", text.getText());
        assertEquals(5, text.getCursorPosition());
    }

    @Test
    void replacesTheSelectedRangeAndCopiesItWithoutBreakingTheText() {
        DialogTextBuffer text = new DialogTextBuffer("hello\nworld", 32, 3);
        text.setCursorPosition(5, false);
        text.setCursorPosition(11, true);

        assertEquals("\nworld", text.getSelectedText());
        assertTrue(text.insert(" 🙂 "));
        assertEquals("hello 🙂 ", text.getText());
    }

    @Test
    void rejectsPastesThatExceedLengthOrLineLimitsWithoutChangingSelection() {
        DialogTextBuffer text = new DialogTextBuffer("abc", 8, 2);
        text.selectAll();

        assertFalse(text.insert("one\ntwo\nthree"));
        assertEquals("abc", text.getText());
        assertEquals("abc", text.getSelectedText());
        assertFalse(text.insert("012345678"));
        assertEquals("abc", text.getText());
    }

    @Test
    void cursorMovementAndDeletionPreserveSurrogatePairs() {
        DialogTextBuffer text = new DialogTextBuffer("é🙂z", 16, 1);
        text.moveHorizontal(-1, false, false);
        text.moveHorizontal(-1, false, false);
        assertEquals(1, text.getCursorPosition());

        text.deleteBackward();
        assertEquals("🙂z", text.getText());
        text.deleteForward();
        assertEquals("z", text.getText());
    }

    @Test
    void verticalMovementPreservesColumnAndClampsToShorterLine() {
        DialogTextBuffer text = new DialogTextBuffer("abc\nde", 16, 3);
        text.setCursorPosition(3, false);
        text.moveVertical(1, false);
        assertEquals(6, text.getCursorPosition());
    }

    @Test
    void verticalMovementKeepsItsColumnAcrossShortLines() {
        DialogTextBuffer text = new DialogTextBuffer("abcd\nx\nabcd", 16, 3);
        text.setCursorPosition(4, false);

        text.moveVertical(1, false);
        text.moveVertical(1, false);

        assertEquals(11, text.getCursorPosition());
    }
}
