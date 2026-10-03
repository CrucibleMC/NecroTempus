package io.github.cruciblemc.necrotempus.modules.mixin.mixins.crucible;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.util.IChatComponent;

import org.bukkit.entity.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import io.github.cruciblemc.necrotempus.utils.ChatSenderContext;
import io.github.cruciblemc.necrotempus.utils.CrucibleChat;

@Pseudo
@Mixin(targets = "net.minecraft.network.NetHandlerPlayServer$5")
public abstract class NetHandlerPlayServerChatWaitableMixin {

    @Shadow
    @Final
    private NetHandlerPlayServer this$0;

    @Redirect(
        method = "evaluate",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/player/EntityPlayerMP;sendMessage([Lnet/minecraft/util/IChatComponent;)V"),
        require = 0,
        remap = false)
    private void necrotempus$associateLazyChatMessage(EntityPlayerMP recipient, IChatComponent[] components) {
        ChatSenderContext
            .withSender(this$0.playerEntity, () -> CrucibleChat.sendComponents(recipient, components));
    }

    @Redirect(
        method = "evaluate",
        at = @At(
            value = "INVOKE",
            target = "Lorg/bukkit/entity/Player;sendMessage(Ljava/lang/String;)V",
            remap = false),
        require = 0,
        remap = false)
    private void necrotempus$associateFormattedChatMessage(Player recipient, String message) {
        ChatSenderContext.withSender(this$0.playerEntity, () -> recipient.sendMessage(message));
    }
}
