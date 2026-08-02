package io.github.cruciblemc.necrotempus.modules.mixin.mixins.minecraft;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiNewChat;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import io.github.cruciblemc.necrotempus.NecroTempusConfig;
import io.github.cruciblemc.necrotempus.modules.features.chatheads.client.render.ChatHeadRenderer;
import io.github.cruciblemc.necrotempus.utils.ChatFormattingUtils;

@Mixin(GuiNewChat.class)
public class GuiNewChatMixin {

    @Shadow
    private Minecraft mc;

    @Shadow
    private List field_146253_i;

    @Shadow
    private int field_146250_j;

    @Redirect(
        method = "drawChat",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;drawRect(IIIII)V", ordinal = 0))
    private void necrotempus$drawChatLineBackground(int left, int top, int right, int bottom, int color) {
        if (getSenderForLineIndex(-bottom / 9) != null) right += ChatHeadRenderer.CHAT_HEAD_WIDTH;

        Gui.drawRect(left, top, right, bottom, color);
    }

    @Redirect(
        method = "drawChat",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/FontRenderer;drawStringWithShadow(Ljava/lang/String;III)I"))
    private int necrotempus$drawStringWithChatHead(FontRenderer fontRenderer, String text, int x, int y, int color) {
        text = ChatFormattingUtils.translateAlternateColorCodes(text);
        ChatHeadRenderer.FoundSender sender = getCurrentSender(text, y);

        if (sender != null) {
            int formattedIndex = ChatHeadRenderer
                .getFormattedIndexForUnformattedIndex(text, sender.getUnformattedIndex());
            String prefix = text.substring(0, formattedIndex);
            String suffix = text.substring(formattedIndex);
            int prefixWidth = fontRenderer.getStringWidth(prefix);
            int alpha = color >>> 24;

            fontRenderer.drawStringWithShadow(prefix, x, y, color);
            ChatHeadRenderer.drawChatHead(sender.getProfile(), x + prefixWidth, y, alpha);

            String activeFormatting = ChatHeadRenderer.getActiveFormatting(prefix);
            return fontRenderer.drawStringWithShadow(
                activeFormatting + suffix,
                x + prefixWidth + ChatHeadRenderer.CHAT_HEAD_WIDTH,
                y,
                color);
        }

        return fontRenderer.drawStringWithShadow(text, x, y, color);
    }

    private ChatHeadRenderer.FoundSender getCurrentSender(String fallbackText, int textY) {
        if (!shouldDrawChatHead()) return null;

        int lineIndex = -(textY + 8) / 9;
        ChatHeadRenderer.FoundSender sender = getSenderForLineIndex(lineIndex);

        if (sender != null) return sender;

        return ChatHeadRenderer.findSender(fallbackText);
    }

    private ChatHeadRenderer.FoundSender getSenderForLineIndex(int lineIndex) {
        if (!shouldDrawChatHead()) return null;

        if (lineIndex >= 0 && lineIndex + field_146250_j < field_146253_i.size()) {
            ChatLine chatLine = (ChatLine) field_146253_i.get(lineIndex + field_146250_j);

            if (chatLine != null) return ChatHeadRenderer.findSender(chatLine);
        }

        return null;
    }

    private boolean shouldDrawChatHead() {
        return NecroTempusConfig.ChatHeadsEnabled && mc != null && mc.gameSettings != null && mc.thePlayer != null;
    }
}
