package io.github.cruciblemc.necrotempus.modules.features.glow.render;

import org.lwjgl.opengl.GL11;

import com.gtnewhorizons.angelica.glsm.GLStateManager;

/**
 * Routes GL state through Angelica's GLStateManager. Loaded only after
 * GlPlatformFactory confirms Angelica is present, so referencing GLStateManager
 * here never triggers a NoClassDefFoundError when Angelica is absent.
 */
public class AngelicaGlPlatform implements GlPlatform {

    @Override
    public void enable(int cap) {
        GLStateManager.glEnable(cap);
    }

    @Override
    public void disable(int cap) {
        GLStateManager.glDisable(cap);
    }

    @Override
    public boolean isEnabled(int cap) {
        return GLStateManager.glIsEnabled(cap);
    }

    @Override
    public void blendFunc(int src, int dst) {
        GLStateManager.glBlendFunc(src, dst);
    }

    @Override
    public void color4f(float r, float g, float b, float a) {
        GLStateManager.glColor4f(r, g, b, a);
    }

    @Override
    public void bindTexture2D(int textureId) {
        GLStateManager.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
    }

    @Override
    public void useProgram(int program) {
        GLStateManager.glUseProgram(program);
    }

    @Override
    public String name() {
        return "Angelica";
    }
}
