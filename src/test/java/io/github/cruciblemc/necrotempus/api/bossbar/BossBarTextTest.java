package io.github.cruciblemc.necrotempus.api.bossbar;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.IChatComponent;

public final class BossBarTextTest {

    public static void main(String[] args) {
        ChatComponentText converted = BossBarText.fromDisplayName(new NonTextComponent("Boss Name"));

        assertEquals(ChatComponentText.class, converted.getClass());
        assertEquals("Boss Name", converted.getChatComponentText_TextValue());
    }

    private static void assertEquals(Object expected, Object actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError("Expected <" + expected + "> but got <" + actual + ">");
        }
    }

    private static final class NonTextComponent implements IChatComponent {

        private final String text;

        private NonTextComponent(String text) {
            this.text = text;
        }

        @Override
        public IChatComponent setChatStyle(ChatStyle style) {
            return this;
        }

        @Override
        public ChatStyle getChatStyle() {
            return new ChatStyle();
        }

        @Override
        public IChatComponent appendText(String text) {
            return this;
        }

        @Override
        public IChatComponent appendSibling(IChatComponent component) {
            return this;
        }

        @Override
        public String getUnformattedTextForChat() {
            return text;
        }

        @Override
        public String getUnformattedText() {
            return text;
        }

        @Override
        public String getFormattedText() {
            return text;
        }

        @Override
        public List getSiblings() {
            return Collections.emptyList();
        }

        @Override
        public IChatComponent createCopy() {
            return new NonTextComponent(text);
        }

        @Override
        public Iterator iterator() {
            return Collections.emptyIterator();
        }
    }

    private BossBarTextTest() {}
}
