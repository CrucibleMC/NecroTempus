package io.github.cruciblemc.necrotempus.modules.features.compat;

import java.lang.reflect.Field;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Detects Angelica (GTNH optimization mod) at runtime and forcefully disables
 * its batched font renderer via reflection. This allows NecroTempus custom glyphs
 * to render correctly, since the vanilla FontRenderer pipeline (renderStringAtPos
 * -> renderCharAtPos) is used instead of Angelica's VBO-based batcher.
 */
public class AngelicaCompat {

    private static final Logger LOG = LogManager.getLogger("AngelicaCompat");
    private static boolean initialized = false;

    public static void disableBatchingIfPresent() {
        if (initialized) return;
        initialized = true;

        try {
            final Class<?> configClass = Class.forName("com.gtnewhorizons.angelica.config.AngelicaConfig");
            final Field field = configClass.getField("enableFontRenderer");

            final boolean currentValue = field.getBoolean(null);
            if (!currentValue) {
                LOG.info("Angelica detected: batched font renderer is already disabled.");
                return;
            }

            field.setBoolean(null, false);
            LOG.info("Angelica detected: forcefully disabled batched font renderer for glyph compatibility.");
        } catch (ClassNotFoundException e) {
            LOG.debug("Angelica not detected, skipping batched font renderer disable.");
        } catch (Exception e) {
            LOG.error("Failed to disable Angelica batched font renderer: {}", e.getMessage());
        }
    }
}
