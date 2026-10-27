package io.github.cruciblemc.necrotempus.modules.mixin.mixins.crucible;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.management.ServerConfigurationManager;
import net.minecraft.util.IChatComponent;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import io.github.cruciblemc.necrotempus.utils.CrucibleChat;

@Mixin(EntityPlayerMP.class)
public abstract class EntityPlayerMPChatMixin {

    @Redirect(
        method = "onDeath",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/management/ServerConfigurationManager;sendChatMsg(Lnet/minecraft/util/IChatComponent;)V"))
    private void necrotempus$sendDeathMessageWithVictim(ServerConfigurationManager manager, IChatComponent component) {

        EntityPlayerMP victim = (EntityPlayerMP) (Object) this;
        CrucibleChat.withSender(victim, () -> manager.sendChatMsg(component));
    }

    @Redirect(
        method = "onDeath",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/management/ServerConfigurationManager;sendMessage([Lnet/minecraft/util/IChatComponent;)V",
            remap = false),
        require = 0)
    private void necrotempus$sendCrucibleDeathMessage(ServerConfigurationManager manager, IChatComponent[] components) {

        EntityPlayerMP victim = (EntityPlayerMP) (Object) this;
        CrucibleChat.withSender(victim, () -> {
            for (IChatComponent component : components) {
                manager.sendChatMsgImpl(component, true);
            }
        });
    }
}
