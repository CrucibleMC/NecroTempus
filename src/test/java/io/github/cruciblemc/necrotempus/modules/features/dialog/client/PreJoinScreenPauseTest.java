package io.github.cruciblemc.necrotempus.modules.features.dialog.client;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PreJoinScreenPauseTest {

    @Test
    void onlyGameplayContextHonorsDialogPauseSetting() {
        assertTrue(DialogScreenContext.shouldPauseGame(DialogScreenContext.GAMEPLAY, true));
        assertFalse(DialogScreenContext.shouldPauseGame(PreJoinClientConnection.INSTANCE, true));
        assertFalse(DialogScreenContext.shouldPauseGame(DialogScreenContext.GAMEPLAY, false));
        assertFalse(DialogScreenContext.shouldPauseGame(PreJoinClientConnection.INSTANCE, false));
        assertTrue(DialogScreenContext.shouldPauseWarning(DialogScreenContext.GAMEPLAY));
        assertFalse(DialogScreenContext.shouldPauseWarning(PreJoinClientConnection.INSTANCE));
    }
}
