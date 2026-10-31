package io.github.cruciblemc.necrotempus.modules.features.dialog.server;

import java.util.Objects;
import java.util.UUID;

final class PreJoinSession {

    private static final long TIMEOUT_NANOS = 120_000_000_000L;

    private enum State {
        NEGOTIATING,
        PENDING,
        ALLOWED,
        CLOSED,
        EXPIRED
    }

    private final UUID connectionId;
    private final long startedAtNanos;
    private State state = State.NEGOTIATING;

    PreJoinSession(UUID connectionId, long startedAtNanos) {
        this.connectionId = Objects.requireNonNull(connectionId, "connectionId");
        this.startedAtNanos = startedAtNanos;
    }

    boolean confirmCapability() {
        if (state != State.NEGOTIATING) return false;
        state = State.PENDING;
        return true;
    }

    boolean isNegotiating() {
        return state == State.NEGOTIATING;
    }

    boolean allow() {
        if (state != State.PENDING) return false;
        state = State.ALLOWED;
        return true;
    }

    boolean close() {
        if (isTerminal()) return false;
        state = State.CLOSED;
        return true;
    }

    boolean expire(long nowNanos) {
        if (isTerminal() || nowNanos - startedAtNanos < TIMEOUT_NANOS) return false;
        state = State.EXPIRED;
        return true;
    }

    boolean isTerminal() {
        return state == State.ALLOWED || state == State.CLOSED || state == State.EXPIRED;
    }
}
