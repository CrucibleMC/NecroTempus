package io.github.cruciblemc.necrotempus.modules.features.dialog.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DialogWaitTimerTest {

    @Test
    void preservesVanillaPostIncrementThresholds() {
        DialogWaitTimer timer = new DialogWaitTimer();
        int elapsed = -1;

        for (int i = 0; i < 20; i++) elapsed = timer.tick();
        assertEquals(19, elapsed);
        assertFalse(DialogWaitTimer.isReturnVisible(elapsed));
        elapsed = timer.tick();
        assertEquals(20, elapsed);
        assertTrue(DialogWaitTimer.isReturnVisible(elapsed));
        assertEquals(1, DialogWaitTimer.countdownStep(elapsed));

        for (int i = 21; i < 100; i++) elapsed = timer.tick();
        assertEquals(99, elapsed);
        assertFalse(DialogWaitTimer.isReturnEnabled(elapsed));
        elapsed = timer.tick();
        assertEquals(100, elapsed);
        assertTrue(DialogWaitTimer.isReturnEnabled(elapsed));
        assertEquals(5, DialogWaitTimer.countdownStep(elapsed));
    }
}
