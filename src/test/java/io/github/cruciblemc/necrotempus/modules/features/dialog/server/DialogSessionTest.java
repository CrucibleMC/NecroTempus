package io.github.cruciblemc.necrotempus.modules.features.dialog.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;

import org.junit.jupiter.api.Test;

import io.github.cruciblemc.necrotempus.api.dialog.Dialog;
import io.github.cruciblemc.necrotempus.api.dialog.DialogManager;

class DialogSessionTest {

    @Test
    void resolvesListSelectionsOnlyThroughRegisteredServerTargets() {
        String id = "test:" + UUID.randomUUID()
            .toString()
            .replace("-", "");
        Dialog destination = Dialog.notice(new ChatComponentText("Target"))
            .action(new Dialog.ActionButton(new ChatComponentText("Close"), Dialog.Action.none()))
            .build();
        DialogManager.register(id, destination);
        Dialog list = Dialog.dialogList(new ChatComponentText("Menu"))
            .entry(new Dialog.Entry(new ChatComponentText("Open"), id))
            .build();
        try {
            assertSame(destination, DialogSessions.resolveListDestination(list, 0));
            assertNull(DialogSessions.resolveListDestination(list, 1));
        } finally {
            DialogManager.unregister(id);
        }
    }

    @Test
    void resolvesInlineListAndActionTargetsWithoutRegisteringIds() {
        Dialog destination = Dialog.notice(new ChatComponentText("Inline target"))
            .action(new Dialog.ActionButton(new ChatComponentText("Close"), Dialog.Action.none()))
            .build();
        Dialog list = Dialog.dialogList(new ChatComponentText("Inline list"))
            .entry(new Dialog.Entry(new ChatComponentText("Open inline"), destination))
            .build();
        Dialog action = Dialog.notice(new ChatComponentText("Inline action"))
            .action(new Dialog.ActionButton(new ChatComponentText("Continue"), Dialog.Action.showDialog(destination)))
            .build();

        assertSame(destination, DialogSessions.resolveListDestination(list, 0));
        assertSame(destination, DialogSessions.resolveActionDestination(action, 0));
    }

    @Test
    void expandsDialogListTagsToServerReferencesInOrder() {
        String tagId = "test:" + UUID.randomUUID()
            .toString()
            .replace("-", "");
        String firstId = "test:" + UUID.randomUUID()
            .toString()
            .replace("-", "");
        String secondId = "test:" + UUID.randomUUID()
            .toString()
            .replace("-", "");
        Dialog first = Dialog.notice(new ChatComponentText("First internal"))
            .externalTitle(new ChatComponentText("First external"))
            .action(new Dialog.ActionButton(new ChatComponentText("Close"), Dialog.Action.none()))
            .build();
        Dialog second = Dialog.notice(new ChatComponentText("Second internal"))
            .externalTitle(new ChatComponentText("Second external"))
            .action(new Dialog.ActionButton(new ChatComponentText("Close"), Dialog.Action.none()))
            .build();
        DialogManager.register(firstId, first);
        DialogManager.register(secondId, second);
        DialogManager.registerTag(tagId, java.util.Arrays.asList(firstId, secondId));
        try {
            Dialog source = Dialog.dialogList(new ChatComponentText("Menu"))
                .columns(3)
                .buttonWidth(230)
                .commandExecutionPolicy(Dialog.CommandExecutionPolicy.NO_CONFIRMATION)
                .entry(Dialog.Entry.tag(tagId))
                .exitAction(new Dialog.ActionButton(new ChatComponentText("Run"), Dialog.Action.runCommand("help")))
                .build();
            Dialog expanded = DialogMenuService.expandDialogListTags(source);

            assertEquals(3, expanded.getColumns());
            assertEquals(230, expanded.getButtonWidth());
            assertEquals(Dialog.CommandExecutionPolicy.NO_CONFIRMATION, expanded.getCommandExecutionPolicy());
            assertEquals(
                2,
                expanded.getEntries()
                    .size());
            assertEquals(
                "First external",
                expanded.getEntries()
                    .get(0)
                    .getLabel()
                    .getUnformattedText());
            assertSame(first, DialogSessions.resolveListDestination(expanded, 0));
            assertEquals(
                "Second external",
                expanded.getEntries()
                    .get(1)
                    .getLabel()
                    .getUnformattedText());
            assertSame(second, DialogSessions.resolveListDestination(expanded, 1));
        } finally {
            DialogManager.unregisterTag(tagId);
            DialogManager.unregister(firstId);
            DialogManager.unregister(secondId);
        }
    }

    @Test
    void pauseMenuFallsBackToServerLinksOnlyWhenNoDialogIsAvailable() {
        Dialog.ServerLink link = new Dialog.ServerLink(new ChatComponentText("Home"), "https://example.com/");
        Dialog fallback = DialogMenuService
            .resolvePauseMenu(Collections.emptyList(), Collections.singletonList(link), "Links");
        assertEquals(Dialog.Type.SERVER_LINKS, fallback.getType());

        String id = "test:" + UUID.randomUUID()
            .toString()
            .replace("-", "");
        Dialog destination = Dialog.notice(new ChatComponentText("Direct"))
            .action(new Dialog.ActionButton(new ChatComponentText("OK"), Dialog.Action.none()))
            .build();
        DialogManager.register(id, destination);
        try {
            assertSame(
                destination,
                DialogMenuService
                    .resolvePauseMenu(Collections.singletonList(id), Collections.singletonList(link), "Links"));
        } finally {
            DialogManager.unregister(id);
        }
    }

    @Test
    void resolvesShowDialogActionsOnlyThroughRegisteredServerTargets() {
        String id = "test:" + UUID.randomUUID()
            .toString()
            .replace("-", "");
        Dialog destination = Dialog.notice(new ChatComponentText("Target"))
            .action(new Dialog.ActionButton(new ChatComponentText("Close"), Dialog.Action.none()))
            .build();
        DialogManager.register(id, destination);
        Dialog source = Dialog.notice(new ChatComponentText("Source"))
            .action(new Dialog.ActionButton(new ChatComponentText("Continue"), Dialog.Action.showDialog(id)))
            .build();
        try {
            assertSame(destination, DialogSessions.resolveActionDestination(source, 0));
            assertNull(DialogSessions.resolveActionDestination(source, 1));
            DialogManager.unregister(id);
            assertNull(DialogSessions.resolveActionDestination(source, 0));
        } finally {
            DialogManager.unregister(id);
        }
    }

    @Test
    void rejectsShowDialogActionsWithUnnamespacedTargets() {
        assertThrows(IllegalArgumentException.class, () -> Dialog.Action.showDialog("unregistered"));
    }

    @Test
    void resolvesServerLinkIndicesToValidatedLocalUrlActions() {
        Dialog links = Dialog.serverLinks(new ChatComponentText("Links"))
            .serverLink(new Dialog.ServerLink(new ChatComponentText("Home"), "https://example.com/"))
            .build();

        assertEquals(
            Dialog.ActionType.OPEN_URL,
            DialogSessions.actionFor(links, 0)
                .getType());
        assertEquals(
            "https://example.com/",
            DialogSessions.actionFor(links, 0)
                .getValue());
        assertArrayEquals(new int[] { 1 }, DialogSessions.actionUseLimits(links, 1, -1));
        assertNull(DialogSessions.actionFor(links, 1));
    }

    @Test
    void resolvesSelectedCustomActionFromTheServerDefinition() {
        Dialog dialog = Dialog.confirmation(new ChatComponentText("Choose"))
            .action(new Dialog.ActionButton(new ChatComponentText("Run"), Dialog.Action.custom("test:run", null)))
            .action(new Dialog.ActionButton(new ChatComponentText("Close"), Dialog.Action.none()))
            .exitAction(new Dialog.ActionButton(new ChatComponentText("Exit"), Dialog.Action.custom("test:exit", null)))
            .build();

        assertEquals(
            "test:run",
            DialogSessions.actionFor(dialog, 0)
                .getValue());
        assertEquals(
            Dialog.ActionType.NONE,
            DialogSessions.actionFor(dialog, 1)
                .getType());
        assertEquals(
            "test:exit",
            DialogSessions.actionFor(dialog, 2)
                .getValue());
        assertNull(DialogSessions.actionFor(dialog, -1));
    }

    @Test
    void acceptsInputValuesOnlyForDynamicCustomActions() {
        Dialog form = Dialog.notice(new ChatComponentText("Form"))
            .input(new Dialog.TextInput("name", new ChatComponentText("Name"), "", 16, 1))
            .action(
                new Dialog.ActionButton(
                    new ChatComponentText("Submit"),
                    Dialog.Action.dynamicCustom("test:submit", new NBTTagCompound())))
            .build();
        Map<String, Object> values = Collections.<String, Object>singletonMap("name", "Ada");

        assertEquals(values, DialogSessions.valuesForAction(form, DialogSessions.actionFor(form, 0), values));
        assertThrows(
            IllegalArgumentException.class,
            () -> DialogSessions.valuesForAction(form, Dialog.Action.none(), values));
    }

    @Test
    void localActionsDoNotInvokeTheServerCallback() {
        assertFalse(DialogSessions.invokesCallback(Dialog.Action.suggestCommand("/help")));
        assertFalse(DialogSessions.invokesCallback(Dialog.Action.copyToClipboard("copied")));
        assertFalse(DialogSessions.invokesCallback(Dialog.Action.openUrl("https://example.com")));
        assertFalse(DialogSessions.invokesCallback(Dialog.Action.showDialog("test:next")));
        assertTrue(DialogSessions.invokesCallback(Dialog.Action.none()));
        assertTrue(DialogSessions.invokesCallback(Dialog.Action.custom("test:run", null)));
    }

    @Test
    void requiresAcknowledgementAndAcceptsOnlyOneAuthorizedAction() {
        UUID id = UUID.randomUUID();
        DialogSession session = new DialogSession(id, 3, 2, 1, true);
        assertFalse(
            session.acceptAction(id, 3, 0)
                .isPresent());
        assertTrue(session.acknowledgeOpened(id, 3));
        assertFalse(
            session.acceptAction(id, 2, 0)
                .isPresent());
        assertFalse(
            session.acceptAction(id, 3, 2)
                .isPresent());
        assertEquals(
            0,
            session.acceptAction(id, 3, 0)
                .getAsInt());
        assertFalse(
            session.acceptAction(id, 3, 1)
                .isPresent());
    }

    @Test
    void noneKeepsTheSessionOpenUntilTheActionUseLimitIsReached() {
        UUID id = UUID.randomUUID();
        DialogSession session = new DialogSession(id, 1, new int[] { 2, 1 }, -1, true);
        session.acknowledgeOpened(id, 1);

        assertEquals(
            0,
            session.acceptAction(id, 1, 0, Dialog.AfterAction.NONE)
                .getAsInt());
        assertTrue(session.isOpen());
        assertEquals(1, session.remainingUses(0));
        assertEquals(
            0,
            session.acceptAction(id, 1, 0, Dialog.AfterAction.NONE)
                .getAsInt());
        assertTrue(session.isOpen());
        assertEquals(0, session.remainingUses(0));
        assertFalse(
            session.acceptAction(id, 1, 0, Dialog.AfterAction.NONE)
                .isPresent());
        assertEquals(
            1,
            session.acceptAction(id, 1, 1, Dialog.AfterAction.NONE)
                .getAsInt());
    }

    @Test
    void defaultActionsRemainUsableRepeatedlyWhileAfterActionIsNone() {
        Dialog dialog = Dialog.notice(new ChatComponentText("Repeat"))
            .action(new Dialog.ActionButton(new ChatComponentText("Stay"), Dialog.Action.none()))
            .afterAction(Dialog.AfterAction.NONE)
            .pause(false)
            .build();
        int[] limits = DialogSessions.actionUseLimits(dialog, 1, -1);
        UUID id = UUID.randomUUID();
        DialogSession session = new DialogSession(id, 1, limits, -1, true);
        session.acknowledgeOpened(id, 1);

        for (int i = 0; i < 3; i++) {
            assertEquals(
                0,
                session.acceptAction(id, 1, 0, Dialog.AfterAction.NONE)
                    .getAsInt());
        }
        assertTrue(session.isOpen());
    }

    @Test
    void waitForResponseBlocksActionsUntilDismissal() {
        UUID id = UUID.randomUUID();
        DialogSession session = new DialogSession(id, 1, new int[] { 2 }, -1, true);
        session.acknowledgeOpened(id, 1);

        assertEquals(
            0,
            session.acceptAction(id, 1, 0, Dialog.AfterAction.WAIT_FOR_RESPONSE)
                .getAsInt());
        assertTrue(session.isWaiting());
        assertFalse(
            session.acceptAction(id, 1, 0, Dialog.AfterAction.NONE)
                .isPresent());
        assertFalse(session.dismissWaiting(UUID.randomUUID(), 1));
        assertTrue(session.dismissWaiting(id, 1));
        assertTrue(session.isClosed());
    }

    @Test
    void cancellationAlwaysClosesEvenWhenNormalActionsKeepTheDialogOpen() {
        UUID id = UUID.randomUUID();
        DialogSession session = new DialogSession(id, 1, new int[] { 1, 1 }, 1, true);
        session.acknowledgeOpened(id, 1);
        session.acceptAction(id, 1, 0, Dialog.AfterAction.NONE);

        assertEquals(
            1,
            session.acceptCancel(id, 1)
                .getAsInt());
        assertTrue(session.isClosed());
    }

    @Test
    void copiesUseLimitsForListEntriesAndExitAction() {
        Dialog list = Dialog.dialogList(new ChatComponentText("Menu"))
            .entry(new Dialog.Entry(new ChatComponentText("Open"), "test:target"))
            .exitAction(
                new Dialog.ActionButton(
                    new ChatComponentText("Exit"),
                    Dialog.Action.none()
                        .withMaxUses(2)))
            .build();

        assertArrayEquals(new int[] { 1, 2 }, DialogSessions.actionUseLimits(list, 1, 1));
    }

    @Test
    void escapeUsesOnlyTheServerDefinedExitAction() {
        UUID id = UUID.randomUUID();
        DialogSession withExit = new DialogSession(id, 1, 3, 2, true);
        withExit.acknowledgeOpened(id, 1);
        assertEquals(
            2,
            withExit.acceptCancel(id, 1)
                .getAsInt());

        DialogSession withoutExit = new DialogSession(id, 1, 3, -1, true);
        withoutExit.acknowledgeOpened(id, 1);
        assertEquals(
            -1,
            withoutExit.acceptCancel(id, 1)
                .getAsInt());
    }

    @Test
    void rejectsEscapeWhenTheDialogIsNotCancellable() {
        UUID id = UUID.randomUUID();
        DialogSession session = new DialogSession(id, 1, 1, -1, false);
        session.acknowledgeOpened(id, 1);
        assertFalse(
            session.acceptCancel(id, 1)
                .isPresent());
    }

    @Test
    void externalGuiCloseInvalidatesWithoutAuthorizingAnAction() {
        UUID id = UUID.randomUUID();
        DialogSession session = new DialogSession(id, 1, 2, -1, true);
        assertTrue(session.closeByOtherGui(id, 1));
        assertTrue(session.isClosed());
        assertFalse(session.acknowledgeOpened(id, 1));
        assertFalse(
            session.acceptAction(id, 1, 0)
                .isPresent());
    }

    @Test
    void rejectionClosesAnAlreadyOpenedSession() {
        UUID id = UUID.randomUUID();
        DialogSession session = new DialogSession(id, 1, 1, -1, true);
        session.acknowledgeOpened(id, 1);
        assertTrue(session.reject(id, 1));
        assertTrue(session.isClosed());
        assertFalse(
            session.acceptAction(id, 1, 0)
                .isPresent());
    }

    @Test
    void overflowSessionMatchDoesNotAcceptStaleIdsOrRevisions() {
        UUID id = UUID.randomUUID();
        DialogSession session = new DialogSession(id, 2, 1, -1, true);
        assertTrue(session.matchesSession(id, 2));
        assertFalse(session.matchesSession(UUID.randomUUID(), 2));
        assertFalse(session.matchesSession(id, 1));
    }
}
