package io.github.cruciblemc.necrotempus.modules.mixin.mixins.minecraft;

import io.github.cruciblemc.necrotempus.modules.features.glyphs.CustomGlyphs;
import io.github.cruciblemc.necrotempus.modules.features.glyphs.GlyphsRegistry;
import io.github.cruciblemc.necrotempus.modules.features.glyphs.GlyphsRender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
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

    @Inject(method = "Lnet/minecraft/client/gui/FontRenderer;getCharWidth(C)I", at = @At("HEAD"), cancellable = true)
    public void getCharWidth(char character, CallbackInfoReturnable<Integer> cir) {
        CustomGlyphs customGlyphs = GlyphsRegistry.getCandidate(character);
        if (customGlyphs != null) {
                cir.setReturnValue(customGlyphs.getFinalCharacterWidth());
        }
    }

    @Inject(method = "Lnet/minecraft/client/gui/FontRenderer;renderCharAtPos(ICZ)F", at = @At("HEAD"), cancellable = true)
    public void renderChatAtPos(int index, char character, boolean shadow, CallbackInfoReturnable<Float> cfr) {
        CustomGlyphs customGlyphs = GlyphsRegistry.getCandidate(character);
        if (customGlyphs != null) {
            cfr.setReturnValue(GlyphsRender.renderGlyph(Minecraft.getMinecraft().getTextureManager(), customGlyphs, posX, posY, shadow));
            GL11.glColor4f(red, blue, green, alpha);
        }
    }

}
