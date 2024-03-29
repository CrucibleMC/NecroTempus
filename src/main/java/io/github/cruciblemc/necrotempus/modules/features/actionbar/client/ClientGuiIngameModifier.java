package io.github.cruciblemc.necrotempus.modules.features.actionbar.client;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import io.github.cruciblemc.necrotempus.api.actionbar.ActionBar;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.GuiIngameForge;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

public class ClientGuiIngameModifier {

    private static ClientGuiIngameModifier instance;

    public static ClientGuiIngameModifier getInstance() {
        return instance != null ? instance : new ClientGuiIngameModifier();
    }

    private ClientGuiIngameModifier() {
        instance = this;
    }

    private static GuiIngameForge guiIngameForge;


    @SubscribeEvent
    public void onRender(RenderGameOverlayEvent.Pre evt) {
        if (guiIngameForge == null) {
            guiIngameForge = (GuiIngameForge) Minecraft.getMinecraft().ingameGUI;
        }
    }

    public static void renderActionBar(ActionBar actionBar) {
        if (guiIngameForge != null) {
            guiIngameForge.func_110326_a(actionBar.getText().getUnformattedText(), false);
            guiIngameForge.recordPlayingUpFor = actionBar.getTime();
        }
    }

    public static void clearActionbar() {
        if (guiIngameForge != null) {
            guiIngameForge.recordPlayingUpFor = 0;
        }
    }

}
