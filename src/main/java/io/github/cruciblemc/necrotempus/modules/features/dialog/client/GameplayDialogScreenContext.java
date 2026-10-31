package io.github.cruciblemc.necrotempus.modules.features.dialog.client;

import java.util.Map;
import java.util.UUID;

import net.minecraft.client.gui.GuiScreen;

import io.github.cruciblemc.necrotempus.api.dialog.Dialog;

final class GameplayDialogScreenContext implements DialogScreenContext {

    @Override
    public void tick() {}

    @Override
    public void drawBackground(GuiScreen screen, float partialTicks) {
        screen.drawDefaultBackground();
    }

    @Override
    public void opened(UUID sessionId, int revision) {
        ClientDialogManager.opened(sessionId, revision);
    }

    @Override
    public boolean action(UUID sessionId, int revision, int index, Map<String, Object> values,
        Dialog.AfterAction afterAction) {
        return ClientDialogManager.action(sessionId, revision, index, values, afterAction);
    }

    @Override
    public boolean cancel(UUID sessionId, int revision, Map<String, Object> values) {
        return ClientDialogManager.cancel(sessionId, revision, values);
    }

    @Override
    public boolean showConfirmation(UUID sessionId, int revision, GuiScreen confirmation) {
        return ClientDialogManager.showConfirmation(sessionId, revision, confirmation);
    }

    @Override
    public boolean showWarning(UUID sessionId, int revision) {
        return ClientDialogManager.showWarning(sessionId, revision);
    }

    @Override
    public void restoreDialog(UUID sessionId, int revision) {
        ClientDialogManager.restoreDialog(sessionId, revision);
    }

    @Override
    public void dismissWaiting(UUID sessionId, int revision) {
        ClientDialogManager.dismissWaiting(sessionId, revision);
    }

    @Override
    public void screenClosed(UUID sessionId, int revision) {
        ClientDialogManager.screenClosed(sessionId, revision);
    }

    @Override
    public void leaveWorld() {
        ClientDialogManager.leaveWorld();
    }

    @Override
    public void returnFromWarning(GuiDialogWarningScreen warning) {
        ClientDialogManager.returnFromWarning(warning);
    }

    @Override
    public void warningClosed(GuiDialogWarningScreen warning) {
        ClientDialogManager.warningClosed(warning);
    }
}
