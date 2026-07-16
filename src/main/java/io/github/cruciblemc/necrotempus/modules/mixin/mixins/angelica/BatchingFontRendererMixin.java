package io.github.cruciblemc.necrotempus.modules.mixin.mixins.angelica;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
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
import io.github.cruciblemc.necrotempus.modules.features.glyphs.GlyphsRender;
import io.github.cruciblemc.necrotempus.modules.features.glyphs.compat.angelica.FontProviderGlyph;
import io.github.cruciblemc.necrotempus.modules.features.modernfonts.ModernFontEntry;
import io.github.cruciblemc.necrotempus.modules.features.modernfonts.ModernFontSupport;

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

    /**
     * Holds the most recent pushTexRect arguments for a glyph character.
     * pushTexRect is called multiple times per character (shadow copies + main).
     * We overwrite on each call so only the LAST one (the main quad) survives.
     * pushDrawCmd is used as the signal to commit this pending quad to the list.
     */
    @Unique
    private GlyphQuad nt$pendingGlyphQuad = null;

    @Unique
    private boolean nt$isModernFont = false;

    @Unique
    private ModernFontEntry nt$currentModernFontEntry = null;

    @Unique
    private ModernFontEntry nt$pendingModernFontEntry = null;

    @Unique
    private float nt$pendingModernFontX;

    @Unique
    private float nt$pendingModernFontY;

    @Unique
    private final List<ModernFontQuad> nt$modernFontQuads = new ArrayList<>();

    @Unique
    private static class ModernFontQuad {
        final float x, y;
        final ModernFontEntry entry;

        ModernFontQuad(float x, float y, ModernFontEntry entry) {
            this.x = x;
            this.y = y;
            this.entry = entry;
        }
    }

    @Unique
    private static class GlyphQuad {
        final float x, y;
        final float alpha;
        final CustomGlyphs glyph;
        final boolean flipV;

        GlyphQuad(float x, float y, float alpha, CustomGlyphs glyph, boolean flipV) {
            this.x = x;
            this.y = y;
            this.alpha = alpha;
            this.glyph = glyph;
            this.flipV = flipV;
        }
    }

    @Inject(method = "drawString(FFIZZLjava/lang/CharSequence;II)F", at = @At("HEAD"))
    private void nt$clearGlyphQuads(float anchorX, float anchorY, int color, boolean enableShadow, boolean unicodeFlag,
        CharSequence string, int stringOffset, int stringLength, CallbackInfoReturnable<Float> cir) {
        this.nt$glyphQuads.clear();
        this.nt$pendingGlyphQuad = null;
        this.nt$modernFontQuads.clear();
        this.nt$pendingModernFontEntry = null;
        this.nt$pendingModernFontX = 0;
        this.nt$pendingModernFontY = 0;
    }

    @Inject(method = "getCharWidthFine", at = @At("HEAD"), cancellable = true, remap = false)
    private void nt$glyphCharWidth(char chr, CallbackInfoReturnable<Float> cir) {

        if (!NecroTempusConfig.modernFonts) return;

        final CustomGlyphs glyph = GlyphsRegistry.getCandidate(chr);

        if (glyph != null) {
            cir.setReturnValue((float) glyph.getFinalCharacterWidth());
            return;
        }

        final ModernFontEntry entry = ModernFontSupport.getCandidate(chr);

        if (entry != null) {
            cir.setReturnValue((float) (entry.width + 1));
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
                this.nt$isModernFont = false;
                this.nt$currentModernFontEntry = null;
                FontProviderGlyph.cachedGlyphScale = glyph.getHeight() / 9.0F;
                return FontProviderGlyph.INSTANCE;
            }

            final ModernFontEntry entry = ModernFontSupport.getCandidate(chr);
            if (entry != null) {
                this.nt$isGlyph = false;
                this.nt$currentGlyph = null;
                this.nt$isModernFont = true;
                this.nt$currentModernFontEntry = entry;
                FontProviderGlyph.cachedGlyphScale = entry.height / 9.0F;
                return FontProviderGlyph.INSTANCE;
            }
        }

        this.nt$isGlyph = false;
        this.nt$currentGlyph = null;
        this.nt$isModernFont = false;
        this.nt$currentModernFontEntry = null;
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
            // Overwrite pending quad: pushTexRect fires once per copy (shadow + bold + main).
            // Only the LAST call (the main quad) survives after pushDrawCmd commits it.
            this.nt$pendingGlyphQuad = new GlyphQuad(x, y, alpha, this.nt$currentGlyph, flipV);
        } else if (this.nt$isModernFont && this.nt$currentModernFontEntry != null) {
            // Same pending quad pattern for ModernFontEntry: shadow copies are overwritten,
            // only the main quad survives when pushDrawCmd commits.
            this.nt$pendingModernFontEntry = this.nt$currentModernFontEntry;
            this.nt$pendingModernFontX = x;
            this.nt$pendingModernFontY = y;
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
            // Commit the pending main quad (survived all shadow-copy overwrites)
            if (this.nt$pendingGlyphQuad != null) {
                this.nt$glyphQuads.add(this.nt$pendingGlyphQuad);
                this.nt$pendingGlyphQuad = null;
            }
            // Skip the actual draw command — no vertex data was batched for glyph characters
        } else if (this.nt$isModernFont && texture != null) {
            // Commit the pending quad for ModernFontEntry
            if (this.nt$pendingModernFontEntry != null) {
                this.nt$modernFontQuads.add(new ModernFontQuad(this.nt$pendingModernFontX, this.nt$pendingModernFontY, this.nt$pendingModernFontEntry));
                this.nt$pendingModernFontEntry = null;
            }
        } else {
            pushDrawCmd(startIdx, idxCount, texture, isUnicode);
        }
    }

    @Inject(
        method = "drawString(FFIZZLjava/lang/CharSequence;II)F",
        at = @At("RETURN"))
    private void nt$renderGlyphs(float anchorX, float anchorY, int color, boolean enableShadow, boolean unicodeFlag,
        CharSequence string, int stringOffset, int stringLength, CallbackInfoReturnable<Float> cir) {

        if (this.nt$glyphQuads.isEmpty() && this.nt$modernFontQuads.isEmpty()) return;

        // endBatch() has already been called by drawString's finally block, so the
        // font shader is no longer active. Save/restore GL state for safety.
        int prevProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        int prevTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        boolean prevBlend = GL11.glGetBoolean(GL11.GL_BLEND);

        GL20.glUseProgram(0);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        TextureManager tm = Minecraft.getMinecraft().getTextureManager();

        // Render CustomGlyphs (image-based glyphs with padding/fit modes)
        for (GlyphQuad quad : this.nt$glyphQuads) {
            if (quad.glyph == null) continue;

            if (quad.flipV) {
                GL11.glMatrixMode(GL11.GL_TEXTURE);
                GL11.glPushMatrix();
                GL11.glTranslatef(0.0F, 1.0F, 0.0F);
                GL11.glScalef(1.0F, -1.0F, 1.0F);
            }

            GlyphsRender.renderGlyph(tm, quad.glyph, quad.x, quad.y + 3.0F, false, quad.alpha);

            if (quad.flipV) {
                GL11.glPopMatrix();
                GL11.glMatrixMode(GL11.GL_MODELVIEW);
            }
        }

        // Render ModernFontEntry (bitmap font atlas glyphs)
        if (!this.nt$modernFontQuads.isEmpty()) {
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            for (ModernFontQuad quad : this.nt$modernFontQuads) {
                if (quad.entry == null) continue;
                GlyphsRender.renderGlyph(tm, quad.entry, quad.x, quad.y, false);
            }
        }

        if (!prevBlend) {
            GL11.glDisable(GL11.GL_BLEND);
        }
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, prevTexture);
        GL20.glUseProgram(prevProgram);

        this.nt$glyphQuads.clear();
        this.nt$modernFontQuads.clear();
    }

}
