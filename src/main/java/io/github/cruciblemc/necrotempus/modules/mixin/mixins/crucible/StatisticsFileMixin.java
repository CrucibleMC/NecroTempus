package io.github.cruciblemc.necrotempus.modules.mixin.mixins.crucible;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.management.ServerConfigurationManager;
import net.minecraft.stats.StatBase;
import net.minecraft.stats.StatisticsFile;
import net.minecraft.util.IChatComponent;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import io.github.cruciblemc.necrotempus.utils.CrucibleChat;

@Mixin(StatisticsFile.class)
public abstract class StatisticsFileMixin {

    @Redirect(
        method = "func_150873_a",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/management/ServerConfigurationManager;sendChatMsg(Lnet/minecraft/util/IChatComponent;)V"))
    private void necrotempus$associateAchievement(ServerConfigurationManager manager, IChatComponent component,
        EntityPlayer player, StatBase stat, int amount) {
        CrucibleChat.withSender(player, () -> manager.sendChatMsg(component));
    }
}
