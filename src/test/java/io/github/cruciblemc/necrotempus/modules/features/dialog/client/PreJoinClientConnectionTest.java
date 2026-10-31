package io.github.cruciblemc.necrotempus.modules.features.dialog.client;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.github.cruciblemc.necrotempus.api.dialog.Dialog;

class PreJoinClientConnectionTest {

    private static final UUID SESSION = UUID.randomUUID();

    @Test
    void onlyAcceptsNewerRevisionsForTheSameConnectionSession() {
        assertTrue(PreJoinClientConnection.acceptsShow(null, 0, SESSION, 1));
        assertTrue(PreJoinClientConnection.acceptsShow(SESSION, 1, SESSION, 2));
        assertFalse(PreJoinClientConnection.acceptsShow(SESSION, 1, SESSION, 1));
        assertFalse(PreJoinClientConnection.acceptsShow(SESSION, 2, SESSION, 1));
        assertFalse(PreJoinClientConnection.acceptsShow(SESSION, 1, UUID.randomUUID(), 2));
    }

    @Test
    void staleHelloCannotReplaceAnOpenConnection() {
        assertFalse(PreJoinClientConnection.canBegin(true, false, true, false));
        assertTrue(PreJoinClientConnection.canBegin(true, false, false, false));
        assertTrue(PreJoinClientConnection.canBegin(true, true, true, false));
        assertTrue(PreJoinClientConnection.canBegin(false, false, false, false));
    }

    @Test
    void helloAfterJoinIsIgnoredButAHelloFromANewHandlerCanBegin() {
        assertFalse(PreJoinClientConnection.canBegin(false, false, true, true));
        assertTrue(PreJoinClientConnection.canBegin(false, false, true, false));
    }

    @Test
    void disconnectedOrTerminalConnectionsIgnorePreJoinMessages() {
        assertFalse(PreJoinClientConnection.canReceive(true, true, false));
        assertFalse(PreJoinClientConnection.canReceive(true, true, true));
        assertFalse(PreJoinClientConnection.canReceive(false, false, false));
        assertTrue(PreJoinClientConnection.canReceive(true, false, true));
    }

    @Test
    void onlyTheCurrentConnectionCanHandOffJoinGame() {
        assertTrue(PreJoinClientConnection.canFinishJoin(true));
        assertFalse(PreJoinClientConnection.canFinishJoin(false));
    }

    @Test
    void onlySubmitsAnOpenedCurrentDialogOnceWhileWaitingForResponse() {
        assertTrue(
            PreJoinClientConnection
                .canSubmit(SESSION, 3, SESSION, 3, true, false, false, Dialog.AfterAction.WAIT_FOR_RESPONSE));
        assertFalse(
            PreJoinClientConnection
                .canSubmit(SESSION, 3, SESSION, 3, false, false, false, Dialog.AfterAction.WAIT_FOR_RESPONSE));
        assertFalse(
            PreJoinClientConnection
                .canSubmit(SESSION, 3, SESSION, 3, true, true, false, Dialog.AfterAction.WAIT_FOR_RESPONSE));
        assertFalse(
            PreJoinClientConnection
                .canSubmit(SESSION, 3, SESSION, 3, true, false, true, Dialog.AfterAction.WAIT_FOR_RESPONSE));
        assertFalse(
            PreJoinClientConnection.canSubmit(SESSION, 3, SESSION, 3, true, false, false, Dialog.AfterAction.CLOSE));
        assertFalse(
            PreJoinClientConnection
                .canSubmit(SESSION, 3, SESSION, 2, true, false, false, Dialog.AfterAction.WAIT_FOR_RESPONSE));
    }
}
