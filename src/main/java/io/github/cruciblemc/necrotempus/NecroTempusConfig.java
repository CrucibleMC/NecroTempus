package io.github.cruciblemc.necrotempus;

import io.github.cruciblemc.omniconfig.api.annotation.AnnotationConfig;
import io.github.cruciblemc.omniconfig.api.annotation.properties.ConfigBoolean;
import io.github.cruciblemc.omniconfig.api.core.VersioningPolicy;

@AnnotationConfig(reloadable = false, policy = VersioningPolicy.NOBLE)
public class NecroTempusConfig {

    @ConfigBoolean(name = "PlayerTab", comment = "Enable PlayerTab Module.", category = "PlayerTab")
    public static boolean PlayerTabEnabled = true;

    @ConfigBoolean(name = "drawNumberedPing", comment = "Should draw number ping instead of the bars.", category = "PlayerTab")
    public static boolean drawNumberedPing = false;

    @ConfigBoolean(name = "drawPlayersHeads", comment = "Should draw the players heads.", category = "PlayerTab")
    public static boolean drawPlayersHeads = true;

    @ConfigBoolean(name = "extraPaddingBars", comment = "Should have extra padding when drawing bars.", category = "PlayerTab")
    public static boolean extraPaddingBars = true;


    @ConfigBoolean(name = "HexColors", comment = "[EXPERIMENTAL] [W.I.P] Enable 1.16+ hex colors system.", category = "HexColors")
    public static boolean HexColorsEnabled = false;


    @ConfigBoolean(name = "ScoreBoard", comment = "Enable ScoreBoard Module", category = "Scoreboard")
    public static boolean ScoreBoardEnabled = true;

    @ConfigBoolean(name = "hideScores", comment = "Should hide scoreboard scores.", category = "Scoreboard")
    public static boolean hideScores = true;

    @ConfigBoolean(name = "titleBackground", comment = "Should scoreboard title have a darker background.", category = "Scoreboard")
    public static boolean titleBackground = false;

}
