package io.github.cruciblemc.necrotempus.modules.mixin.mixins.crucible;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.server.management.ServerConfigurationManager;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import io.github.cruciblemc.necrotempus.modules.mixin.accessors.S02PacketChatSender;

@Mixin(ServerConfigurationManager.class)
public abstract class ServerConfigurationManagerMixin {

    @Redirect(
        method = "playerLoggedIn",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/management/ServerConfigurationManager;sendPacketToAllPlayers(Lnet/minecraft/network/Packet;)V"))
    private void necrotempus$associateJoinMessage(ServerConfigurationManager manager, Packet packet,
        EntityPlayerMP joiningPlayer) {
        if (packet instanceof S02PacketChat) {
            S02PacketChatSender metadata = (S02PacketChatSender) packet;
            metadata.necrotempus$setSenderUuid(joiningPlayer.getUniqueID());
            metadata.necrotempus$setSenderName(joiningPlayer.getCommandSenderName());
        }
        manager.sendPacketToAllPlayers(packet);
    }
}
