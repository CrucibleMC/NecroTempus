package io.github.cruciblemc.necrotempus.utils;

import com.gtnewhorizons.angelica.client.font.ColorCodeUtils;
import com.gtnewhorizons.angelica.config.AngelicaConfig;

public final class ChatFormattingUtils {

    private static final char COLOR_CHAR = '§';

    private static final String VALID_CODES = "0123456789AaBbCcDdEeFfKkLlMmNnOoRr";

    private static boolean angelicaPresent;

    public static void setAngelicaPresent(boolean present) {
        angelicaPresent = present;
    }

    public static boolean isAngelicaFormattingEnabled() {
        return angelicaPresent && AngelicaConfig.enableFontRenderer;
    }

    public static String translateAlternateColorCodes(String text) {

        if (isAngelicaFormattingEnabled()) text = ColorCodeUtils.convertAmpersandToSectionX(text);

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
        return stripFormatting(text).length();
    }

    public static int findNameInMessage(String text, String name) {
        return findNameInVisibleText(stripFormatting(text), stripFormatting(name).trim());
    }

    public static int findNameInVisibleText(String visibleText, String visibleName) {
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

    public static int findUniqueNameInMessage(String text, String name) {
        return findUniqueNameInVisibleText(stripFormatting(text), stripFormatting(name).trim());
    }

    public static int findUniqueNameInVisibleText(String visibleText, String visibleName) {
        if (visibleName.isEmpty()) return -1;
        int found = -1;
        for (int start = 0; start + visibleName.length() <= visibleText.length(); start++) {
            if (!matchesVisibleNameAt(visibleText, visibleName, start)) continue;
            if (found >= 0) return -1;
            found = start;
        }
        return found;
    }

    public static String stripFormatting(String text) {
        if (text == null || text.isEmpty()) return "";
        text = translateAlternateColorCodes(text);
        StringBuilder visible = new StringBuilder(text.length());

        for (int i = 0; i < text.length();) {
            int length = formattingCodeLength(text, i);
            if (length > 0) {
                i += length;
            } else {
                char current = text.charAt(i++);
                visible.append(
                    isAngelicaFormattingEnabled() && current == ColorCodeUtils.ESCAPED_AMPERSAND ? '&' : current);
            }
        }
        return visible.toString();
    }

    public static int formattingCodeLength(String text, int index) {
        if (index + 1 >= text.length()) return 0;
        char marker = text.charAt(index);
        if (marker != COLOR_CHAR && marker != '&') return 0;
        char code = Character.toLowerCase(text.charAt(index + 1));
        if (marker == COLOR_CHAR && isAngelicaFormattingEnabled()) {
            if (code == 'g' && ColorCodeUtils.isValidSectionX(text, index + 2)
                && ColorCodeUtils.isValidSectionX(text, index + 2 + ColorCodeUtils.SECTION_X_LENGTH))
                return ColorCodeUtils.GRADIENT_LENGTH;
            if (code == 'u' && ColorCodeUtils.isValidSectionX(text, index + 2))
                return 2 + ColorCodeUtils.SECTION_X_LENGTH;
            if (code == 'x' && ColorCodeUtils.isValidSectionX(text, index)) return ColorCodeUtils.SECTION_X_LENGTH;
            return 2;
        }
        return VALID_CODES.indexOf(code) >= 0 ? 2 : 0;
    }

    public static int getFormattedIndexForVisibleIndex(String text, int visibleIndex) {
        if (text == null || visibleIndex < 0) return 0;
        int visible = 0;
        for (int i = 0; i < text.length();) {
            int length = formattingCodeLength(text, i);
            if (length > 0) i += length;
            else {
                if (visible++ == visibleIndex) return i;
                i++;
            }
        }
        return text.length();
    }

    public static int safeFormattingBoundary(String text, int endIndex) {
        int end = Math.max(0, Math.min(endIndex, text.length()));
        for (int i = 0; i < end;) {
            int length = Math.max(1, formattingCodeLength(text, i));
            if (i + length > end) return i;
            i += length;
        }
        return end;
    }

    public static String getActiveFormatting(String text, int endIndex) {
        StringBuilder formatting = new StringBuilder();
        int end = safeFormattingBoundary(text, endIndex);
        for (int i = 0; i < end;) {
            int length = formattingCodeLength(text, i);
            if (length > 0) formatting.append(text, i, i + length);
            i += Math.max(1, length);
        }
        return formatting.toString();
    }

    private static boolean isNameCharacter(char character) {
        return Character.isLetterOrDigit(character) || character == '_';
    }

    private ChatFormattingUtils() {}
}
