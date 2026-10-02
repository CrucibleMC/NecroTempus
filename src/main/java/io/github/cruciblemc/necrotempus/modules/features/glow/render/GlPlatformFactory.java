package io.github.cruciblemc.necrotempus.modules.features.glow.render;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class GlPlatformFactory {

    private static final Logger LOG = LogManager.getLogger("GlowGlPlatform");
    private static GlPlatform instance;

    private GlPlatformFactory() {}

    public static GlPlatform get() {
        if (instance == null) {
            instance = detect();
            LOG.info("Entity glow using GlPlatform: {}", instance.name());
        }
        return instance;
    }

    private static GlPlatform detect() {
        try {
            Class.forName("com.gtnewhorizons.angelica.glsm.GLStateManager");
            return new AngelicaGlPlatform();
        } catch (Throwable notPresent) {
            return new VanillaGlPlatform();
        }
    }
}
