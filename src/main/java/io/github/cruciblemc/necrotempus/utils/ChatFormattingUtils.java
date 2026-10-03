package io.github.cruciblemc.necrotempus.utils;

public final class ChatFormattingUtils {

    private static final char COLOR_CHAR = '§';

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

    public static int visibleCharacterCount(String text) {
        if (text == null || text.isEmpty()) return 0;

        int visibleCharacters = 0;

        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);

            if ((character == COLOR_CHAR || character == '&') && i + 1 < text.length()
                && VALID_CODES.indexOf(text.charAt(i + 1)) >= 0) {
                i++;
            } else {
                visibleCharacters++;
            }
        }

        return visibleCharacters;
    }

    public static int findNameInMessage(String text, String name) {
        String visibleText = stripFormatting(text);
        String visibleName = stripFormatting(name).trim();
        if (visibleText.isEmpty() || visibleName.isEmpty()) return -1;

        for (int start = 0; start + visibleName.length() <= visibleText.length(); start++) {
            if (matchesVisibleNameAt(visibleText, visibleName, start)) return start;
        }
        return -1;
    }

    public static boolean matchesVisibleNameAt(String text, String name, int start) {
        if (text == null || name == null || name.isEmpty() || start < 0 || start + name.length() > text.length())
            return false;
        if (start > 0 && isNameCharacter(text.charAt(start - 1))) return false;
        if (!text.regionMatches(true, start, name, 0, name.length())) return false;
        int end = start + name.length();
        return end == text.length() || !isNameCharacter(text.charAt(end));
    }

    private static String stripFormatting(String text) {
        if (text == null || text.isEmpty()) return "";
        StringBuilder visible = new StringBuilder(text.length());

        for (int i = 0; i < text.length(); i++) {
            char current = text.charAt(i);
            if ((current == COLOR_CHAR || current == '&') && i + 1 < text.length()
                && VALID_CODES.indexOf(text.charAt(i + 1)) >= 0) {
                i++;
            } else {
                visible.append(current);
            }
        }
        return visible.toString();
    }

    private static boolean isNameCharacter(char character) {
        return Character.isLetterOrDigit(character) || character == '_';
    }

    private ChatFormattingUtils() {}
}
