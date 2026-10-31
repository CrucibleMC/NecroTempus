package io.github.cruciblemc.necrotempus.modules.features.dialog.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;

public final class GuiDialogWarningScreen extends GuiScreen {

    private GuiScreen returnScreen;
    private final DialogScreenContext context;

    GuiDialogWarningScreen(GuiScreen returnScreen) {
        this(returnScreen, DialogScreenContext.GAMEPLAY);
    }

    GuiDialogWarningScreen(GuiScreen returnScreen, DialogScreenContext context) {
        this.returnScreen = returnScreen;
        this.context = context;
    }

    @Override
    public void initGui() {
        buttonList.clear();
        buttonList.add(
            new GuiButton(
                0,
                width / 2 - 155,
                height / 2 + 24,
                150,
                20,
                I18n.format(
                    Minecraft.getMinecraft()
                        .isIntegratedServerRunning() ? "menu.returnToMenu" : "menu.disconnect")));
        buttonList.add(new GuiButton(1, width / 2 + 5, height / 2 + 24, 150, 20, I18n.format("gui.back")));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 0) {
            context.leaveWorld();
        } else if (button.id == 1) {
            context.returnFromWarning(this);
        }
    }

    @Override
    protected void keyTyped(char character, int keyCode) {
        if (keyCode == 1) context.returnFromWarning(this);
        else super.keyTyped(character, keyCode);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        context.drawBackground(this, partialTicks);
        drawCenteredString(
            fontRendererObj,
            I18n.format("gui.necrotempus.dialogs.warning.title"),
            width / 2,
            height / 2 - 64,
            0xFFFFFF);
        fontRendererObj.drawSplitString(
            I18n.format("gui.necrotempus.dialogs.warning.contents"),
            width / 2 - 150,
            height / 2 - 42,
            300,
            0xFFFFFF);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public void updateScreen() {
        context.tick();
    }

    @Override
    public void onGuiClosed() {
        context.warningClosed(this);
    }

    void updateReturnScreen(GuiScreen screen) {
        returnScreen = screen;
    }

    GuiScreen getReturnScreen() {
        return returnScreen;
    }

    @Override
    public boolean doesGuiPauseGame() {
        return DialogScreenContext.shouldPauseWarning(context);
    }
}
