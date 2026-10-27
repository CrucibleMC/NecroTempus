package io.github.cruciblemc.necrotempus.modules.mixin.mixins.minecraft;

import net.minecraft.client.gui.ChatLine;
import net.minecraft.util.IChatComponent;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.cruciblemc.necrotempus.modules.features.chatheads.client.render.ChatHeadRenderer;
import io.github.cruciblemc.necrotempus.utils.ChatSenderContext;

@Mixin(ChatLine.class)
public abstract class ChatLineMixin {

    @Inject(method = "<init>", at = @At("TAIL"))
    private void necrotempus$rememberSenderUuid(int updateCounter, IChatComponent component, int chatLineId,
        CallbackInfo ci) {

        if (!ChatSenderContext.currentHeads()
            .isEmpty()) {
            ChatHeadRenderer.rememberHeads(
                (ChatLine) (Object) this,
                ChatSenderContext.currentHeads(),
                ChatSenderContext.getLineHeads(component));
        }

    }

}
