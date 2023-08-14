package io.github.cruciblemc.necrotempus.modules.mixin.mixins.minecraft;

import io.github.cruciblemc.necrotempus.utils.ColorUtils;
import net.minecraft.client.gui.FontRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.awt.*;

@Mixin(value = FontRenderer.class)
abstract class FontRendererMixin {

    @Unique
    public boolean is_hex = false;

    @Shadow
    private int textColor;

    @Shadow
    public abstract int getCharWidth(char p_78263_1_);

    @Shadow
    protected abstract void setColor(float r, float g, float b, float a);

    // These methods are used to calculate the correct size of string, parsing the hex characters.
    // Calculate the string width
    @Inject(method = "getStringWidth(Ljava/lang/String;)I", at = @At("HEAD"), cancellable = true)
    private void getHexStringWidth(String text, CallbackInfoReturnable<Integer> cir) {

        if (text == null) {
            cir.setReturnValue(0);
            return;
        }

        int i = 0;
        boolean flag = false;

        for (int character = 0; character < text.length(); ++character) {

            char charAt = text.charAt(character);
            int charWidth = this.getCharWidth(charAt);

            if (charWidth < 0 && character < text.length() - 1) {

                ++character;

                charAt = text.charAt(character);

                if (charAt == 120 && character < text.length() - 6)
                    character += 6;

                if (charAt != 108 && charAt != 76) {
                    if (charAt == 114 || charAt == 82) {
                        flag = false;
                    }
                } else {
                    flag = true;
                }

                charWidth = 0;
            }

            i += charWidth;

            if (flag && charWidth > 0) {
                ++i;
            }
        }

        cir.setReturnValue(i);

    }



    // These methods are used to inject the Hex Codes on Render
    // Reset hex state too
    @Inject(method = "resetStyles()V", at = @At(value = "HEAD"))
    private void resetStyles(CallbackInfo callbackInfo) {
        is_hex = false;
    }

    // Inject x on char list
    @ModifyConstant(method = "renderStringAtPos(Ljava/lang/String;Z)V", constant = @Constant(stringValue = "0123456789abcdefklmnor"))
    private static String injectHexChar(String constant) {
        return "0123456789abcdefklmnorx";
    }

    // Detect when is x and set color
    @Inject(method = "renderStringAtPos(Ljava/lang/String;Z)V", at = @At(value = "JUMP", ordinal = 3, shift = At.Shift.BEFORE), locals = LocalCapture.CAPTURE_FAILEXCEPTION)
    private void renderStringAtPos(String p_78255_1_, boolean p_78255_2_, CallbackInfo callbackInfo, int i, char c0, int j) {
        if (j == 22 && i + 8 < p_78255_1_.length()) {

            is_hex = true;
            i += 2;

            try {

                Color color = ColorUtils.decodeColor(p_78255_1_.substring(i, i + 6));

                if (p_78255_2_) {
                    int intColor = color.getRGB();
                    intColor = (intColor & 16579836) >> 2 | intColor & -16777216;
                    color = new Color(intColor);
                }

                setColor(color.getRed() / 255F, color.getGreen() / 255F, color.getBlue() / 255F, color.getAlpha() / 255F);

                this.textColor = color.getRGB();

            } catch (Exception ignored) {
            }

        }
    }

    // If is hex_mode, jump color chars
    @ModifyVariable(method = "renderStringAtPos(Ljava/lang/String;Z)V", ordinal = 0, at = @At(value = "JUMP", ordinal = 3, shift = At.Shift.BEFORE))
    private int calculateHexOffset(int i) {
        if (is_hex) {
            is_hex = false;
            return i + 6;
        } else {
            return i;
        }
    }

}
