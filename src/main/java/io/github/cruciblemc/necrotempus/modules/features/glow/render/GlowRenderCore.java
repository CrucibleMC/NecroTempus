package io.github.cruciblemc.necrotempus.modules.features.glow.render;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;

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
import io.github.cruciblemc.necrotempus.modules.features.glow.GlowVolume;
import io.github.cruciblemc.necrotempus.modules.features.glow.GlowingEntityRegistry;
import io.github.cruciblemc.necrotempus.modules.features.glow.client.GlowClientManager;

/** Renders glowing entities as flat silhouettes into a secondary framebuffer. */
public class GlowRenderCore {

    public static final GlowRenderCore INSTANCE = new GlowRenderCore();
    private static final FloatBuffer CLEAR_COLOR_BUFFER = ByteBuffer.allocateDirect(16 * Float.BYTES)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer();
    private static final DoubleBuffer CLEAR_DEPTH_BUFFER = ByteBuffer.allocateDirect(16 * Double.BYTES)
        .order(ByteOrder.nativeOrder())
        .asDoubleBuffer();

    /**
     * True while {@link #renderOutlines} is rendering entity silhouettes.
     * Mixins (e.g. {@code RendererLivingEntity}) check this to skip nameplate
     * rendering that would otherwise pollute the glow mask.
     */
    public static boolean silhouettePassActive = false;

    private static final int DEFAULT_RGB = 0xFFFFFF; // 1.7.10 has no team colours; fallback constant
    // Keep the mask at native resolution; upscaling a half-resolution binary silhouette makes
    // diagonal edges visibly stair-step when the glow is active.
    private static final int GLOW_SCALE = 1;

    private Framebuffer glowFbo;
    private GlShaderProgram silhouette;

    private Framebuffer blurFbo;
    private GlShaderProgram outline;
    private GlShaderProgram blit;

    private GlowRenderCore() {}

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

    /** Recreate the glow framebuffer if the main framebuffer size changed. */
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

        GlPlatform gl = GlPlatformFactory.get();

        int prevProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        boolean prevTexture = gl.isEnabled(GL11.GL_TEXTURE_2D);
        int prevTextureBinding = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        boolean prevDepth = gl.isEnabled(GL11.GL_DEPTH_TEST);
        boolean prevLighting = gl.isEnabled(GL11.GL_LIGHTING);
        boolean prevLight0 = gl.isEnabled(GL11.GL_LIGHT0);
        boolean prevLight1 = gl.isEnabled(GL11.GL_LIGHT1);
        boolean prevColorMaterial = gl.isEnabled(GL11.GL_COLOR_MATERIAL);
        CLEAR_COLOR_BUFFER.clear();
        GL11.glGetFloat(GL11.GL_COLOR_CLEAR_VALUE, CLEAR_COLOR_BUFFER);
        float prevClearRed = CLEAR_COLOR_BUFFER.get(0);
        float prevClearGreen = CLEAR_COLOR_BUFFER.get(1);
        float prevClearBlue = CLEAR_COLOR_BUFFER.get(2);
        float prevClearAlpha = CLEAR_COLOR_BUFFER.get(3);
        CLEAR_DEPTH_BUFFER.clear();
        GL11.glGetDouble(GL11.GL_DEPTH_CLEAR_VALUE, CLEAR_DEPTH_BUFFER);
        double prevClearDepth = CLEAR_DEPTH_BUFFER.get(0);

        try {
            ensureSize();
            glowFbo.framebufferClear();
            glowFbo.bindFramebuffer(true); // true sets the framebuffer viewport for the silhouette render

            // Always render the full silhouette with depth test off, so the outline shows through walls
            // (matching 1.9+ vanilla glow).
            gl.disable(GL11.GL_DEPTH_TEST);
            gl.disable(GL11.GL_LIGHTING);
            gl.disable(GL11.GL_LIGHT0);
            gl.disable(GL11.GL_LIGHT1);
            gl.disable(GL11.GL_COLOR_MATERIAL);
            gl.useProgram(silhouette.id());
            GL20.glUniform1i(silhouette.uniform("uTex"), 0);
            GL20.glUniform1f(silhouette.uniform("uSolid"), 0.0F);

            silhouettePassActive = true;
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

            GL20.glUniform1f(silhouette.uniform("uSolid"), 1.0F);
            boolean volumeTexture = gl.isEnabled(GL11.GL_TEXTURE_2D);
            gl.disable(GL11.GL_TEXTURE_2D);
            try {
                for (GlowVolume volume : registry.glowingVolumes()) {
                    int rgb = volume.rgb < 0 ? DEFAULT_RGB : volume.rgb;
                    GL20.glUniform3f(
                        silhouette.uniform("uColor"),
                        ((rgb >> 16) & 0xFF) / 255.0F,
                        ((rgb >> 8) & 0xFF) / 255.0F,
                        (rgb & 0xFF) / 255.0F);
                    drawVolumeOutline(volume);
                }
            } finally {
                setEnabled(gl, GL11.GL_TEXTURE_2D, volumeTexture);
            }
        } finally {
            silhouettePassActive = false;
            gl.useProgram(prevProgram);
            gl.bindTexture2D(prevTextureBinding);
            setEnabled(gl, GL11.GL_TEXTURE_2D, prevTexture);
            setEnabled(gl, GL11.GL_DEPTH_TEST, prevDepth);
            setEnabled(gl, GL11.GL_LIGHTING, prevLighting);
            setEnabled(gl, GL11.GL_LIGHT0, prevLight0);
            setEnabled(gl, GL11.GL_LIGHT1, prevLight1);
            setEnabled(gl, GL11.GL_COLOR_MATERIAL, prevColorMaterial);
            mc.getFramebuffer()
                .bindFramebuffer(true);
            GL11.glClearColor(prevClearRed, prevClearGreen, prevClearBlue, prevClearAlpha);
            GL11.glClearDepth(prevClearDepth);
        }
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

    private void drawVolumeOutline(GlowVolume volume) {
        double minX = volume.minX - RenderManager.renderPosX;
        double minY = volume.minY - RenderManager.renderPosY;
        double minZ = volume.minZ - RenderManager.renderPosZ;
        double maxX = volume.maxX - RenderManager.renderPosX;
        double maxY = volume.maxY - RenderManager.renderPosY;
        double maxZ = volume.maxZ - RenderManager.renderPosZ;

        GL11.glBegin(GL11.GL_LINES);
        edge(minX, minY, minZ, maxX, minY, minZ);
        edge(maxX, minY, minZ, maxX, minY, maxZ);
        edge(maxX, minY, maxZ, minX, minY, maxZ);
        edge(minX, minY, maxZ, minX, minY, minZ);
        edge(minX, maxY, minZ, maxX, maxY, minZ);
        edge(maxX, maxY, minZ, maxX, maxY, maxZ);
        edge(maxX, maxY, maxZ, minX, maxY, maxZ);
        edge(minX, maxY, maxZ, minX, maxY, minZ);
        edge(minX, minY, minZ, minX, maxY, minZ);
        edge(maxX, minY, minZ, maxX, maxY, minZ);
        edge(maxX, minY, maxZ, maxX, maxY, maxZ);
        edge(minX, minY, maxZ, minX, maxY, maxZ);
        GL11.glEnd();
    }

    private void edge(double x1, double y1, double z1, double x2, double y2, double z2) {
        GL11.glVertex3d(x1, y1, z1);
        GL11.glVertex3d(x2, y2, z2);
    }

    private void ensureBlurSize() {
        int w = glowFbo.framebufferWidth;
        int h = glowFbo.framebufferHeight;
        if (blurFbo == null || blurFbo.framebufferWidth != w || blurFbo.framebufferHeight != h) {
            if (blurFbo != null) blurFbo.deleteFramebuffer();
            blurFbo = new Framebuffer(w, h, false);
            blurFbo.setFramebufferColor(0.0F, 0.0F, 0.0F, 0.0F);
            blurFbo.setFramebufferFilter(GL11.GL_LINEAR); // filter the ring when it is sampled during compositing
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

        GlPlatform gl = GlPlatformFactory.get();
        boolean prevBlend = gl.isEnabled(GL11.GL_BLEND);
        boolean prevDepth = gl.isEnabled(GL11.GL_DEPTH_TEST);
        boolean prevLighting = gl.isEnabled(GL11.GL_LIGHTING);
        boolean prevTexture = gl.isEnabled(GL11.GL_TEXTURE_2D);
        int prevProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        int prevTextureBinding = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        int prevBlendSrc = GL11.glGetInteger(GL11.GL_BLEND_SRC);
        int prevBlendDst = GL11.glGetInteger(GL11.GL_BLEND_DST);
        CLEAR_COLOR_BUFFER.clear();
        GL11.glGetFloat(GL11.GL_COLOR_CLEAR_VALUE, CLEAR_COLOR_BUFFER);
        float prevClearRed = CLEAR_COLOR_BUFFER.get(0);
        float prevClearGreen = CLEAR_COLOR_BUFFER.get(1);
        float prevClearBlue = CLEAR_COLOR_BUFFER.get(2);
        float prevClearAlpha = CLEAR_COLOR_BUFFER.get(3);
        CLEAR_DEPTH_BUFFER.clear();
        GL11.glGetDouble(GL11.GL_DEPTH_CLEAR_VALUE, CLEAR_DEPTH_BUFFER);
        double prevClearDepth = CLEAR_DEPTH_BUFFER.get(0);
        // The setting is in screen pixels; convert it to framebuffer pixels using GLOW_SCALE.
        float width = GlowOutlineSizing.framebufferPixels(NecroTempusConfig.glowOutlineWidth, GLOW_SCALE);
        float blackStrokeWidth = GlowOutlineSizing.blackStrokeFramebufferPixels(GLOW_SCALE);
        float blackStrokeFeather = GlowOutlineSizing.blackStrokeFeatherFramebufferPixels(GLOW_SCALE);
        float tw = 1.0F / glowFbo.framebufferWidth;
        float th = 1.0F / glowFbo.framebufferHeight;

        try {
            ensureBlurSize();
            gl.disable(GL11.GL_DEPTH_TEST);
            gl.disable(GL11.GL_LIGHTING);

            // Outline pass: dilate glowFbo's silhouette by `width` px and subtract the interior -> ring.
            blurFbo.framebufferClear();
            blurFbo.bindFramebuffer(true); // true sets the framebuffer viewport for the outline pass
            gl.useProgram(outline.id());
            GL20.glUniform1i(outline.uniform("uTex"), 0);
            GL20.glUniform2f(outline.uniform("uTexel"), tw, th);
            GL20.glUniform1f(outline.uniform("uWidth"), width);
            GL20.glUniform1f(outline.uniform("uStrokeWidth"), blackStrokeWidth);
            GL20.glUniform1f(outline.uniform("uStrokeFeather"), blackStrokeFeather);
            gl.bindTexture2D(glowFbo.framebufferTexture);
            drawFullscreenQuad(gl);

            // Sample only the ring texture in the blit shader, independent of world-rendering texture state.
            Minecraft.getMinecraft()
                .getFramebuffer()
                .bindFramebuffer(true);
            gl.enable(GL11.GL_BLEND);
            gl.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            gl.useProgram(blit.id());
            GL20.glUniform1i(blit.uniform("uTex"), 0);
            gl.bindTexture2D(blurFbo.framebufferTexture);
            drawFullscreenQuad(gl);
        } finally {
            gl.useProgram(prevProgram);
            gl.blendFunc(prevBlendSrc, prevBlendDst);
            gl.bindTexture2D(prevTextureBinding);
            setEnabled(gl, GL11.GL_BLEND, prevBlend);
            setEnabled(gl, GL11.GL_DEPTH_TEST, prevDepth);
            setEnabled(gl, GL11.GL_LIGHTING, prevLighting);
            setEnabled(gl, GL11.GL_TEXTURE_2D, prevTexture);
            Minecraft.getMinecraft()
                .getFramebuffer()
                .bindFramebuffer(true);
            GL11.glClearColor(prevClearRed, prevClearGreen, prevClearBlue, prevClearAlpha);
            GL11.glClearDepth(prevClearDepth);
        }
    }

    public void renderAndComposite(float partialTicks) {
        renderOutlines(partialTicks);
        GlowingEntityRegistry registry = GlowClientManager.getInstance()
            .registry();
        if (NecroTempusConfig.enableEntityGlow && !registry.isEmpty()) {
            composite(partialTicks);
        }
    }

    private void drawFullscreenQuad(GlPlatform gl) {
        int previousMatrixMode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
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
        GL11.glMatrixMode(previousMatrixMode);
    }

    private static void setEnabled(GlPlatform gl, int capability, boolean enabled) {
        if (enabled) gl.enable(capability);
        else gl.disable(capability);
    }
}
