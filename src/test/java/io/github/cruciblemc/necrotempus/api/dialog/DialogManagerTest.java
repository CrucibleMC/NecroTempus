package io.github.cruciblemc.necrotempus.api.dialog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import net.minecraft.util.ChatComponentText;

import org.junit.jupiter.api.Test;

class DialogManagerTest {

    @Test
    void registersAndUnregistersScriptDialogsByNamespacedId() {
        String id = "test:" + UUID.randomUUID()
            .toString()
            .replace("-", "");
        Dialog dialog = Dialog.notice(new ChatComponentText("Script test"))
            .action(new Dialog.ActionButton(new ChatComponentText("Close"), Dialog.Action.none()))
            .build();

        DialogManager.register(id, dialog);
        assertTrue(DialogManager.unregister(id));
        assertFalse(DialogManager.unregister(id));
    }

    @Test
    void registrationPolicyIsExplicitAndDefaultsToCompatibilityConfirmation() {
        Dialog dialog = Dialog.notice(new ChatComponentText("Command"))
            .action(new Dialog.ActionButton(new ChatComponentText("Run"), Dialog.Action.runCommand("help")))
            .build();

        assertEquals(Dialog.CommandExecutionPolicy.COMPATIBILITY_CONFIRM, dialog.getCommandExecutionPolicy());
        assertTrue(
            dialog.getCommandExecutionPolicy()
                .requiresConfirmation());
        assertEquals(
            Dialog.CommandExecutionPolicy.NO_CONFIRMATION,
            Dialog.notice(new ChatComponentText("Command"))
                .commandExecutionPolicy(Dialog.CommandExecutionPolicy.NO_CONFIRMATION)
                .action(new Dialog.ActionButton(new ChatComponentText("Run"), Dialog.Action.runCommand("help")))
                .build()
                .getCommandExecutionPolicy());
        assertFalse(Dialog.CommandExecutionPolicy.NO_CONFIRMATION.requiresConfirmation());
    }

    @Test
    void registersDialogListsWithNamespacedTargets() {
        String id = "test:" + UUID.randomUUID()
            .toString()
            .replace("-", "");
        Dialog list = Dialog.dialogList(new ChatComponentText("Menu"))
            .externalTitle(new ChatComponentText("Options"))
            .entry(new Dialog.Entry(new ChatComponentText("Open"), "test:target"))
            .build();

        DialogManager.register(id, list);
        try {
            assertSame(list, DialogManager.getRegistered(id));
        } finally {
            DialogManager.unregister(id);
        }
    }

    @Test
    void rejectsInvalidIdsAndRegistersServerCommandActions() {
        Dialog dialog = Dialog.notice(new ChatComponentText("Script test"))
            .action(new Dialog.ActionButton(new ChatComponentText("Run"), Dialog.Action.runCommand("/op")))
            .build();

        assertThrows(IllegalArgumentException.class, () -> DialogManager.register("bad id", dialog));
        DialogManager.register("test:command", dialog);
        assertSame(dialog, DialogManager.getRegistered("test:command"));
        assertTrue(DialogManager.unregister("test:command"));
    }

    @Test
    void registersTheCommandExecutionPolicyWithTheDialog() {
        String id = "test:" + UUID.randomUUID()
            .toString()
            .replace("-", "");
        Dialog dialog = Dialog.notice(new ChatComponentText("Command"))
            .action(new Dialog.ActionButton(new ChatComponentText("Run"), Dialog.Action.runCommand("help")))
            .build();

        DialogManager.register(id, dialog, Dialog.CommandExecutionPolicy.NO_CONFIRMATION);
        try {
            assertEquals(
                Dialog.CommandExecutionPolicy.NO_CONFIRMATION,
                DialogManager.getRegistered(id)
                    .getCommandExecutionPolicy());
            assertEquals(Dialog.CommandExecutionPolicy.COMPATIBILITY_CONFIRM, dialog.getCommandExecutionPolicy());
        } finally {
            DialogManager.unregister(id);
        }
    }
}
