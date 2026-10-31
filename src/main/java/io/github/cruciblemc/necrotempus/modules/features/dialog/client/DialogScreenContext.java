package io.github.cruciblemc.necrotempus.modules.features.dialog.client;

import java.util.Map;
import java.util.UUID;

import net.minecraft.client.gui.GuiScreen;

import io.github.cruciblemc.necrotempus.api.dialog.Dialog;

/** Owns dialog lifecycle decisions for the screen currently being shown. */
public interface DialogScreenContext {

    DialogScreenContext GAMEPLAY = new GameplayDialogScreenContext();

    static boolean shouldPauseGame(DialogScreenContext context, boolean pauseRequested) {
        return context == GAMEPLAY && pauseRequested;
    }

    static boolean shouldPauseWarning(DialogScreenContext context) {
        return shouldPauseGame(context, true);
    }

    void tick();

    void drawBackground(GuiScreen screen, float partialTicks);

    void opened(UUID sessionId, int revision);

    boolean action(UUID sessionId, int revision, int index, Map<String, Object> values, Dialog.AfterAction afterAction);

    boolean cancel(UUID sessionId, int revision, Map<String, Object> values);

    boolean showConfirmation(UUID sessionId, int revision, GuiScreen confirmation);

    boolean showWarning(UUID sessionId, int revision);

    void restoreDialog(UUID sessionId, int revision);

    void dismissWaiting(UUID sessionId, int revision);

    void screenClosed(UUID sessionId, int revision);

    void leaveWorld();

    void returnFromWarning(GuiDialogWarningScreen warning);

    void warningClosed(GuiDialogWarningScreen warning);
}
