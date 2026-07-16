package io.github.cruciblemc.necrotempus.modules.mixin.mixins.angelica;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import io.github.cruciblemc.necrotempus.utils.MathUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gtnewhorizons.angelica.client.font.BatchingFontRenderer;
import com.gtnewhorizons.angelica.client.font.FontProvider;
import com.gtnewhorizons.angelica.client.font.FontStrategist;

import io.github.cruciblemc.necrotempus.NecroTempusConfig;
import io.github.cruciblemc.necrotempus.modules.features.glyphs.CustomGlyphs;
import io.github.cruciblemc.necrotempus.modules.features.glyphs.GlyphsRegistry;
import io.github.cruciblemc.necrotempus.modules.features.glyphs.compat.angelica.FontProviderGlyph;

@Mixin(value = BatchingFontRenderer.class, remap = false)
public abstract class BatchingFontRendererMixin {

    @Shadow
    protected net.minecraft.client.gui.FontRenderer underlying;

    @Shadow
    private void pushTexRect(float x, float y, float w, float h, float itOff, int rgba, float uStart, float vStart,
        float uSz, float vSz, boolean flipV) {}

    @Shadow
    private void pushDrawCmd(int startIdx, int idxCount, ResourceLocation texture, boolean isUnicode) {}

    @Unique
    private boolean nt$isGlyph = false;

    @Unique
    private CustomGlyphs nt$currentGlyph = null;

    @Unique
    private final List<GlyphQuad> nt$glyphQuads = new ArrayList<>();

    @Unique
    private static class GlyphQuad {
        final float x, y;
        final float alpha;
        final ResourceLocation texture;
        final boolean flipV;
        final int hPad, vPad;
        final int glyphW, glyphH;
        final CustomGlyphs.FitMode fitMode;

        GlyphQuad(float x, float y, float alpha, ResourceLocation texture, boolean flipV,
            int hPad, int vPad, int glyphW, int glyphH, CustomGlyphs.FitMode fitMode) {
            this.x = x;
            this.y = y;
            this.alpha = alpha;
            this.texture = texture;
            this.flipV = flipV;
            this.hPad = hPad;
            this.vPad = vPad;
            this.glyphW = glyphW;
            this.glyphH = glyphH;
            this.fitMode = fitMode;
        }
    }

    @Inject(method = "drawString(FFIZZLjava/lang/CharSequence;II)F", at = @At("HEAD"))
    private void nt$clearGlyphQuads(float anchorX, float anchorY, int color, boolean enableShadow, boolean unicodeFlag,
        CharSequence string, int stringOffset, int stringLength, CallbackInfoReturnable<Float> cir) {
        this.nt$glyphQuads.clear();
    }

    @Inject(method = "getCharWidthFine", at = @At("HEAD"), cancellable = true, remap = false)
    private void nt$glyphCharWidth(char chr, CallbackInfoReturnable<Float> cir) {

        if (!NecroTempusConfig.modernFonts) return;

        final CustomGlyphs glyph = GlyphsRegistry.getCandidate(chr);

        if (glyph != null) {
            cir.setReturnValue((float) glyph.getFinalCharacterWidth());
        }

    }

    @Redirect(
        method = "drawString",
        at = @At(
            value = "INVOKE",
            target = "Lcom/gtnewhorizons/angelica/client/font/FontStrategist;getFontProvider(Lcom/gtnewhorizons/angelica/client/font/BatchingFontRenderer;CZZ)Lcom/gtnewhorizons/angelica/client/font/FontProvider;"))
    private FontProvider nt$redirectGetFontProvider(BatchingFontRenderer me, char chr, boolean customFontEnabled,
        boolean forceUnicode) {

        if (NecroTempusConfig.modernFonts) {
            final CustomGlyphs glyph = GlyphsRegistry.getCandidate(chr);
            if (glyph != null) {
                this.nt$isGlyph = true;
                this.nt$currentGlyph = glyph;
                FontProviderGlyph.cachedGlyphScale = glyph.getHeight() / 9.0F;
                return FontProviderGlyph.INSTANCE;
            }
        }

        this.nt$isGlyph = false;
        this.nt$currentGlyph = null;
        return FontStrategist.getFontProvider(me, chr, customFontEnabled, forceUnicode);

    }

    @Redirect(
        method = "drawString",
        at = @At(
            value = "INVOKE",
            target = "Lcom/gtnewhorizons/angelica/client/font/BatchingFontRenderer;pushTexRect(FFFFFIFFFFZ)V"))
    private void nt$redirectPushTexRect(BatchingFontRenderer self, float x, float y, float w, float h, float itOff,
        int rgba, float uStart, float vStart, float uSz, float vSz, boolean flipV) {
        if (this.nt$isGlyph && this.nt$currentGlyph != null) {
            float alpha = ((rgba >> 24) & 0xFF) / 255.0F;
            // Don't batch glyph vertices — will render via GL11 after batch flush
            // to bypass the font shader which only reads texture.a and ignores RGB
            CustomGlyphs g = this.nt$currentGlyph;
            this.nt$glyphQuads.add(new GlyphQuad(x, y, alpha, g.getResource(), flipV,
                g.getHorizontalPadding(), g.getVerticalPadding(), g.getWidth(), g.getHeight(), g.getFitMode()));
        } else {
            pushTexRect(x, y, w, h, itOff, rgba, uStart, vStart, uSz, vSz, flipV);
        }
    }

    @Redirect(
        method = "drawString",
        at = @At(
            value = "INVOKE",
            target = "Lcom/gtnewhorizons/angelica/client/font/BatchingFontRenderer;pushDrawCmd(IILnet/minecraft/util/ResourceLocation;Z)V"))
    private void nt$redirectPushDrawCmd(BatchingFontRenderer self, int startIdx, int idxCount, ResourceLocation texture,
        boolean isUnicode) {
        if (this.nt$isGlyph && texture != null) {
            // Skip the draw command for glyph characters since we skipped vertex data
        } else {
            pushDrawCmd(startIdx, idxCount, texture, isUnicode);
        }
    }

    @Inject(
        method = "drawString(FFIZZLjava/lang/CharSequence;II)F",
        at = @At("RETURN"))
    private void nt$renderGlyphs(float anchorX, float anchorY, int color, boolean enableShadow, boolean unicodeFlag,
        CharSequence string, int stringOffset, int stringLength, CallbackInfoReturnable<Float> cir) {

        if (this.nt$glyphQuads.isEmpty()) return;

        // endBatch() has already been called by drawString's finally block, so the
        // font shader is no longer active. Save/restore GL state for safety.
        int prevProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        int prevTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        boolean prevBlend = GL11.glGetBoolean(GL11.GL_BLEND);

        GL20.glUseProgram(0);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        TextureManager tm = Minecraft.getMinecraft().getTextureManager();

        for (GlyphQuad quad : this.nt$glyphQuads) {
            if (quad.texture == null) continue;

            tm.bindTexture(quad.texture);

            GL11.glColor4f(1.0F, 1.0F, 1.0F, quad.alpha);

            // Apply padding from CustomGlyphs
            float renderX = quad.x - quad.hPad;
            float renderY = quad.y - quad.vPad;

            float v0 = quad.flipV ? 1.0F : 0.0F;
            float v1 = quad.flipV ? 0.0F : 1.0F;

            if (quad.fitMode != CustomGlyphs.FitMode.NONE) {
                // drawGlyphContains-style: fit to font height (9), y--
                renderY -= 1.0F;
                float width = quad.fitMode == CustomGlyphs.FitMode.CONTAINS ? 9.0F
                    : (float) Math.ceil(MathUtils.calculateWidth(quad.glyphW, quad.glyphH, 9));
                float height = 9.0F;

                GL11.glBegin(GL11.GL_QUADS);
                GL11.glTexCoord2f(0.0F, v1);
                GL11.glVertex3f(renderX, renderY + height, 0.0F);
                GL11.glTexCoord2f(1.0F, v1);
                GL11.glVertex3f(renderX + width, renderY + height, 0.0F);
                GL11.glTexCoord2f(1.0F, v0);
                GL11.glVertex3f(renderX + width, renderY, 0.0F);
                GL11.glTexCoord2f(0.0F, v0);
                GL11.glVertex3f(renderX, renderY, 0.0F);
                GL11.glEnd();
            } else {
                // No fit: draw at original pixel size
                GL11.glBegin(GL11.GL_QUADS);
                GL11.glTexCoord2f(0.0F, v1);
                GL11.glVertex3f(renderX, renderY + quad.glyphH, 0.0F);
                GL11.glTexCoord2f(1.0F, v1);
                GL11.glVertex3f(renderX + quad.glyphW, renderY + quad.glyphH, 0.0F);
                GL11.glTexCoord2f(1.0F, v0);
                GL11.glVertex3f(renderX + quad.glyphW, renderY, 0.0F);
                GL11.glTexCoord2f(0.0F, v0);
                GL11.glVertex3f(renderX, renderY, 0.0F);
                GL11.glEnd();
            }
        }

        if (!prevBlend) {
            GL11.glDisable(GL11.GL_BLEND);
        }
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, prevTexture);
        GL20.glUseProgram(prevProgram);

        this.nt$glyphQuads.clear();
    }

}
