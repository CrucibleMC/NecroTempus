package io.github.cruciblemc.necrotempus.modules.features.glow.render;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GlowRenderPolicyTest {

    @Test
    void invisibleEntityIsRenderedOnlyDuringSilhouettePass() {
        assertFalse(GlowRenderPolicy.shouldRenderBaseModel(true, false));
        assertTrue(GlowRenderPolicy.shouldRenderBaseModel(true, true));
    }

    @Test
    void visibleEntityKeepsRenderingOutsideAndInsideSilhouettePass() {
        assertTrue(GlowRenderPolicy.shouldRenderBaseModel(false, false));
        assertTrue(GlowRenderPolicy.shouldRenderBaseModel(false, true));
    }
}
