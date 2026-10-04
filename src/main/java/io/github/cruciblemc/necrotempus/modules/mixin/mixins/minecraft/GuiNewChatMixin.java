package io.github.cruciblemc.necrotempus.modules.mixin.mixins.minecraft;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.MathHelper;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.cruciblemc.necrotempus.NecroTempusConfig;
import io.github.cruciblemc.necrotempus.modules.features.chatheads.client.render.ChatHeadRenderer;
import io.github.cruciblemc.necrotempus.utils.ChatFormattingUtils;
import io.github.cruciblemc.necrotempus.utils.ChatHead;
import io.github.cruciblemc.necrotempus.utils.ChatSenderContext;

@Mixin(GuiNewChat.class)
public abstract class GuiNewChatMixin {

    @Final
    @Shadow
    private Minecraft mc;

    @Final
    @Shadow
    private List<?> field_146253_i;

    @Final
    @Shadow
    private List<?> chatLines;

    @Shadow
    private int field_146250_j;

    @Unique
    private final Deque<ChatSenderContext.Snapshot> necrotempus$senderContexts = new ArrayDeque<>();

    @Shadow
    public abstract boolean getChatOpen();

    @Shadow
    public abstract float func_146244_h();

    @Shadow
    public abstract int func_146232_i();

    @Inject(method = "func_146237_a", at = @At("HEAD"))
    private void necrotempus$resolveHeads(IChatComponent component, int chatLineId, int updateCounter,
        boolean displayOnly, CallbackInfo ci) {
        necrotempus$senderContexts.push(ChatSenderContext.snapshot());
        List<ChatHead> heads = Collections.emptyList();
        if (displayOnly) {
            for (Object entry : chatLines) {
                ChatLine line = (ChatLine) entry;
                if (line.func_151461_a() == component) {
                    heads = ChatHeadRenderer.getMessageHeads(line);
                    break;
                }
            }
        } else {
            heads = ChatHeadRenderer.findIncomingHeads(
                component,
                ChatSenderContext.currentSenderUuid(),
                ChatSenderContext.currentSenderName(),
                ChatSenderContext.currentSenderDisplayName(),
                ChatSenderContext.isChatMessage());
        }
        ChatSenderContext.setHeads(heads);
    }

    @Inject(method = "func_146237_a", at = @At("RETURN"))
    private void necrotempus$restoreSenderContext(IChatComponent component, int chatLineId, int updateCounter,
        boolean displayOnly, CallbackInfo ci) {
        ChatSenderContext.restore(necrotempus$senderContexts.pop());
    }

    @Redirect(
        method = "func_146237_a",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/FontRenderer;getStringWidth(Ljava/lang/String;)I"))
    private int necrotempus$measureTextWithHeads(FontRenderer renderer, String text) {
        int width = renderer.getStringWidth(ChatFormattingUtils.translateAlternateColorCodes(text));
        if (!NecroTempusConfig.ChatHeadsEnabled) return width;
        return width + ChatSenderContext.wrappingHeadCount(ChatFormattingUtils.visibleCharacterCount(text))
            * ChatHeadRenderer.CHAT_HEAD_WIDTH;
    }

    @Redirect(
        method = "func_146237_a",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/FontRenderer;trimStringToWidth(Ljava/lang/String;IZ)Ljava/lang/String;"))
    private String necrotempus$trimTextWithHeads(FontRenderer renderer, String text, int width, boolean reverse) {
        String trimmed = renderer
            .trimStringToWidth(ChatFormattingUtils.translateAlternateColorCodes(text), width, reverse);
        int end = trimmed.length();
        while (end > 0 && necrotempus$measureTextWithHeads(renderer, trimmed.substring(0, end)) > width) {
            end--;
            if (end > 0 && trimmed.charAt(end - 1) == '\u00a7') end--;
        }
        return trimmed.substring(0, end);
    }

    @Redirect(
        method = "func_146237_a",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/util/ChatComponentText;appendSibling(Lnet/minecraft/util/IChatComponent;)Lnet/minecraft/util/IChatComponent;"))
    private IChatComponent necrotempus$advanceWrapping(ChatComponentText line, IChatComponent part) {
        ChatSenderContext.advanceWrapping(ChatFormattingUtils.visibleCharacterCount(part.getUnformattedText()));
        return line.appendSibling(part);
    }

    @Redirect(
        method = "drawChat",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/FontRenderer;drawStringWithShadow(Ljava/lang/String;III)I"))
    private int necrotempus$drawTextWithHeads(FontRenderer renderer, String text, int x, int y, int color) {
        text = ChatFormattingUtils.translateAlternateColorCodes(text);
        if (necrotempus$shouldNotDrawChatHead()) return renderer.drawStringWithShadow(text, x, y, color);
        List<ChatHead> heads = ChatHeadRenderer.getLineHeads(necrotempus$getChatLineForLineIndex(-(y + 8) / 9));
        int start = 0;
        int padding = 0;
        for (ChatHead head : heads) {
            int end = ChatHeadRenderer.getFormattedIndexForVisibleIndex(text, head.offset);
            int segmentX = x + renderer.getStringWidth(text.substring(0, start)) + padding;
            if (end > start) renderer.drawStringWithShadow(
                ChatHeadRenderer.getActiveFormatting(text, start) + text.substring(start, end),
                segmentX,
                y,
                color);
            int headX = x + renderer.getStringWidth(text.substring(0, end)) + padding;
            ChatHeadRenderer.drawChatHead(head.profile, headX, y, color >>> 24);
            padding += ChatHeadRenderer.CHAT_HEAD_WIDTH;
            start = end;
        }
        return renderer.drawStringWithShadow(
            ChatHeadRenderer.getActiveFormatting(text, start) + text.substring(start),
            x + renderer.getStringWidth(text.substring(0, start)) + padding,
            y,
            color);
    }

    @Unique
    private ChatLine necrotempus$getChatLineForLineIndex(int lineIndex) {
        if (lineIndex < 0 || lineIndex + field_146250_j >= field_146253_i.size()) return null;

        return (ChatLine) field_146253_i.get(lineIndex + field_146250_j);
    }

    @ModifyVariable(method = "func_146236_a", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int necrotempus$adjustChatHitTestingForHead(int mouseX, int mouseY) {
        if (necrotempus$shouldNotDrawChatHead() || !getChatOpen()) return mouseX;

        ScaledResolution resolution = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        int scaleFactor = resolution.getScaleFactor();
        float chatScale = func_146244_h();
        int lineX = MathHelper.floor_float((float) (mouseX / scaleFactor - 3) / chatScale);
        int lineY = MathHelper.floor_float((float) (mouseY / scaleFactor - 27) / chatScale);

        if (lineX < 0 || lineY < 0) return mouseX;

        int visibleLineCount = Math.min(func_146232_i(), field_146253_i.size());
        if (lineY >= mc.fontRenderer.FONT_HEIGHT * visibleLineCount + visibleLineCount) return mouseX;

        int lineIndex = lineY / mc.fontRenderer.FONT_HEIGHT + field_146250_j;
        if (lineIndex < 0 || lineIndex >= field_146253_i.size()) return mouseX;

        ChatLine chatLine = (ChatLine) field_146253_i.get(lineIndex);
        List<ChatHead> heads = ChatHeadRenderer.getLineHeads(chatLine);
        String text = ChatFormattingUtils.translateAlternateColorCodes(
            chatLine.func_151461_a()
                .getFormattedText());
        int padding = 0;
        for (ChatHead head : heads) {
            int index = ChatHeadRenderer.getFormattedIndexForVisibleIndex(text, head.offset);
            int headX = mc.fontRenderer.getStringWidth(text.substring(0, index)) + padding;
            if (lineX < headX) break;
            if (lineX < headX + ChatHeadRenderer.CHAT_HEAD_WIDTH) return 0;
            padding += ChatHeadRenderer.CHAT_HEAD_WIDTH;
        }
        return mouseX - Math.round(padding * chatScale * scaleFactor);
    }

    @Unique
    private boolean necrotempus$shouldNotDrawChatHead() {
        return !NecroTempusConfig.ChatHeadsEnabled || mc == null || mc.gameSettings == null || mc.thePlayer == null;
    }
}
