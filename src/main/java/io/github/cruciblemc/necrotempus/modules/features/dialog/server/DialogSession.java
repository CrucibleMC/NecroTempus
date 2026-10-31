package io.github.cruciblemc.necrotempus.modules.features.dialog.server;

import java.util.Arrays;
import java.util.OptionalInt;
import java.util.UUID;

import io.github.cruciblemc.necrotempus.api.dialog.Dialog;

final class DialogSession {

    private enum State {
        PENDING,
        OPEN,
        WAITING,
        CLOSED
    }

    private final UUID id;
    private final int revision;
    private final int cancelActionIndex;
    private final boolean canCancel;
    private final int[] remainingUses;
    private State state = State.PENDING;

    DialogSession(UUID id, int revision, int actionCount, int cancelActionIndex, boolean canCancel) {
        this(id, revision, useLimits(actionCount), cancelActionIndex, canCancel);
    }

    DialogSession(UUID id, int revision, int[] useLimits, int cancelActionIndex, boolean canCancel) {
        if (id == null || revision < 1
            || useLimits == null
            || useLimits.length == 0
            || cancelActionIndex < -1
            || cancelActionIndex >= useLimits.length) {
            throw new IllegalArgumentException("Invalid dialog session");
        }
        this.id = id;
        this.revision = revision;
        this.cancelActionIndex = cancelActionIndex;
        this.canCancel = canCancel;
        this.remainingUses = Arrays.copyOf(useLimits, useLimits.length);
        for (int limit : remainingUses) {
            if (limit < 1) throw new IllegalArgumentException("Action uses must be positive");
        }
    }

    boolean acknowledgeOpened(UUID responseId, int responseRevision) {
        if (state != State.PENDING || !matches(responseId, responseRevision)) return false;
        state = State.OPEN;
        return true;
    }

    OptionalInt acceptAction(UUID responseId, int responseRevision, int actionIndex) {
        return acceptAction(responseId, responseRevision, actionIndex, Dialog.AfterAction.CLOSE);
    }

    OptionalInt acceptAction(UUID responseId, int responseRevision, int actionIndex, Dialog.AfterAction afterAction) {
        if (state != State.OPEN || !matches(responseId, responseRevision)
            || afterAction == null
            || actionIndex < 0
            || actionIndex >= remainingUses.length
            || remainingUses[actionIndex] == 0) {
            return OptionalInt.empty();
        }
        remainingUses[actionIndex]--;
        if (afterAction == Dialog.AfterAction.CLOSE) state = State.CLOSED;
        else if (afterAction == Dialog.AfterAction.WAIT_FOR_RESPONSE) state = State.WAITING;
        return OptionalInt.of(actionIndex);
    }

    OptionalInt acceptCancel(UUID responseId, int responseRevision) {
        if (state != State.OPEN || !matches(responseId, responseRevision) || !canCancel) return OptionalInt.empty();
        state = State.CLOSED;
        return OptionalInt.of(cancelActionIndex);
    }

    boolean dismissWaiting(UUID responseId, int responseRevision) {
        if (state != State.WAITING || !matches(responseId, responseRevision)) return false;
        state = State.CLOSED;
        return true;
    }

    boolean closeByOtherGui(UUID responseId, int responseRevision) {
        if (state == State.CLOSED || !matches(responseId, responseRevision)) return false;
        state = State.CLOSED;
        return true;
    }

    boolean reject(UUID responseId, int responseRevision) {
        if (state == State.CLOSED || !matches(responseId, responseRevision)) return false;
        state = State.CLOSED;
        return true;
    }

    boolean matchesSession(UUID responseId, int responseRevision) {
        return matches(responseId, responseRevision);
    }

    boolean isPending() {
        return state == State.PENDING;
    }

    boolean isOpen() {
        return state == State.OPEN;
    }

    boolean isWaiting() {
        return state == State.WAITING;
    }

    boolean isClosed() {
        return state == State.CLOSED;
    }

    int remainingUses(int actionIndex) {
        return actionIndex < 0 || actionIndex >= remainingUses.length ? 0 : remainingUses[actionIndex];
    }

    private boolean matches(UUID responseId, int responseRevision) {
        return id.equals(responseId) && revision == responseRevision;
    }

    private static int[] useLimits(int actionCount) {
        if (actionCount < 1) throw new IllegalArgumentException("A dialog session needs an action");
        int[] result = new int[actionCount];
        Arrays.fill(result, 1);
        return result;
    }
}
