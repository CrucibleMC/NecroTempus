package io.github.cruciblemc.necrotempus.modules.mixin.mixins.minecraft;

import java.util.Optional;
import java.util.UUID;

import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.util.IChatComponent;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.cruciblemc.necrotempus.modules.mixin.accessors.S02PacketChatSender;
import io.github.cruciblemc.necrotempus.utils.ChatSenderContext;

@Mixin(S02PacketChat.class)
public abstract class S02PacketChatMixin implements S02PacketChatSender {

    @Unique
    private UUID necrotempus$senderUuid;

    @Unique
    private String necrotempus$senderName;

    @Inject(method = "<init>(Lnet/minecraft/util/IChatComponent;Z)V", at = @At("TAIL"))
    private void necrotempus$captureSender(IChatComponent component, boolean chat, CallbackInfo ci) {
        Optional<UUID> senderUuid = ChatSenderContext.currentSenderUuid();
        necrotempus$senderUuid = senderUuid == null ? null : senderUuid.orElse(null);
        necrotempus$senderName = ChatSenderContext.currentSenderName();
    }

    @Inject(method = "readPacketData", at = @At("TAIL"))
    private void necrotempus$clearLocalMetadata(PacketBuffer data, CallbackInfo ci) {
        necrotempus$senderUuid = null;
        necrotempus$senderName = null;
    }

    @Override
    public UUID necrotempus$getSenderUuid() {
        return necrotempus$senderUuid;
    }

    @Override
    public void necrotempus$setSenderUuid(UUID senderUuid) {
        necrotempus$senderUuid = senderUuid;
    }

    @Override
    public String necrotempus$getSenderName() {
        return necrotempus$senderName;
    }

    @Override
    public void necrotempus$setSenderName(String senderName) {
        necrotempus$senderName = senderName;
    }
}
