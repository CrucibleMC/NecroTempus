package io.github.cruciblemc.necrotempus.modules.features.glow.render;

/**
 * The single seam between vanilla raw-GL and Angelica's GLStateManager. Every GL
 * state change the glow pass makes goes through here so Angelica's state cache stays
 * in sync (the same failure class handled in BatchingFontRendererMixin).
 */
public interface GlPlatform {

    void enable(int cap);

    void disable(int cap);

    boolean isEnabled(int cap);

    void blendFunc(int src, int dst);

    void color4f(float r, float g, float b, float a);

    void bindTexture2D(int textureId);

    void useProgram(int program);

    String name();
}
