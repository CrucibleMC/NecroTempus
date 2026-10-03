package io.github.cruciblemc.necrotempus.modules.mixin.mixins.crucible;


import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.server.management.ServerConfigurationManager;
import net.minecraft.util.IChatComponent;

import org.bukkit.entity.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import io.github.cruciblemc.necrotempus.NecroTempus;
import io.github.cruciblemc.necrotempus.modules.features.packet.ChatHeadPacket;
import io.github.cruciblemc.necrotempus.modules.features.packet.NTClientPacketHandler;
import io.github.cruciblemc.necrotempus.modules.mixin.accessors.S02PacketChatSender;
import io.github.cruciblemc.necrotempus.utils.ChatSenderContext;
import io.github.cruciblemc.necrotempus.utils.CrucibleChat;
import io.netty.util.concurrent.GenericFutureListener;

@Mixin(NetHandlerPlayServer.class)
public abstract class NetHandlerPlayServerMixin {

    @Shadow
    public EntityPlayerMP playerEntity;

    @Redirect(
        method = "chat",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/player/EntityPlayerMP;sendMessage([Lnet/minecraft/util/IChatComponent;)V"),
        require = 0,
        remap = false)
    private void necrotempus$associateLazyChatMessage(EntityPlayerMP recipient, IChatComponent[] components) {
        ChatSenderContext.withSender(playerEntity, () -> CrucibleChat.sendComponents(recipient, components));
    }

    @Redirect(
        method = "chat",
        at = @At(
            value = "INVOKE",
            target = "Lorg/bukkit/entity/Player;sendMessage(Ljava/lang/String;)V",
            remap = false),
        require = 0,
        remap = false)
    private void necrotempus$associateFormattedChatMessage(Player recipient, String message) {
        ChatSenderContext.withSender(playerEntity, () -> recipient.sendMessage(message));
    }

    @Redirect(
        method = "onDisconnect",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/management/ServerConfigurationManager;sendMessage([Lnet/minecraft/util/IChatComponent;)V"),
        require = 0)
    private void necrotempus$associateQuitMessage(ServerConfigurationManager manager, IChatComponent[] components) {
        ChatSenderContext.withSender(
            playerEntity,
            () -> { for (IChatComponent component : components) manager.sendChatMsgImpl(component, true); });
    }

    @Redirect(
        method = "sendPacket",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/NetworkManager;scheduleOutboundPacket(Lnet/minecraft/network/Packet;[Lio/netty/util/concurrent/GenericFutureListener;)V"))
    private void necrotempus$sendNegotiatedChat(NetworkManager manager, Packet packet,
        GenericFutureListener<?>[] listeners) {
        if (packet instanceof S02PacketChat && NTClientPacketHandler.supportsChatHeads((NetHandlerPlayServer) (Object) this)) {
            S02PacketChat chatPacket = (S02PacketChat) packet;
            S02PacketChatSender metadata = (S02PacketChatSender) chatPacket;
            if (metadata.necrotempus$getSenderUuid() != null) {
                NecroTempus.DISPATCHER.sendTo(
                    new ChatHeadPacket(
                        metadata.necrotempus$getSenderUuid(),
                        metadata.necrotempus$getSenderName(),
                        chatPacket.func_148915_c(),
                        chatPacket.func_148916_d()),
                    playerEntity);
                return;
            }
        }

        manager.scheduleOutboundPacket(packet, listeners);
    }
}
