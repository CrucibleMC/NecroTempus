package io.github.cruciblemc.necrotempus.modules.features.dialog.server;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class PreJoinSessionTest {

    private static final long TIMEOUT_NANOS = 120_000_000_000L;

    @Test
    void confirmsCapabilityBeforeAllowingOnlyOnce() {
        PreJoinSession session = new PreJoinSession(UUID.randomUUID(), 0L);

        assertFalse(session.allow());
        assertTrue(session.confirmCapability());
        assertFalse(session.confirmCapability());
        assertFalse(session.isTerminal());
        assertTrue(session.allow());
        assertFalse(session.allow());
        assertFalse(session.close());
        assertTrue(session.isTerminal());
    }

    @Test
    void closeWinsDuringNegotiationAndPending() {
        PreJoinSession negotiating = new PreJoinSession(UUID.randomUUID(), 0L);
        assertTrue(negotiating.close());
        assertFalse(negotiating.close());
        assertFalse(negotiating.confirmCapability());
        assertFalse(negotiating.allow());
        assertTrue(negotiating.isTerminal());

        PreJoinSession pending = new PreJoinSession(UUID.randomUUID(), 0L);
        assertTrue(pending.confirmCapability());
        assertTrue(pending.close());
        assertFalse(pending.allow());
        assertTrue(pending.isTerminal());
    }

    @Test
    void expiresAtAbsoluteDeadlineWithoutRenewingOnConfirmation() {
        PreJoinSession session = new PreJoinSession(UUID.randomUUID(), 50L);
        assertTrue(session.confirmCapability());
        assertFalse(session.expire(50L + TIMEOUT_NANOS - 1));
        assertTrue(session.expire(50L + TIMEOUT_NANOS));
        assertFalse(session.close());
        assertFalse(session.expire(50L + TIMEOUT_NANOS + 1));
        assertFalse(session.allow());
        assertTrue(session.isTerminal());

        PreJoinSession afterDeadline = new PreJoinSession(UUID.randomUUID(), 50L);
        assertTrue(afterDeadline.confirmCapability());
        assertTrue(afterDeadline.expire(50L + TIMEOUT_NANOS + 1));
    }

    @Test
    void expiresByElapsedTimeAcrossNanoTimeOverflow() {
        long startedAt = Long.MAX_VALUE - 10;
        PreJoinSession session = new PreJoinSession(UUID.randomUUID(), startedAt);

        assertFalse(session.expire(startedAt + TIMEOUT_NANOS - 1));
        assertTrue(session.expire(startedAt + TIMEOUT_NANOS));
    }
}
