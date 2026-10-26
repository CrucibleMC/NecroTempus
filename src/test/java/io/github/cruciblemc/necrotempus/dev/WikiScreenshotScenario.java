package io.github.cruciblemc.necrotempus.dev;

import java.util.Locale;

final class WikiScreenshotScenario {

    enum Scenario {
        BOSSBAR,
        TITLE,
        ACTIONBAR,
        PLAYERTAB,
        GLOW,
        CLEAR
    }

    static Scenario parse(String... arguments) {
        if (arguments == null || arguments.length != 1 || arguments[0] == null) return null;

        try {
            return Scenario.valueOf(arguments[0].toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private WikiScreenshotScenario() {}
}
