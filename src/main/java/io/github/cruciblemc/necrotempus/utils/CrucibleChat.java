package io.github.cruciblemc.necrotempus.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.IChatComponent;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class CrucibleChat {

    private CrucibleChat() {}

    public static void withSender(EntityPlayer player, Runnable action) {
        Player bukkitPlayer = player == null ? null : Bukkit.getPlayer(player.getUniqueID());
        String displayName = bukkitPlayer != null ? bukkitPlayer.getDisplayName()
            : player == null ? null
                : player.func_145748_c_()
                    .getUnformattedText();
        ChatSenderContext.withSender(player, displayName, action);
    }

    public static void sendComponents(EntityPlayerMP recipient, IChatComponent[] components) {
        try {
            Method sendMessage = recipient.getClass()
                .getMethod("sendMessage", IChatComponent[].class);
            sendMessage.invoke(recipient, new Object[] { components });
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException) throw (RuntimeException) cause;
            if (cause instanceof Error) throw (Error) cause;
            throw new RuntimeException(cause);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(
                "Crucible EntityPlayerMP.sendMessage(IChatComponent[]) is missing",
                exception);
        }
    }

}
