package io.github.cruciblemc.necrotempus.modules.features.glow.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class GlowOutlineSizingTest {

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
