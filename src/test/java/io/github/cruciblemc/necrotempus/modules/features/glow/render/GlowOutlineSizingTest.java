package io.github.cruciblemc.necrotempus.modules.features.glow.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Scanner;

import org.junit.jupiter.api.Test;

class GlowOutlineSizingTest {

    @Test
    void glowMaskUsesNativeResolutionForCleanSilhouetteEdges() throws ReflectiveOperationException {
        Field scale = GlowRenderCore.class.getDeclaredField("GLOW_SCALE");
        scale.setAccessible(true);
        assertEquals(1, scale.getInt(null));
    }

    @Test
    void outlineUsesContinuousMaskCoverageForAntialiasing() throws IOException {
        try (InputStream stream = getClass()
            .getResourceAsStream("/assets/necrotempus/shaders/program/glow_outline.fsh")) {
            assertNotNull(stream);
            String shader = new Scanner(stream, "UTF-8").useDelimiter("\\A")
                .next();
            assertTrue(shader.contains("float maxAlpha"));
            assertTrue(shader.contains("float maxInnerAlpha"));
            assertFalse(shader.contains("if (s.a > 0.5)"));
        }
    }

    @Test
    void configuredWidthRemainsMeasuredInScreenPixelsWhenFramebufferIsDownscaled() {
        assertEquals(0.5F, GlowOutlineSizing.framebufferPixels(1, 2));
        assertEquals(2.0F, GlowOutlineSizing.framebufferPixels(4, 2));
    }

    @Test
    void blackStrokeUsesOneAndAHalfScreenPixelsWithOnePixelAntialiasing() {
        assertEquals(0.75F, GlowOutlineSizing.blackStrokeFramebufferPixels(2));
        assertEquals(0.5F, GlowOutlineSizing.blackStrokeFeatherFramebufferPixels(2));
    }
}
