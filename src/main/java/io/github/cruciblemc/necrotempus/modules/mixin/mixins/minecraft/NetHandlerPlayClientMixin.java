package io.github.cruciblemc.necrotempus.modules.mixin.mixins.minecraft;

import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.play.server.S01PacketJoinGame;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.util.IChatComponent;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.cruciblemc.necrotempus.modules.features.dialog.client.PreJoinClientConnection;
import io.github.cruciblemc.necrotempus.modules.mixin.accessors.S02PacketChatSender;
import io.github.cruciblemc.necrotempus.utils.ChatSenderContext;

@Mixin(NetHandlerPlayClient.class)
public abstract class NetHandlerPlayClientMixin {

    @Inject(method = "handleJoinGame", at = @At("HEAD"), require = 1)
    private void necrotempus$finishPreJoin(S01PacketJoinGame packet, CallbackInfo ci) {
        PreJoinClientConnection.joined((NetHandlerPlayClient) (Object) this);
    }

    @Redirect(
        method = "handleChat",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiNewChat;printChatMessage(Lnet/minecraft/util/IChatComponent;)V"))
    private void necrotempus$printChatWithSenderUuid(GuiNewChat chatGui, IChatComponent message, S02PacketChat packet) {

        S02PacketChatSender senderPacket = (S02PacketChatSender) packet;
        ChatSenderContext.Snapshot previous = ChatSenderContext.snapshot();
        ChatSenderContext.setReceivedSender(
            senderPacket.necrotempus$getSenderUuid(),
            senderPacket.necrotempus$getSenderName(),
            senderPacket.necrotempus$getSenderDisplayName(),
            packet.func_148916_d());

        try {
            chatGui.printChatMessage(message);
        } finally {
            ChatSenderContext.restore(previous);
        }

    }
}
