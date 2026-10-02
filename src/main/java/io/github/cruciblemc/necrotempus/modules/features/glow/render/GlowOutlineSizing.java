package io.github.cruciblemc.necrotempus.modules.features.glow.render;

/** Converts user-facing screen-pixel measurements to the glow framebuffer's pixel grid. */
final class GlowOutlineSizing {

    private static final float BLACK_STROKE_SCREEN_PIXELS = 1.5F;
    private static final float BLACK_STROKE_FEATHER_SCREEN_PIXELS = 1.0F;

    private GlowOutlineSizing() {}

    static float framebufferPixels(int screenPixels, int framebufferScale) {
        return Math.max(1, screenPixels) / (float) framebufferScale;
    }

    static float blackStrokeFramebufferPixels(int framebufferScale) {
        return BLACK_STROKE_SCREEN_PIXELS / framebufferScale;
    }

    static float blackStrokeFeatherFramebufferPixels(int framebufferScale) {
        return BLACK_STROKE_FEATHER_SCREEN_PIXELS / framebufferScale;
    }
}
