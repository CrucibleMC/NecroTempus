package io.github.crucible.necrotempus.utils;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

import java.util.List;
import java.util.UUID;

public class ServerUtils {

    public static List<EntityPlayerMP> getAllPlayers(){
        return MinecraftServer.getServer().getConfigurationManager().playerEntityList;
    }

    public static EntityPlayerMP getPlayer(UUID uuid){
        return getAllPlayers()
                .stream()
                .filter(entityPlayerMP -> entityPlayerMP.getUniqueID().equals(uuid))
                .findFirst()
                .orElse(null);
    }

}
