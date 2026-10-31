package io.github.cruciblemc.necrotempus.modules.features.dialog.client;

import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class PreJoinPanorama {

    private static GuiMainMenu activeMenu;

    private PreJoinPanorama() {}

    public static void setActiveMenu(GuiMainMenu menu) {
        activeMenu = menu;
    }

    public static void tick() {
        if (activeMenu != null) activeMenu.updateScreen();
    }

    static void draw(GuiScreen screen, float partialTicks) {
        if (activeMenu == null) return;
        int menuWidth = activeMenu.width;
        int menuHeight = activeMenu.height;
        activeMenu.width = screen.width;
        activeMenu.height = screen.height;
        try {
            boolean alphaTestWasEnabled = GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
            GL11.glDisable(GL11.GL_ALPHA_TEST);
            try {
                ((GuiMainMenuAccess) activeMenu).necrotempus$renderSkybox(0, 0, partialTicks);
            } finally {
                if (alphaTestWasEnabled) GL11.glEnable(GL11.GL_ALPHA_TEST);
                else GL11.glDisable(GL11.GL_ALPHA_TEST);
            }
        } finally {
            activeMenu.width = menuWidth;
            activeMenu.height = menuHeight;
        }
    }
}
