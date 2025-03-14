package io.github.cruciblemc.necrotempus.modules.mixin.mixins.minecraft;

import io.github.cruciblemc.necrotempus.modules.features.glyphs.CustomGlyphs;
import io.github.cruciblemc.necrotempus.modules.features.glyphs.GlyphsRegistry;
import io.github.cruciblemc.necrotempus.modules.features.glyphs.GlyphsRender;
import io.github.cruciblemc.necrotempus.modules.features.modernfonts.ModernFontEntry;
import io.github.cruciblemc.necrotempus.modules.features.modernfonts.ModernFontSupport;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FontRenderer.class)
public class FontRenderer2Mixin {

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

    @Unique
    public boolean is_render_modern = false;

    @Unique
    public boolean is_render_glyph = false;

    @Group(name = "necrotempus_fontRenderer_chatWidth", min = 1)
    @Inject(method = "Lnet/minecraft/client/gui/FontRenderer;getCharWidth(C)I", at = @At("HEAD"), cancellable = true, expect = 0)
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
    @Inject(method = "Lnet/minecraft/client/gui/FontRenderer;getCharWidthFloat(C)F", at = @At("HEAD"), cancellable = true, remap = false, expect = 0)
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

    @Inject(method = "Lnet/minecraft/client/gui/FontRenderer;renderCharAtPos(ICZ)F", at = @At("HEAD"), cancellable = true)
    public void renderChatAtPos(int index, char character, boolean shadow, CallbackInfoReturnable<Float> cfr) {

        CustomGlyphs customGlyphs = GlyphsRegistry.getCandidate(character);

        if (customGlyphs != null) {
            cfr.setReturnValue(GlyphsRender.renderGlyph(Minecraft.getMinecraft().getTextureManager(), customGlyphs, posX, posY, shadow, alpha));
            GL11.glColor4f(red, blue, green, alpha);
            return;
        }

        ModernFontEntry entry = ModernFontSupport.getCandidate(character);

        if (entry != null) {

            cfr.setReturnValue(GlyphsRender.renderGlyph(
                    Minecraft.getMinecraft().getTextureManager(),
                    entry,
                    posX,
                    posY,
                    shadow
            ));

        }

    }

    @Redirect(method = "Lnet/minecraft/client/gui/FontRenderer;renderStringAtPos(Ljava/lang/String;Z)V", at = @At(value = "INVOKE", target = "Ljava/lang/String;charAt(I)C", ordinal = 0))
    private char checkRenderModern(String string, int pos) {

        char character = string.charAt(pos);

        is_render_glyph = GlyphsRegistry.getCandidate(character) != null;
        is_render_modern = ModernFontSupport.hasCandidate(character);
        return character;

    }

    @Redirect(method = "Lnet/minecraft/client/gui/FontRenderer;renderStringAtPos(Ljava/lang/String;Z)V", at = @At(value = "INVOKE", target = "Ljava/lang/String;indexOf(I)I", ordinal = 1))
    private int j_charAt(String string, int character) {
        return is_render_glyph ? -1 : is_render_modern ? 0 : string.indexOf(character);
    }

}
