package io.github.cruciblemc.necrotempus.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.gtnewhorizons.angelica.config.AngelicaConfig;

import io.github.cruciblemc.necrotempus.modules.features.chatheads.client.render.ChatHeadRenderer;
import io.github.cruciblemc.necrotempus.modules.mixin.mixins.minecraft.GuiNewChatMixin;

class ChatFormattingUtilsTest {

    private final boolean originalRenderer = AngelicaConfig.enableFontRenderer;
    private final boolean originalConversion = AngelicaConfig.enableAmpersandConversion;

    @BeforeEach
    void enableAngelicaFormatting() {
        AngelicaConfig.enableFontRenderer = true;
        AngelicaConfig.enableAmpersandConversion = true;
        ChatFormattingUtils.setAngelicaPresent(true);
    }

    @AfterEach
    void restoreFormatting() {
        ChatFormattingUtils.setAngelicaPresent(false);
        AngelicaConfig.enableFontRenderer = originalRenderer;
        AngelicaConfig.enableAmpersandConversion = originalConversion;
    }

    @Test
    void preservesVanillaFormattingAndLiteralUnknownCodes() {
        ChatFormattingUtils.setAngelicaPresent(false);
        assertEquals("\u00a74hello", ChatFormattingUtils.translateAlternateColorCodes("&4hello"));
        assertEquals("plain text", ChatFormattingUtils.translateAlternateColorCodes("plain text"));
        assertEquals("&zhello", ChatFormattingUtils.translateAlternateColorCodes("&zhello"));
        assertEquals("\u00a7lbold \u00a7rreset", ChatFormattingUtils.translateAlternateColorCodes("&lbold &rreset"));
        assertEquals("&4hello", ChatFormattingUtils.restoreAlternateColorCodes("\u00a74hello"));
    }

    @Test
    void worksWhenAngelicaClassesAreUnavailable() throws Exception {
        String name = ChatFormattingUtils.class.getName();
        java.net.URL location = ChatFormattingUtils.class.getProtectionDomain()
            .getCodeSource()
            .getLocation();
        try (java.net.URLClassLoader loader = new java.net.URLClassLoader(
            new java.net.URL[] { location },
            getClass().getClassLoader()) {

            @Override
            protected Class<?> loadClass(String requested, boolean resolve) throws ClassNotFoundException {
                if (requested.startsWith("com.gtnewhorizons.angelica.")) throw new ClassNotFoundException(requested);
                if (!requested.equals(name)) return super.loadClass(requested, resolve);
                Class<?> result = findLoadedClass(requested);
                if (result == null) result = findClass(requested);
                if (resolve) resolveClass(result);
                return result;
            }
        }) {
            Class<?> formatting = loader.loadClass(name);
            assertEquals(
                "Bob",
                formatting.getMethod("stripFormatting", String.class)
                    .invoke(null, "&aBob"));
            assertEquals(
                "&qBob",
                formatting.getMethod("stripFormatting", String.class)
                    .invoke(null, "&qBob"));
        }
    }

    @Test
    void effectsDoNotCountAsVisibleCharactersOrHideTheName() {
        String text = "[VIP] \u00a7q\u00a7zNick: hello";
        assertEquals(17, ChatFormattingUtils.visibleCharacterCount(text));
        assertEquals(6, ChatFormattingUtils.findUniqueNameInMessage(text, "Nick"));
    }

    @Test
    void effectsArePreservedAfterTheHeadSplit() {
        String text = "\u00a7q\u00a7z[VIP] Nick: hello";
        int split = ChatHeadRenderer.getFormattedIndexForVisibleIndex(text, 6);
        assertEquals(text.indexOf("Nick"), split);
        assertEquals("\u00a7q\u00a7z", ChatHeadRenderer.getActiveFormatting(text, split));
    }

    @Test
    void rgbAndGradientPayloadsAreSkippedTogether() {
        assertEquals("Nick", ChatFormattingUtils.stripFormatting("\u00a7gNick"));
        String rgb = "\u00a7x\u00a7F\u00a7F\u00a70\u00a70\u00a70\u00a70";
        String gradient = "\u00a7g" + rgb + "\u00a7x\u00a70\u00a70\u00a70\u00a70\u00a7F\u00a7F";
        for (String code : new String[] { rgb, gradient, "\u00a7u" + rgb }) {
            String text = "[VIP] " + code + "Nick: hello";
            assertEquals(17, ChatFormattingUtils.visibleCharacterCount(text));
            assertEquals(6, ChatFormattingUtils.findUniqueNameInMessage(text, "Nick"));
            int split = ChatHeadRenderer.getFormattedIndexForVisibleIndex(text, 6);
            assertEquals(text.indexOf("Nick"), split);
            assertEquals(code, ChatHeadRenderer.getActiveFormatting(text, split));
        }
    }

    @Test
    void convertsAngelicaEffectsRgbGradientsAndShadowUsingItsConverter() {
        String[] codes = { "&q", "&z", "&v", "&u", "&#FF0000", "&g&#FF0000&#0000FF", "&u&#FF0000" };
        for (String code : codes) {
            String text = "[VIP] " + code + "Nick: hello";
            String formatted = ChatFormattingUtils.translateAlternateColorCodes(text);
            assertEquals(17, ChatFormattingUtils.visibleCharacterCount(text), code);
            assertEquals(6, ChatFormattingUtils.findUniqueNameInMessage(text, "Nick"), code);
            assertEquals(
                formatted.indexOf("Nick"),
                ChatHeadRenderer.getFormattedIndexForVisibleIndex(formatted, 6),
                code);
        }
    }

    @Test
    void recognizesEveryNativeSingleCodeInBothCasesAndEncodings() {
        for (char code : com.gtnewhorizons.angelica.client.font.ColorCodeUtils.VALID_SINGLE_CODES.toCharArray()) {
            for (char variant : new char[] { code, Character.toUpperCase(code) }) {
                for (char marker : new char[] { '&', '\u00a7' }) {
                    String formatting = "" + marker + variant;
                    String formatted = ChatFormattingUtils
                        .translateAlternateColorCodes(formatting + "[VIP] Nick: hello");
                    assertEquals(17, ChatFormattingUtils.visibleCharacterCount(formatted), formatting);
                    assertEquals(6, ChatFormattingUtils.findUniqueNameInMessage(formatted, "Nick"), formatting);
                    int split = ChatFormattingUtils.getFormattedIndexForVisibleIndex(formatted, 6);
                    assertEquals(formatted.indexOf("Nick"), split, formatting);
                    assertEquals(
                        formatted.substring(0, 2),
                        ChatFormattingUtils.getActiveFormatting(formatted, split),
                        formatting);
                }
            }
        }
    }

    @Test
    void keepsCompoundCodesAndDocumentedCombinationsAtTheSameVisiblePosition() {
        for (String code : new String[] { "&#fF6b4A", "&G&#Ff0000&#FfFf00", "&U&#7b68Ee", "&c&l", "&a&o", "&e&n",
            "&g&#00CED1&#FF69B4&z&l", "&#FF69B4&l&o", "&q&z&v&u&r" }) {
            String formattedCode = ChatFormattingUtils.translateAlternateColorCodes(code);
            for (String prefix : new String[] { code, formattedCode }) {
                String text = prefix + "[VIP] Nick: hello";
                String formatted = ChatFormattingUtils.translateAlternateColorCodes(text);
                assertEquals("[VIP] Nick: hello", ChatFormattingUtils.stripFormatting(text), prefix);
                assertEquals(17, ChatFormattingUtils.visibleCharacterCount(text), prefix);
                assertEquals(6, ChatFormattingUtils.findUniqueNameInMessage(text, "Nick"), prefix);
                int split = ChatFormattingUtils.getFormattedIndexForVisibleIndex(formatted, 6);
                assertEquals(formatted.indexOf("Nick"), split, prefix);
                assertEquals(formattedCode, ChatFormattingUtils.getActiveFormatting(formatted, split), prefix);
                for (int end = 1; end < formattedCode.length(); end++) {
                    int boundary = ChatFormattingUtils.safeFormattingBoundary(formatted, end);
                    assertEquals("", ChatFormattingUtils.stripFormatting(formatted.substring(0, boundary)), prefix);
                }
            }
        }
    }

    @Test
    void skipsUnrecognizedSectionPairsJustLikeAngelicasCharacterLoop() {
        assertEquals("[VIP] Nick", ChatFormattingUtils.stripFormatting("\u00a7h[VIP] \u00a7iNick"));
        assertEquals(6, ChatFormattingUtils.findUniqueNameInMessage("\u00a7h[VIP] \u00a7iNick", "Nick"));
        assertEquals("&h[VIP] &iNick", ChatFormattingUtils.stripFormatting("&h[VIP] &iNick"));
        ChatFormattingUtils.setAngelicaPresent(false);
        assertEquals("\u00a7h[VIP] \u00a7iNick", ChatFormattingUtils.stripFormatting("\u00a7h[VIP] \u00a7iNick"));
    }

    @Test
    void escapedAmpersandsRemainVisibleAndDisabledConversionKeepsEffectsLiteral() {
        assertEquals("&qNick", ChatFormattingUtils.stripFormatting("\\&qNick"));
        assertEquals(6, ChatFormattingUtils.visibleCharacterCount("\\&qNick"));
        AngelicaConfig.enableAmpersandConversion = false;
        assertEquals("&qNick", ChatFormattingUtils.translateAlternateColorCodes("&qNick"));
        assertEquals(6, ChatFormattingUtils.visibleCharacterCount("&qNick"));
        AngelicaConfig.enableFontRenderer = false;
        assertEquals("&zNick", ChatFormattingUtils.translateAlternateColorCodes("&zNick"));
        assertEquals("\u00a7aNick", ChatFormattingUtils.translateAlternateColorCodes("&aNick"));
    }

    @Test
    void trimmingNeverSplitsCompoundFormattingCodes() {
        for (String code : new String[] { "&q", "&#FF0000", "&g&#FF0000&#0000FF", "&u&#FF0000" }) {
            String formatted = ChatFormattingUtils.translateAlternateColorCodes("prefix " + code + "Nick");
            int start = 7;
            int end = formatted.indexOf("Nick");
            for (int split = start + 1; split < end; split++) {
                assertEquals(
                    start,
                    ChatFormattingUtils.safeFormattingBoundary(formatted, split),
                    code + " at " + split);
            }
            assertEquals(end, ChatFormattingUtils.safeFormattingBoundary(formatted, end));
        }
    }

    @Test
    void preservesEffectTogglesAndResetsInTheirOriginalOrder() {
        String text = "\u00a7z\u00a7q[VIP] \u00a7a\u00a7zNick";
        assertEquals("\u00a7z\u00a7q\u00a7a\u00a7z", ChatHeadRenderer.getActiveFormatting(text, text.indexOf("Nick")));
    }

    @Test
    void wrappedRemainderPreservesEffectsAndUsesNormalizedIndices() throws Exception {
        GuiNewChatMixin chat = new GuiNewChatMixin() {

            @Override
            public boolean getChatOpen() {
                return false;
            }

            @Override
            public float func_146244_h() {
                return 1;
            }

            @Override
            public int func_146232_i() {
                return 10;
            }
        };
        Method normalize = GuiNewChatMixin.class
            .getDeclaredMethod("necrotempus$normalizeDisplayFormatting", String.class);
        normalize.setAccessible(true);
        Method remainder = GuiNewChatMixin.class
            .getDeclaredMethod("necrotempus$preserveWrappedFormatting", String.class, int.class);
        remainder.setAccessible(true);
        for (String code : new String[] { "&q&z", "&g&#FF0000&#0000FF", "&u&#FF0000" }) {
            String formatted = (String) normalize.invoke(chat, code + "[VIP] Nick: hello");
            int split = ChatHeadRenderer.getFormattedIndexForVisibleIndex(formatted, 6);
            String tail = (String) remainder.invoke(chat, formatted, split);
            assertEquals("Nick: hello", ChatFormattingUtils.stripFormatting(tail));
            assertEquals(11, ChatFormattingUtils.visibleCharacterCount(tail));
            assertEquals(ChatFormattingUtils.translateAlternateColorCodes(code) + "Nick: hello", tail);
        }
        ChatFormattingUtils.setAngelicaPresent(false);
        assertEquals("Nick", remainder.invoke(chat, "\u00a7a[VIP] Nick", 8));
    }
}
