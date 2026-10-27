package io.github.cruciblemc.necrotempus.modules.mixin.mixins.angelica;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gtnewhorizons.angelica.client.font.BatchingFontRenderer;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;

import io.github.cruciblemc.necrotempus.modules.features.chatheads.client.render.ChatHeadRenderer;
import io.github.cruciblemc.necrotempus.utils.ChatSenderContext;

@Mixin(value = BatchingFontRenderer.class, remap = false)
public abstract class ChatHeadsFontRendererMixin {

    @Inject(
        method = "drawString(FFIZZLjava/lang/CharSequence;II)F",
        at = @At(value = "INVOKE", target = "Ljava/lang/CharSequence;charAt(I)C", ordinal = 0))
    private void necrotempus$reserveHeadSpace(float x, float y, int color, boolean shadow, boolean unicode,
        CharSequence text, int offset, int length, CallbackInfoReturnable<Float> cir,
        @Local(name = "charIdx") int index, @Local(name = "curX") LocalFloatRef cursor) {
        if (ChatSenderContext.positionRenderingHead(index, cursor.get())) {
            cursor.set(cursor.get() + ChatHeadRenderer.CHAT_HEAD_WIDTH);
        }
    }
}
