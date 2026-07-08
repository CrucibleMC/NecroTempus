package io.github.cruciblemc.necrotempus.modules.features.core;

import io.github.cruciblemc.necrotempus.modules.features.actionbar.client.ClientGuiIngameModifier;
import io.github.cruciblemc.necrotempus.modules.features.bossbar.client.ClientBossBarManager;
import io.github.cruciblemc.necrotempus.modules.features.playertab.client.ClientPlayerTabManager;
import io.github.cruciblemc.necrotempus.modules.features.title.client.ClientTitleManager;

public class ClientResetState {

    public static void resetRender() {
        ClientGuiIngameModifier.clearActionbar();
        ClientBossBarManager.clear();
        ClientPlayerTabManager.setPlayerTab(null);
        ClientTitleManager.getInstance().clear();
    }

}
