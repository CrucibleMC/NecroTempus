package io.github.cruciblemc.necrotempus.utils;

public final class ChatFormattingUtils {

    private static final char COLOR_CHAR = '\u00a7';
    private static final String VALID_CODES = "0123456789AaBbCcDdEeFfKkLlMmNnOoRr";

    public static String translateAlternateColorCodes(String text) {
        if (text == null || text.indexOf('&') == -1) {
            return text;
        }

        char[] chars = text.toCharArray();

        for (int i = 0; i < chars.length - 1; i++) {
            if (chars[i] == '&' && VALID_CODES.indexOf(chars[i + 1]) != -1) {
                chars[i] = COLOR_CHAR;
                chars[i + 1] = Character.toLowerCase(chars[i + 1]);
            }
        }

        return new String(chars);
    }

    public static String restoreAlternateColorCodes(String text) {
        if (text == null || text.indexOf(COLOR_CHAR) == -1) {
            return text;
        }

        return text.replace(COLOR_CHAR, '&');
    }

    private ChatFormattingUtils() {}
}
