package io.github.cruciblemc.necrotempus.modules.features.glow.render;

import net.minecraft.client.renderer.OpenGlHelper;

import org.lwjgl.opengl.GL11;

public class VanillaGlPlatform implements GlPlatform {

    @Override
    public void enable(int cap) {
        GL11.glEnable(cap);
    }

    @Override
    public void disable(int cap) {
        GL11.glDisable(cap);
    }

    @Override
    public boolean isEnabled(int cap) {
        return GL11.glIsEnabled(cap);
    }

    @Override
    public void blendFunc(int src, int dst) {
        GL11.glBlendFunc(src, dst);
    }

    @Override
    public void color4f(float r, float g, float b, float a) {
        GL11.glColor4f(r, g, b, a);
    }

    @Override
    public void bindTexture2D(int textureId) {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
    }

    @Override
    public void useProgram(int program) {
        OpenGlHelper.func_153161_d(program); // glUseProgram
    }

    @Override
    public String name() {
        return "Vanilla";
    }
}
