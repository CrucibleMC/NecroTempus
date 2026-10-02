package io.github.cruciblemc.necrotempus.modules.mixin.mixins.minecraft;

import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiTextField;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.cruciblemc.necrotempus.utils.ChatFormattingUtils;

@Mixin(GuiChat.class)
public abstract class GuiChatMixin {

    @Shadow
    protected GuiTextField inputField;

    @Inject(method = "initGui", at = @At("TAIL"))
    private void necrotempus$translateDefaultChatFormatting(CallbackInfo ci) {
        this.necrotempus$translateChatInputFormatting();
    }

    @Inject(method = "keyTyped", at = @At("TAIL"))
    private void necrotempus$translateTypedChatFormatting(char typedChar, int keyCode, CallbackInfo ci) {
        this.necrotempus$translateChatInputFormatting();
    }

    @ModifyVariable(method = "func_146403_a", at = @At("HEAD"), argsOnly = true)
    private String necrotempus$restoreOutgoingChatFormatting(String text) {
        return ChatFormattingUtils.restoreAlternateColorCodes(text);
    }

    private void necrotempus$translateChatInputFormatting() {
        if (this.inputField == null) {
            return;
        }

        String text = this.inputField.getText();
        String formatted = ChatFormattingUtils.translateAlternateColorCodes(text);

        if (!text.equals(formatted)) {
            int cursorPosition = this.inputField.getCursorPosition();
            int selectionEnd = this.inputField.getSelectionEnd();
            this.inputField.setText(formatted);
            this.inputField.setCursorPosition(cursorPosition);
            this.inputField.setSelectionPos(selectionEnd);
        }
    }
}
