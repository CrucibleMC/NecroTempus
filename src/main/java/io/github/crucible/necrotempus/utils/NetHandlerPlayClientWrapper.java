package io.github.crucible.necrotempus.utils;

import net.minecraft.client.gui.GuiPlayerInfo;
import net.minecraft.client.network.NetHandlerPlayClient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings({"unchecked"})
public class NetHandlerPlayClientWrapper {

    public static final Comparator<GuiPlayerInfo> PLAYER_INFO_COMPARATOR = Comparator.comparing(elementA -> elementA.name);

    private final NetHandlerPlayClient internal;


    private NetHandlerPlayClientWrapper(NetHandlerPlayClient netHandlerPlayClient){
        internal = netHandlerPlayClient;
    }

    public static NetHandlerPlayClientWrapper of(NetHandlerPlayClient netHandlerPlayClient){
        return new NetHandlerPlayClientWrapper(netHandlerPlayClient);
    }

    public List<GuiPlayerInfo> getServerPlayers(){
        return (List<GuiPlayerInfo>) internal.playerInfoList;
    }

    public List<GuiPlayerInfo> getOrderedServerPlayers(){
        List<GuiPlayerInfo> newList = new ArrayList<>(getServerPlayers());
        newList.sort(PLAYER_INFO_COMPARATOR);
        return newList;
    }

    public int getServerMaxPlayers(){
        return internal.currentServerMaxPlayers;
    }

}
