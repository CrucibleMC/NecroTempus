package io.github.cruciblemc.necrotempus.modules.features.dialog.client;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.WorldClient;

import io.github.cruciblemc.necrotempus.NecroTempus;
import io.github.cruciblemc.necrotempus.api.dialog.Dialog;
import io.github.cruciblemc.necrotempus.modules.features.dialog.network.DialogPacket;
import io.github.cruciblemc.necrotempus.modules.features.dialog.network.DialogResponsePacket;

public final class ClientDialogManager {

    private static UUID sessionId;
    private static int revision;
    private static GuiScreen origin;
    private static GuiDialogScreen dialogScreen;
    private static GuiDialogWarningScreen warningScreen;
    private static GuiScreen localConfirmation;
    private static boolean changingScreen;

    private ClientDialogManager() {}

    public static void handle(DialogPacket packet) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (packet.getOperation() == DialogPacket.Operation.CLEAR) {
            if (!matches(packet)) return;
            if (minecraft.currentScreen == warningScreen) {
                warningScreen.updateReturnScreen(origin);
                resetDialogState();
            } else {
                close(minecraft);
            }
            return;
        }
        if (minecraft.thePlayer == null) {
            send(DialogResponsePacket.rejected(packet.getSessionId(), packet.getRevision()));
            return;
        }
        if (minecraft.thePlayer.openContainer != minecraft.thePlayer.inventoryContainer) {
            send(DialogResponsePacket.rejected(packet.getSessionId(), packet.getRevision()));
            return;
        }

        boolean deferredByWarning = warningScreen != null && minecraft.currentScreen == warningScreen;
        if (!isManagedScreen(minecraft.currentScreen)) origin = minecraft.currentScreen;
        sessionId = packet.getSessionId();
        revision = packet.getRevision();
        localConfirmation = null;
        dialogScreen = new GuiDialogScreen(packet.getDialog(), sessionId, revision);
        if (deferredByWarning) {
            warningScreen.updateReturnScreen(dialogScreen);
            opened(sessionId, revision);
            return;
        }
        showScreen(minecraft, dialogScreen);
        if (minecraft.currentScreen != dialogScreen) {
            send(DialogResponsePacket.rejected(sessionId, revision));
            resetState();
        }
    }

    public static void opened(UUID id, int screenRevision) {
        if (matches(id, screenRevision)) send(DialogResponsePacket.opened(id, screenRevision));
    }

    public static void action(UUID id, int screenRevision, int index) {
        action(id, screenRevision, index, Collections.emptyMap());
    }

    public static boolean action(UUID id, int screenRevision, int index, Map<String, Object> values) {
        return action(id, screenRevision, index, values, Dialog.AfterAction.CLOSE);
    }

    public static boolean action(UUID id, int screenRevision, int index, Map<String, Object> values,
        Dialog.AfterAction afterAction) {
        if (!matches(id, screenRevision) || afterAction == null) return false;
        send(DialogResponsePacket.action(id, screenRevision, index, values));
        Minecraft minecraft = Minecraft.getMinecraft();
        if (afterAction == Dialog.AfterAction.CLOSE) close(minecraft);
        else if (afterAction == Dialog.AfterAction.WAIT_FOR_RESPONSE) {
            showScreen(minecraft, new GuiWaitingForResponseScreen(id, screenRevision));
        } else if (minecraft.currentScreen == localConfirmation) {
            restoreDialog(minecraft, id, screenRevision);
        }
        return true;
    }

    public static void cancel(UUID id, int screenRevision) {
        cancel(id, screenRevision, Collections.emptyMap());
    }

    public static boolean cancel(UUID id, int screenRevision, Map<String, Object> values) {
        if (!matches(id, screenRevision)) return false;
        send(DialogResponsePacket.cancel(id, screenRevision, values));
        close(Minecraft.getMinecraft());
        return true;
    }

    public static boolean showConfirmation(UUID id, int screenRevision, GuiScreen confirmation) {
        if (!matches(id, screenRevision) || confirmation == null) return false;
        localConfirmation = confirmation;
        showScreen(Minecraft.getMinecraft(), confirmation);
        return true;
    }

    public static boolean showWarning(UUID id, int screenRevision) {
        if (!matches(id, screenRevision) || Minecraft.getMinecraft().currentScreen != dialogScreen) return false;
        warningScreen = new GuiDialogWarningScreen(dialogScreen);
        showScreen(Minecraft.getMinecraft(), warningScreen);
        return true;
    }

    static void returnFromWarning(GuiDialogWarningScreen warning) {
        if (warningScreen != warning) return;
        GuiScreen destination = warning.getReturnScreen();
        boolean returningToDialog = destination == dialogScreen && sessionId != null;
        warningScreen = null;
        if (!returningToDialog) resetState();
        showScreen(Minecraft.getMinecraft(), destination);
    }

    static void leaveWorld() {
        Minecraft minecraft = Minecraft.getMinecraft();
        resetState();
        if (minecraft.theWorld != null) {
            minecraft.theWorld.sendQuittingDisconnectingPacket();
            minecraft.loadWorld((WorldClient) null);
        }
        minecraft.displayGuiScreen(new GuiMainMenu());
    }

    static void warningClosed(GuiDialogWarningScreen warning) {
        if (!changingScreen && warningScreen == warning) resetState();
    }

    public static void restoreDialog(UUID id, int screenRevision) {
        if (matches(id, screenRevision)) restoreDialog(Minecraft.getMinecraft(), id, screenRevision);
    }

    public static void dismissWaiting(UUID id, int screenRevision) {
        if (!matches(id, screenRevision)) return;
        send(DialogResponsePacket.dismiss(id, screenRevision));
        close(Minecraft.getMinecraft());
    }

    public static void screenClosed(UUID id, int screenRevision) {
        if (!changingScreen && matches(id, screenRevision)) {
            send(DialogResponsePacket.closedByOtherGui(id, screenRevision));
            resetState();
        }
    }

    public static void reset() {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (!minecraft.func_152345_ab()) {
            minecraft.func_152344_a(ClientDialogManager::reset);
            return;
        }
        if (isManagedScreen(minecraft.currentScreen)) close(minecraft);
        else resetState();
    }

    private static void restoreDialog(Minecraft minecraft, UUID id, int screenRevision) {
        if (dialogScreen != null && matches(id, screenRevision)) showScreen(minecraft, dialogScreen);
    }

    private static void close(Minecraft minecraft) {
        if (isManagedScreen(minecraft.currentScreen)) showScreen(minecraft, origin);
        resetState();
    }

    private static void showScreen(Minecraft minecraft, GuiScreen screen) {
        changingScreen = true;
        try {
            minecraft.displayGuiScreen(screen);
        } finally {
            changingScreen = false;
        }
    }

    private static boolean isManagedScreen(GuiScreen screen) {
        return screen != null && (screen instanceof GuiDialogScreen || screen instanceof GuiWaitingForResponseScreen
            || screen == localConfirmation
            || screen == warningScreen);
    }

    private static void resetState() {
        resetDialogState();
        warningScreen = null;
        origin = null;
    }

    private static void resetDialogState() {
        sessionId = null;
        revision = 0;
        dialogScreen = null;
        localConfirmation = null;
    }

    private static boolean matches(DialogPacket packet) {
        return matches(packet.getSessionId(), packet.getRevision());
    }

    private static boolean matches(UUID id, int screenRevision) {
        return id != null && id.equals(sessionId) && screenRevision == revision;
    }

    private static void send(DialogResponsePacket packet) {
        NecroTempus.DISPATCHER.sendToServer(packet);
    }
}
