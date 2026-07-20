package io.github.cruciblemc.necrotempus.modules.mixin.mixins.minecraft;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.texture.TextureManager;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.github.cruciblemc.necrotempus.modules.features.glyphs.CustomGlyphs;
import io.github.cruciblemc.necrotempus.modules.features.glyphs.GlyphsRegistry;
import io.github.cruciblemc.necrotempus.modules.features.glyphs.GlyphsRender;
import io.github.cruciblemc.necrotempus.modules.features.modernfonts.ModernFontEntry;
import io.github.cruciblemc.necrotempus.modules.features.modernfonts.ModernFontSupport;
import io.github.cruciblemc.necrotempus.utils.ColorUtils;

@Mixin(FontRenderer.class)
public class FontRendererMixin {

    @Shadow
    protected float posX;

    @Shadow
    protected float posY;

    @Shadow
    private float red;

    @Shadow
    private float blue;

    @Shadow
    private float green;

    @Shadow
    private float alpha;

    @Shadow
    private int textColor;

    @Final
    @Shadow
    private TextureManager renderEngine;

    @Unique
    private boolean nt$isRenderModern = false;

    @Unique
    private boolean nt$isRenderGlyph = false;

    @Unique
    private boolean nt$isRenderingStringShadow = false;

    @Group(name = "necrotempus_fontRenderer_chatWidth", min = 1)
    @Inject(
        method = "Lnet/minecraft/client/gui/FontRenderer;getCharWidth(C)I",
        at = @At("HEAD"),
        cancellable = true,
        expect = 0)
    public void getCharWidth(char character, CallbackInfoReturnable<Integer> cir) {

        if (character == 167) {
            cir.setReturnValue(-1);
            return;
        }

        if (character == 32) {
            cir.setReturnValue(4);
            return;
        }

        CustomGlyphs customGlyphs = GlyphsRegistry.getCandidate(character);
        if (customGlyphs != null) {
            cir.setReturnValue(customGlyphs.getFinalCharacterWidth());
            return;
        }

        ModernFontEntry entry = ModernFontSupport.getCandidate(character);
        if (entry != null) {
            cir.setReturnValue(entry.width + 1);
        }

    }

    @Group(name = "necrotempus_fontRenderer_chatWidth", min = 1)
    @Inject(
        method = "Lnet/minecraft/client/gui/FontRenderer;getCharWidthFloat(C)F",
        at = @At("HEAD"),
        cancellable = true,
        remap = false,
        expect = 0)
    public void getCharWidthFloat(char character, CallbackInfoReturnable<Float> cir) {

        if (character == 167) {
            cir.setReturnValue(-1F);
            return;
        }

        if (character == 32) {
            cir.setReturnValue(4F);
            return;
        }

        CustomGlyphs customGlyphs = GlyphsRegistry.getCandidate(character);

        if (customGlyphs != null) {
            cir.setReturnValue((float) customGlyphs.getFinalCharacterWidth());
            return;
        }

        ModernFontEntry entry = ModernFontSupport.getCandidate(character);
        if (entry != null) {
            cir.setReturnValue((float) entry.width + 1);
        }

    }

    @Inject(
        method = "Lnet/minecraft/client/gui/FontRenderer;renderCharAtPos(ICZ)F",
        at = @At("HEAD"),
        cancellable = true)
    public void renderChatAtPos(int index, char character, boolean shadow, CallbackInfoReturnable<Float> cfr) {

        CustomGlyphs customGlyphs = GlyphsRegistry.getCandidate(character);

        if (customGlyphs != null) {

            if (ColorUtils.isShadow(textColor)) shadow = true;

            if (!shadow && nt$isRenderingStringShadow) shadow = true;
            else if (shadow && !nt$isRenderingStringShadow) {
                shadow = false;
            }

            cfr.setReturnValue(GlyphsRender.renderGlyph(renderEngine, customGlyphs, posX, posY, shadow, alpha));
            // NOTE: The channel order (red, blue, green) is NOT a typo — MCP 1.7.10 misnames
            // FontRenderer.blue and FontRenderer.green. Red is red, blue is actually green, green is actually blue.
            GL11.glColor4f(red, blue, green, alpha);
            return;
        }

        ModernFontEntry entry = ModernFontSupport.getCandidate(character);

        if (entry != null) {
            float glyphX = shadow ? posX - 1.0F : posX;
            cfr.setReturnValue(
                GlyphsRender.renderGlyph(renderEngine, entry, glyphX, posY, 0.0F, false, red, blue, green, alpha));
        }

    }

    @Redirect(
        method = "Lnet/minecraft/client/gui/FontRenderer;renderStringAtPos(Ljava/lang/String;Z)V",
        at = @At(value = "INVOKE", target = "Ljava/lang/String;charAt(I)C", ordinal = 0))
    private char checkRenderModern(String string, int pos) {

        char character = string.charAt(pos);

        nt$isRenderGlyph = GlyphsRegistry.getCandidate(character) != null;
        nt$isRenderModern = ModernFontSupport.hasCandidate(character);

        return character;

    }

    @Redirect(
        method = "Lnet/minecraft/client/gui/FontRenderer;renderStringAtPos(Ljava/lang/String;Z)V",
        at = @At(value = "INVOKE", target = "Ljava/lang/String;indexOf(I)I", ordinal = 1))
    private int j_charAt(String string, int character) {
        return nt$isRenderGlyph ? -1 : nt$isRenderModern ? 0 : string.indexOf(character);
    }

    @Inject(
        method = "Lnet/minecraft/client/gui/FontRenderer;drawString(Ljava/lang/String;IIIZ)I",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/FontRenderer;renderString(Ljava/lang/String;IIIZ)I",
            ordinal = 0))
    private void onDrawWithShadowA(String text, int x, int y, int color, boolean dropShadow,
        CallbackInfoReturnable<Integer> callbackInfo) {
        nt$isRenderingStringShadow = true;
    }

    @Inject(
        method = "Lnet/minecraft/client/gui/FontRenderer;drawString(Ljava/lang/String;IIIZ)I",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/FontRenderer;renderString(Ljava/lang/String;IIIZ)I",
            ordinal = 1))
    private void onDrawWithShadowB(String text, int x, int y, int color, boolean dropShadow,
        CallbackInfoReturnable<Integer> callbackInfo) {
        nt$isRenderingStringShadow = false;
    }

}
