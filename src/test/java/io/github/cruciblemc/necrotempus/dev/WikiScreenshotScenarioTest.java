package io.github.cruciblemc.necrotempus.dev;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class WikiScreenshotScenarioTest {

    @Test
    void parsesSupportedScenariosCaseInsensitively() {
        assertEquals(WikiScreenshotScenario.Scenario.BOSSBAR, WikiScreenshotScenario.parse("bossbar"));
        assertEquals(WikiScreenshotScenario.Scenario.TITLE, WikiScreenshotScenario.parse("title"));
        assertEquals(WikiScreenshotScenario.Scenario.ACTIONBAR, WikiScreenshotScenario.parse("actionbar"));
        assertEquals(WikiScreenshotScenario.Scenario.PLAYERTAB, WikiScreenshotScenario.parse("playertab"));
        assertEquals(WikiScreenshotScenario.Scenario.GLOW, WikiScreenshotScenario.parse("glow"));
        assertEquals(WikiScreenshotScenario.Scenario.CLEAR, WikiScreenshotScenario.parse("clear"));
        assertEquals(WikiScreenshotScenario.Scenario.PLAYERTAB, WikiScreenshotScenario.parse("PLAYERTAB"));
    }

    @Test
    void rejectsMissingExtraAndUnknownScenarioArguments() {
        assertNull(WikiScreenshotScenario.parse());
        assertNull(WikiScreenshotScenario.parse((String[]) null));
        assertNull(WikiScreenshotScenario.parse((String) null));
        assertNull(WikiScreenshotScenario.parse("unknown"));
        assertNull(WikiScreenshotScenario.parse("title", "extra"));
    }
}
