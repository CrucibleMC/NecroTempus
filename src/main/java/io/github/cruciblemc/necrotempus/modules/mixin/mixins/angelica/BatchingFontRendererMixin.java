package io.github.cruciblemc.necrotempus.modules.mixin.mixins.angelica;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gtnewhorizons.angelica.client.font.BatchingFontRenderer;
import com.gtnewhorizons.angelica.client.font.FontProvider;
import com.gtnewhorizons.angelica.client.font.FontStrategist;
import com.gtnewhorizons.angelica.glsm.GLStateManager;

import io.github.cruciblemc.necrotempus.NecroTempusConfig;
import io.github.cruciblemc.necrotempus.modules.features.glyphs.CustomGlyphs;
import io.github.cruciblemc.necrotempus.modules.features.glyphs.GlyphsRegistry;
import io.github.cruciblemc.necrotempus.modules.features.glyphs.compat.angelica.FontProviderGlyph;
import io.github.cruciblemc.necrotempus.utils.MathUtils;

@Mixin(value = BatchingFontRenderer.class, remap = false)
public abstract class BatchingFontRendererMixin {

    @Unique
    private boolean nt$isGlyph = false;

    @Unique
    private char nt$glyphChr;

    @Unique
    private final List<float[]> nt$glyphRects = new ArrayList<>();

    @Unique
    private final List<ResourceLocation> nt$glyphTextures = new ArrayList<>();

    @Invoker(remap = false)
    public abstract void invokePushTexRect(float x, float y, float w, float h, float itOff, int rgba, float uStart,
        float vStart, float uSz, float vSz, boolean flipV);

    @Invoker(remap = false)
    public abstract void invokePushDrawCmd(int startIdx, int idxCount, ResourceLocation texture, boolean isUnicode);

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
                this.nt$glyphChr = chr;
                return FontProviderGlyph.INSTANCE;
            }
        }

        this.nt$isGlyph = false;
        return FontStrategist.getFontProvider(me, chr, customFontEnabled, forceUnicode);

    }

    @Redirect(
        method = "drawString",
        at = @At(
            value = "INVOKE",
            target = "Lcom/gtnewhorizons/angelica/client/font/BatchingFontRenderer;pushTexRect(FFFFFIFFFFZ)V"))
    private void nt$redirectPushTexRect(BatchingFontRenderer instance, float x, float y, float w, float h, float itOff,
        int rgba, float uStart, float vStart, float uSz, float vSz, boolean flipV) {

        if (!this.nt$isGlyph) {
            this.invokePushTexRect(x, y, w, h, itOff, rgba, uStart, vStart, uSz, vSz, flipV);
            return;
        }

        final CustomGlyphs g = GlyphsRegistry.getCandidate(this.nt$glyphChr);
        if (g == null) {
            this.invokePushTexRect(x, y, w, h, itOff, rgba, uStart, vStart, uSz, vSz, flipV);
            return;
        }

        // Defer glyph rendering until after Angelica's batch completes
        final float rx = x + (-g.getHorizontalPadding());
        final float ry = y + (-g.getVerticalPadding());
        final float rw;
        final float rh;

        switch (g.getFitMode()) {
            case CONTAINS:
                rw = 9;
                rh = 9;
                break;
            case VERTICALLY:
                rw = (float) Math.ceil(MathUtils.calculateWidth(g.getWidth(), g.getHeight(), 9));
                rh = 9;
                break;
            default:
                rw = g.getWidth();
                rh = g.getHeight();
                break;
        }

        this.nt$glyphRects.add(new float[] { rx, ry, rw, rh });
        this.nt$glyphTextures.add(g.getResource());

        // Push zero-size quad so Angelica's index batching doesn't break
        this.invokePushTexRect(0, 0, 0, 0, 0, 0, 0f, 0f, 0f, 0f, false);

    }

    @Redirect(
        method = "drawString",
        at = @At(
            value = "INVOKE",
            target = "Lcom/gtnewhorizons/angelica/client/font/BatchingFontRenderer;pushDrawCmd(IILnet/minecraft/util/ResourceLocation;Z)V"))
    private void nt$redirectPushDrawCmd(BatchingFontRenderer instance, int startIdx, int idxCount,
        ResourceLocation texture, boolean isUnicode) {

        if (this.nt$isGlyph) {
            this.nt$isGlyph = false;
            return;
        }

        this.invokePushDrawCmd(startIdx, idxCount, texture, isUnicode);

    }

    @Inject(method = "drawString", at = @At("RETURN"))
    private void nt$renderDeferredGlyphs(CallbackInfoReturnable<Float> cir) {
        if (this.nt$glyphRects.isEmpty()) return;

        final int prevProgram = GLStateManager.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        GLStateManager.glUseProgram(0);

        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glEnable(GL11.GL_TEXTURE_2D);

        for (int i = 0; i < this.nt$glyphRects.size(); i++) {
            final float[] rect = this.nt$glyphRects.get(i);
            final ResourceLocation tex = this.nt$glyphTextures.get(i);

            Minecraft.getMinecraft().getTextureManager().bindTexture(tex);

            final Tessellator ts = Tessellator.instance;
            ts.startDrawingQuads();
            ts.addVertexWithUV(rect[0], rect[1] + rect[3], 0.0, 0.0, 1.0);
            ts.addVertexWithUV(rect[0] + rect[2], rect[1] + rect[3], 0.0, 1.0, 1.0);
            ts.addVertexWithUV(rect[0] + rect[2], rect[1], 0.0, 1.0, 0.0);
            ts.addVertexWithUV(rect[0], rect[1], 0.0, 0.0, 0.0);
            ts.draw();
        }

        GLStateManager.glUseProgram(prevProgram);

        this.nt$glyphRects.clear();
        this.nt$glyphTextures.clear();

    }

}
