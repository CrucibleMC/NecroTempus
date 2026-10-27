package io.github.cruciblemc.necrotempus;

import io.github.cruciblemc.omniconfig.api.annotation.AnnotationConfig;
import io.github.cruciblemc.omniconfig.api.annotation.properties.ConfigBoolean;
import io.github.cruciblemc.omniconfig.api.annotation.properties.ConfigInt;
import io.github.cruciblemc.omniconfig.api.annotation.properties.ConfigString;
import io.github.cruciblemc.omniconfig.api.core.VersioningPolicy;

@AnnotationConfig(reloadable = false, policy = VersioningPolicy.NOBLE)
public class NecroTempusConfig {

    @ConfigBoolean(name = "PlayerTab", comment = "Enable PlayerTab Module.", category = "PlayerTab")
    public static boolean PlayerTabEnabled = true;

    @ConfigBoolean(
        name = "drawNumberedPing",
        comment = "Should draw number ping instead of the bars.",
        category = "PlayerTab")
    public static boolean drawNumberedPing = false;

    @ConfigBoolean(name = "drawPlayersHeads", comment = "Should draw the players heads.", category = "PlayerTab")
    public static boolean drawPlayersHeads = true;

    @ConfigBoolean(
        name = "extraPaddingBars",
        comment = "Should have extra padding when drawing bars.",
        category = "PlayerTab")
    public static boolean extraPaddingBars = true;

    @ConfigBoolean(
        name = "enableHeadsFallback",
        comment = "Use the configured skin URL when the profile has no usable skin.",
        category = "PlayerTab")
    public static boolean enableHeadsFallback = true;

    @ConfigString(
        name = "headsFallbackURL",
        comment = "Fallback skin URL. Supports %name%, %uuid% and %uuidTrim%.",
        category = "PlayerTab")
    public static String headsFallbackURL = "https://crafatar.com/skins/%uuid%.png";

    @ConfigBoolean(name = "ScoreBoard", comment = "Enable ScoreBoard Module", category = "Scoreboard")
    public static boolean ScoreBoardEnabled = true;

    @ConfigBoolean(name = "hideScores", comment = "Should hide scoreboard scores.", category = "Scoreboard")
    public static boolean hideScores = true;

    @ConfigBoolean(
        name = "titleBackground",
        comment = "Should scoreboard title have a darker background.",
        category = "Scoreboard")
    public static boolean titleBackground = false;

    @ConfigBoolean(name = "glyphs", comment = "Enable the Custom Glyphs system.", category = "Glyphs")
    public static boolean glyphs = true;

    @ConfigBoolean(name = "modernFonts", comment = "Enable the modern fonts system.", category = "ModernFonts")
    public static boolean modernFonts = true;

    @ConfigBoolean(name = "ChatHeads", comment = "Enable player heads next to chat messages.", category = "ChatHeads")
    public static boolean ChatHeadsEnabled = true;

    @ConfigString(
        name = "senderDetection",
        comment = "Sender lookup: UUID_ONLY, UUID_AND_HEURISTIC, HEURISTIC_ONLY, or CLICK_EVENTS.",
        category = "ChatHeads")
    public static String ChatHeadsSenderDetection = "UUID_AND_HEURISTIC";

    @ConfigBoolean(
        name = "smartHeuristics",
        comment = "Stop fallback name lookup after a server UUID, unless handleSystemMessages is enabled.",
        category = "ChatHeads")
    public static boolean ChatHeadsSmartHeuristics = true;

    @ConfigBoolean(
        name = "handleSystemMessages",
        comment = "Scan names in messages without server identity. Minecraft 1.7.10 does not reliably distinguish system chat.",
        category = "ChatHeads")
    public static boolean ChatHeadsHandleSystemMessages = true;

    @ConfigBoolean(
        name = "detectNameAliases",
        comment = "Use configured nicknames when detecting senders.",
        category = "ChatHeads")
    public static boolean ChatHeadsDetectNameAliases = true;

    @ConfigString(
        name = "nameAliases",
        comment = "Nickname-to-profile mappings separated by ';', for example nick=ProfileName.",
        category = "ChatHeads")
    public static String ChatHeadsNameAliases = "";

    @ConfigBoolean(
        name = "angelicaGlyphsIntegration",
        comment = "Enable Custom Glyphs integration with Angelica.",
        category = "Angelica")
    public static boolean angelicaGlyphsIntegration = true;

    @ConfigBoolean(
        name = "angelicaModernFontsIntegration",
        comment = "Enable Modern Font integration with Angelica.",
        category = "Angelica")
    public static boolean angelicaModernFontsIntegration = true;

    @ConfigBoolean(name = "enableEntityGlow", comment = "Enable the 1.9 entity glow outline.", category = "EntityGlow")
    public static boolean enableEntityGlow = true;

    @ConfigInt(
        name = "glowOutlineWidth",
        comment = "Outline thickness in pixels (1-4).",
        category = "EntityGlow",
        min = 1,
        max = 4)
    public static int glowOutlineWidth = 1;

    @ConfigBoolean(
        name = "glowDebugKey",
        comment = "Dev only: bind a key to glow the entity you look at.",
        category = "EntityGlow")
    public static boolean glowDebugKey = false;

}
