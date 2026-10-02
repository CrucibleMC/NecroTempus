package io.github.cruciblemc.necrotempus.utils;

public final class ChatFormattingUtilsTest {

    public static void main(String[] args) {
        assertEquals("\u00a74awdiawdim", ChatFormattingUtils.translateAlternateColorCodes("&4awdiawdim"));
        assertEquals("plain text", ChatFormattingUtils.translateAlternateColorCodes("plain text"));
        assertEquals("&znot-a-code", ChatFormattingUtils.translateAlternateColorCodes("&znot-a-code"));
        assertEquals(
            "\u00a7calready-formatted",
            ChatFormattingUtils.translateAlternateColorCodes("\u00a7calready-formatted"));
        assertEquals("\u00a7lbold \u00a7rreset", ChatFormattingUtils.translateAlternateColorCodes("&lbold &rreset"));
        assertEquals("&4awdiawdim", ChatFormattingUtils.restoreAlternateColorCodes("\u00a74awdiawdim"));
    }

    private static void assertEquals(String expected, String actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError("Expected <" + expected + "> but got <" + actual + ">");
        }
    }

    private ChatFormattingUtilsTest() {}
}
