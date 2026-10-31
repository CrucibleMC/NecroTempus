package io.github.cruciblemc.necrotempus.modules.features.dialog.client;

import java.util.UUID;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;

public final class GuiWaitingForResponseScreen extends GuiScreen {

    private final UUID sessionId;
    private final int revision;
    private final DialogScreenContext context;
    private final boolean disconnectImmediately;
    private final DialogWaitTimer timer = new DialogWaitTimer();
    private GuiButton returnButton;
    private boolean returnEnabled;

    public GuiWaitingForResponseScreen(UUID sessionId, int revision) {
        this(sessionId, revision, DialogScreenContext.GAMEPLAY, false);
    }

    GuiWaitingForResponseScreen(UUID sessionId, int revision, DialogScreenContext context) {
        this(sessionId, revision, context, false);
    }

    GuiWaitingForResponseScreen(UUID sessionId, int revision, DialogScreenContext context,
        boolean disconnectImmediately) {
        this.sessionId = sessionId;
        this.revision = revision;
        this.context = context;
        this.disconnectImmediately = disconnectImmediately;
    }

    GuiWaitingForResponseScreen(DialogScreenContext context) {
        this(null, 0, context, true);
    }

    @Override
    public void initGui() {
        returnButton = new GuiButton(
            0,
            width / 2 - 100,
            height / 2 + 18,
            200,
            20,
            I18n.format("gui.waitingForResponse.button.inactive", 4));
        returnButton.visible = false;
        returnButton.enabled = false;
        buttonList.add(returnButton);
    }

    @Override
    public void updateScreen() {
        int elapsed = timer.tick();
        int countdown = DialogWaitTimer.countdownStep(elapsed);
        returnButton.visible = disconnectImmediately || DialogWaitTimer.isReturnVisible(elapsed);
        returnEnabled = disconnectImmediately || DialogWaitTimer.isReturnEnabled(elapsed);
        returnButton.displayString = disconnectImmediately ? I18n.format("menu.disconnect")
            : countdown == 5 ? I18n.format("gui.back")
                : I18n.format("gui.waitingForResponse.button.inactive", 5 - countdown);
        returnButton.enabled = returnEnabled;
        context.tick();
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button == returnButton && button.enabled) context.dismissWaiting(sessionId, revision);
    }

    @Override
    protected void keyTyped(char character, int keyCode) {
        if (keyCode == 1 && returnEnabled) context.dismissWaiting(sessionId, revision);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        context.drawBackground(this, partialTicks);
        drawCenteredString(
            fontRendererObj,
            I18n.format("gui.waitingForResponse.title"),
            width / 2,
            height / 2 - 24,
            0xFFFFFF);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public void onGuiClosed() {
        context.screenClosed(sessionId, revision);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
