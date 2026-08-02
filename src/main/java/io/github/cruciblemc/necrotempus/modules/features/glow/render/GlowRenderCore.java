package io.github.cruciblemc.necrotempus.modules.features.glow.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import io.github.cruciblemc.necrotempus.NecroTempusConfig;
import io.github.cruciblemc.necrotempus.modules.features.glow.GlowingEntityRegistry;
import io.github.cruciblemc.necrotempus.modules.features.glow.client.GlowClientManager;

/** Renders glowing entities as flat silhouettes into a secondary framebuffer. */
public class GlowRenderCore {

    public static final GlowRenderCore INSTANCE = new GlowRenderCore();

    /**
     * True while {@link #renderOutlines} is rendering entity silhouettes.
     * Mixins (e.g. {@code RendererLivingEntity}) check this to skip nameplate
     * rendering that would otherwise pollute the glow mask.
     */
    public static boolean silhouettePassActive = false;

    private static final int DEFAULT_RGB = 0xFFFFFF; // 1.7.10 has no team colours; fallback constant
    // Render the glow at 1/GLOW_SCALE resolution: the fullscreen outline shader is the cost, so fewer
    // fragments is a big win, and the linear upscale on composite doubles as free anti-aliasing.
    private static final int GLOW_SCALE = 2;

    private Framebuffer glowFbo;
    private GlShaderProgram silhouette;

    private Framebuffer blurFbo;
    private GlShaderProgram outline;
    private GlShaderProgram blit;

    private GlowRenderCore() {}

    public Framebuffer glowFramebuffer() {
        return glowFbo;
    }

    private void lazyInit() {
        if (silhouette == null) {
            silhouette = new GlShaderProgram(
                new ResourceLocation("necrotempus", "shaders/program/glow_silhouette.vsh"),
                new ResourceLocation("necrotempus", "shaders/program/glow_silhouette.fsh"));
        }
    }

    private void lazyInitComposite() {
        if (outline == null) {
            outline = new GlShaderProgram(
                new ResourceLocation("necrotempus", "shaders/program/glow_outline.vsh"),
                new ResourceLocation("necrotempus", "shaders/program/glow_outline.fsh"));
        }
        if (blit == null) {
            blit = new GlShaderProgram(
                new ResourceLocation("necrotempus", "shaders/program/glow_outline.vsh"),
                new ResourceLocation("necrotempus", "shaders/program/glow_blit.fsh"));
        }
    }

    /** Recreate the (downscaled) glow framebuffer if the main framebuffer size changed. */
    public void ensureSize() {
        Framebuffer main = Minecraft.getMinecraft()
            .getFramebuffer();
        int w = Math.max(1, main.framebufferWidth / GLOW_SCALE);
        int h = Math.max(1, main.framebufferHeight / GLOW_SCALE);
        if (glowFbo == null || glowFbo.framebufferWidth != w || glowFbo.framebufferHeight != h) {
            if (glowFbo != null) glowFbo.deleteFramebuffer();
            glowFbo = new Framebuffer(w, h, true); // useDepth=true
            glowFbo.setFramebufferColor(0.0F, 0.0F, 0.0F, 0.0F);
            glowFbo.setFramebufferFilter(GL11.GL_LINEAR); // smooth when the outline shader samples it
        }
    }

    public void renderOutlines(float partialTicks) {
        GlowingEntityRegistry registry = GlowClientManager.getInstance()
            .registry();
        if (!NecroTempusConfig.enableEntityGlow || registry.isEmpty()) return;

        if (!OpenGlHelper.isFramebufferEnabled()) return; // FBOs unsupported -> disable gracefully

        Minecraft mc = Minecraft.getMinecraft();
        World world = mc.theWorld;
        if (world == null) return;

        lazyInit();
        if (!silhouette.valid()) return;
        ensureSize();

        GlPlatform gl = GlPlatformFactory.get();

        glowFbo.framebufferClear();
        glowFbo.bindFramebuffer(true); // true -> set the downscaled viewport for the silhouette render

        // Always render the full silhouette with depth test off, so the outline shows through walls
        // (matching 1.9+ vanilla glow).
        boolean prevDepth = gl.isEnabled(GL11.GL_DEPTH_TEST);
        gl.disable(GL11.GL_DEPTH_TEST);
        boolean prevLighting = gl.isEnabled(GL11.GL_LIGHTING);
        boolean prevLight0 = gl.isEnabled(GL11.GL_LIGHT0);
        boolean prevLight1 = gl.isEnabled(GL11.GL_LIGHT1);
        boolean prevColorMaterial = gl.isEnabled(GL11.GL_COLOR_MATERIAL);
        gl.disable(GL11.GL_LIGHTING);
        gl.disable(GL11.GL_LIGHT0);
        gl.disable(GL11.GL_LIGHT1);
        gl.disable(GL11.GL_COLOR_MATERIAL);
        gl.useProgram(silhouette.id());
        GL20.glUniform1i(silhouette.uniform("uTex"), 0);

        silhouettePassActive = true;
        try {
            for (int id : registry.glowingIds()) {
                Entity e = world.getEntityByID(id);
                if (e == null) continue;
                Integer stored = registry.colorFor(id);
                int rgb = (stored == null || stored < 0) ? DEFAULT_RGB : stored;
                GL20.glUniform3f(
                    silhouette.uniform("uColor"),
                    ((rgb >> 16) & 0xFF) / 255.0F,
                    ((rgb >> 8) & 0xFF) / 255.0F,
                    (rgb & 0xFF) / 255.0F);
                renderEntitySilhouette(e, partialTicks);
            }
        } finally {
            silhouettePassActive = false;
        }

        gl.useProgram(0);
        if (prevDepth) gl.enable(GL11.GL_DEPTH_TEST);
        else gl.disable(GL11.GL_DEPTH_TEST);
        if (prevLighting) gl.enable(GL11.GL_LIGHTING);
        if (prevLight0) gl.enable(GL11.GL_LIGHT0);
        if (prevLight1) gl.enable(GL11.GL_LIGHT1);
        if (prevColorMaterial) gl.enable(GL11.GL_COLOR_MATERIAL);

        // Restore the main framebuffer + full viewport so the rest of the frame draws normally.
        mc.getFramebuffer()
            .bindFramebuffer(true);
    }

    /**
     * Render only the entity's model via Render.doRender, deliberately NOT going through
     * RenderManager.renderEntitySimple (which also calls doRenderShadowAndFire). That keeps the
     * ground drop-shadow and fire overlay out of the silhouette mask, flag-independently — so it
     * also suppresses the shadow under Angelica, whose shadow toggle is separate from fancyGraphics.
     * Replicates renderEntityStatic's interpolated position/yaw; lightmap/brightness setup is skipped
     * because the silhouette shader ignores lighting and texture colour anyway.
     */
    private void renderEntitySilhouette(Entity e, float partialTicks) {
        Render render = RenderManager.instance.getEntityRenderObject(e);
        if (render == null) return;
        double x = e.lastTickPosX + (e.posX - e.lastTickPosX) * partialTicks - RenderManager.renderPosX;
        double y = e.lastTickPosY + (e.posY - e.lastTickPosY) * partialTicks - RenderManager.renderPosY;
        double z = e.lastTickPosZ + (e.posZ - e.lastTickPosZ) * partialTicks - RenderManager.renderPosZ;
        float yaw = e.prevRotationYaw + (e.rotationYaw - e.prevRotationYaw) * partialTicks;
        render.doRender(e, x, y, z, yaw, partialTicks);
    }

    private void ensureBlurSize() {
        int w = glowFbo.framebufferWidth;
        int h = glowFbo.framebufferHeight;
        if (blurFbo == null || blurFbo.framebufferWidth != w || blurFbo.framebufferHeight != h) {
            if (blurFbo != null) blurFbo.deleteFramebuffer();
            blurFbo = new Framebuffer(w, h, false);
            blurFbo.setFramebufferColor(0.0F, 0.0F, 0.0F, 0.0F);
            blurFbo.setFramebufferFilter(GL11.GL_LINEAR); // smooth upscale when blitting the ring to screen
        }
    }

    /**
     * Turn the filled silhouette in glowFbo into an outline: one dilate-and-subtract pass writes
     * the coloured ring into blurFbo, then blend that ring over the scene. glowFbo (the original
     * mask) is left intact — the outline shader reads it.
     */
    public void composite(float partialTicks) {
        if (glowFbo == null) return;
        lazyInitComposite();
        if (!outline.valid() || !blit.valid()) return;
        ensureBlurSize();

        GlPlatform gl = GlPlatformFactory.get();
        boolean prevBlend = gl.isEnabled(GL11.GL_BLEND);
        boolean prevDepth = gl.isEnabled(GL11.GL_DEPTH_TEST);
        boolean prevLighting = gl.isEnabled(GL11.GL_LIGHTING);
        // The setting is expressed in final screen pixels. Convert it to the downscaled FBO grid so
        // compositing does not multiply the outline (and its black edge) by GLOW_SCALE.
        float width = GlowOutlineSizing.framebufferPixels(NecroTempusConfig.glowOutlineWidth, GLOW_SCALE);
        float blackStrokeWidth = GlowOutlineSizing.blackStrokeFramebufferPixels(GLOW_SCALE);
        float blackStrokeFeather = GlowOutlineSizing.blackStrokeFeatherFramebufferPixels(GLOW_SCALE);
        float tw = 1.0F / glowFbo.framebufferWidth;
        float th = 1.0F / glowFbo.framebufferHeight;

        gl.disable(GL11.GL_DEPTH_TEST);
        gl.disable(GL11.GL_LIGHTING);

        // Outline pass: dilate glowFbo's silhouette by `width` px and subtract the interior -> ring.
        blurFbo.framebufferClear();
        blurFbo.bindFramebuffer(true); // downscaled viewport for the outline pass
        gl.useProgram(outline.id());
        GL20.glUniform1i(outline.uniform("uTex"), 0);
        GL20.glUniform2f(outline.uniform("uTexel"), tw, th);
        GL20.glUniform1f(outline.uniform("uWidth"), width);
        GL20.glUniform1f(outline.uniform("uStrokeWidth"), blackStrokeWidth);
        GL20.glUniform1f(outline.uniform("uStrokeFeather"), blackStrokeFeather);
        GL20.glUniform1f(outline.uniform("uTime"), (float) (Minecraft.getSystemTime() / 1000.0));
        gl.bindTexture2D(glowFbo.framebufferTexture);
        drawFullscreenQuad(gl);

        // Composite the outline ring over the main framebuffer (full viewport; the half-res ring is
        // upscaled via GL_LINEAR filtering). Blit through a shader (not fixed-function) so it samples
        // ONLY the ring texture, immune to leftover lightmap / texture-env / glColor state from world
        // rendering (e.g. the chest tile-entity renderer) that would otherwise darken the outline.
        Minecraft.getMinecraft()
            .getFramebuffer()
            .bindFramebuffer(true);
        gl.enable(GL11.GL_BLEND);
        gl.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        gl.useProgram(blit.id());
        GL20.glUniform1i(blit.uniform("uTex"), 0);
        gl.bindTexture2D(blurFbo.framebufferTexture);
        drawFullscreenQuad(gl);
        gl.useProgram(0);

        if (prevDepth) gl.enable(GL11.GL_DEPTH_TEST);
        else gl.disable(GL11.GL_DEPTH_TEST);
        if (prevLighting) gl.enable(GL11.GL_LIGHTING);
        else gl.disable(GL11.GL_LIGHTING);
        if (!prevBlend) gl.disable(GL11.GL_BLEND);
    }

    /** Entry point the mixin calls: outline pass then composite. */
    public void renderAndComposite(float partialTicks) {
        renderOutlines(partialTicks);
        GlowingEntityRegistry registry = GlowClientManager.getInstance()
            .registry();
        if (NecroTempusConfig.enableEntityGlow && !registry.isEmpty()) {
            composite(partialTicks);
        }
    }

    private void drawFullscreenQuad(GlPlatform gl) {
        // Ortho fullscreen quad in NDC via immediate mode.
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        gl.enable(GL11.GL_TEXTURE_2D);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0, 0);
        GL11.glVertex2f(-1, -1);
        GL11.glTexCoord2f(1, 0);
        GL11.glVertex2f(1, -1);
        GL11.glTexCoord2f(1, 1);
        GL11.glVertex2f(1, 1);
        GL11.glTexCoord2f(0, 1);
        GL11.glVertex2f(-1, 1);
        GL11.glEnd();
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPopMatrix();
    }
}
