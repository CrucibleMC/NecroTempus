package io.github.cruciblemc.necrotempus.utils;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class ServerUtils {

    @SuppressWarnings("unchecked")
    public static List<EntityPlayerMP> getAllPlayers() {
        return MinecraftServer.getServer().getConfigurationManager().playerEntityList;
    }

    public static EntityPlayerMP getPlayer(UUID uuid) {
        return getAllPlayers()
                .stream()
                .filter(entityPlayerMP -> entityPlayerMP.getUniqueID().equals(uuid))
                .findFirst()
                .orElse(null);
    }

    @SuppressWarnings("unchecked")
    public static <T> T lazyAccess(boolean accessible, String method, Object target, Object... params) {
        try {
            Method mt = target.getClass().getDeclaredMethod(method, params.getClass());
            if (!accessible)
                mt.setAccessible(true);
            T element = (T) mt.invoke(target, params);
            if (!accessible)
                mt.setAccessible(false);
            return element;
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException ignored) {
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    public static <T> T lazyAccess(boolean accessible, String method, Object target) {
        try {
            System.out.println(Arrays.toString(target.getClass().getMethods()));
            Method mt = target.getClass().getDeclaredMethod(method);
            if (!accessible)
                mt.setAccessible(true);
            T element = (T) mt.invoke(target);
            if (!accessible)
                mt.setAccessible(false);
            return element;
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException ignored) {
        }

        return null;
    }

}
