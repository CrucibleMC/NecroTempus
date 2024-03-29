package io.github.cruciblemc.necrotempus.modules.features.title.client.render;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import io.github.cruciblemc.necrotempus.modules.features.title.client.ClientTitleManager;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

public class TitleDisplayListener {

    private static TitleDisplayListener instance;

    public static TitleDisplayListener getInstance() {
        return (instance != null) ? instance : new TitleDisplayListener();
    }

    private TitleDisplayListener() {
        instance = this;
    }

    @SubscribeEvent
    public void onRenderGameOverlayEvent(RenderGameOverlayEvent.Post event) {
        if (event.type == RenderGameOverlayEvent.ElementType.EXPERIENCE) {
            if (TitleGui.getInstance().shouldRender()) {
                TitleGui.getInstance().render(event.resolution);
            } else {
                if (ClientTitleManager.getCurrentTitle() != null)
                    ClientTitleManager.setCurrentTitle(null);
            }
        }
    }
}
