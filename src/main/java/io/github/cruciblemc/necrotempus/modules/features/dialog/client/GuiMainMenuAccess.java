package io.github.cruciblemc.necrotempus.modules.features.dialog.client;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public interface GuiMainMenuAccess {

    void necrotempus$renderSkybox(int mouseX, int mouseY, float partialTicks);
}
