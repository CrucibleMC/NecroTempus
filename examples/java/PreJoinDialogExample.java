package example;

import net.minecraft.util.ChatComponentText;

import io.github.cruciblemc.necrotempus.api.dialog.Dialog;
import io.github.cruciblemc.necrotempus.api.dialog.PreJoinConnection;
import io.github.cruciblemc.necrotempus.api.dialog.PreJoinDialogManager;

/** Minimal server-side integration example; call enable/disable from the server thread. */
public final class PreJoinDialogExample {

    private static AutoCloseable registration;

    private PreJoinDialogExample() {}

    public static void enable() {
        if (registration != null) return;
        registration = PreJoinDialogManager.register(PreJoinDialogExample::onJoinAttempt);
    }

    private static void onJoinAttempt(PreJoinConnection connection) {
        Dialog dialog = Dialog.confirmation(new ChatComponentText("Example server"))
            .body(new Dialog.TextBody(new ChatComponentText("Continue to this server?")))
            .afterAction(Dialog.AfterAction.WAIT_FOR_RESPONSE)
            .action(new Dialog.ActionButton(new ChatComponentText("Continue"), Dialog.Action.none()))
            .action(new Dialog.ActionButton(new ChatComponentText("Disconnect"), Dialog.Action.none()))
            .build();

        connection.show(dialog, (current, response) -> {
            if (!response.isCancelled() && response.getActionIndex() == 0) current.allow();
            else current.disconnect(new ChatComponentText("Connection declined"));
        });
    }

    public static void disable() throws Exception {
        AutoCloseable current = registration;
        registration = null;
        if (current != null) current.close();
    }
}
