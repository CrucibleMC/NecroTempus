package io.github.cruciblemc.necrotempus.modules.features.title.client;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import io.github.cruciblemc.necrotempus.modules.features.title.component.TimedTitle;
import lombok.Getter;
import lombok.Setter;

public class ClientTitleManager {

    private static ClientTitleManager instance;

    private ClientTitleManager() {
        instance = this;
    }

    public static ClientTitleManager getInstance() {
        return (instance != null) ? instance : new ClientTitleManager();
    }

    @Getter
    @Setter
    private static TimedTitle currentTitle;

    @SubscribeEvent
    public void onPlayerQuit(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        currentTitle = null;
    }

    public void clear(){
        currentTitle = null;
    }

}
