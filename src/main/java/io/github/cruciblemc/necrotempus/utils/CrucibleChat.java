package io.github.cruciblemc.necrotempus.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.IChatComponent;

public final class CrucibleChat {

    private CrucibleChat() {}

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
