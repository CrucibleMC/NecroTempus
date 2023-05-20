package io.github.crucible.necrotempus;

import io.github.crucible.necrotempus.modules.playertab.PlayerTabManager;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class NecroTempusAPI {

    private PlayerTabManager playerTabManager;

    public PlayerTabManager getPlayerTabManager() {
        return playerTabManager;
    }

    public boolean isCustomServerTabEnabled(){
        return playerTabManager != null;
    }

}
