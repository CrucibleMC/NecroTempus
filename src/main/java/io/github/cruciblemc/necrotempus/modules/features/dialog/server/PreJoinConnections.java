package io.github.cruciblemc.necrotempus.modules.features.dialog.server;

import java.net.SocketAddress;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;

import io.github.cruciblemc.necrotempus.api.dialog.Dialog;
import io.github.cruciblemc.necrotempus.api.dialog.DialogResponse;
import io.github.cruciblemc.necrotempus.api.dialog.PreJoinConnection;
import io.github.cruciblemc.necrotempus.modules.features.dialog.DialogFormValues;
import io.github.cruciblemc.necrotempus.modules.features.dialog.PreJoinDialogPolicy;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/** Registry and bounded lifetime ledger for suspended handshakes. */
public final class PreJoinConnections {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final long TIMEOUT_NANOS = 120_000_000_000L;
    private static final int MAX_PENDING_PER_CONNECTION = 16;
    private static final int MAX_PENDING_GLOBAL = 1024;
    private static final int MAX_DRAIN_PER_TICK = 256;
    private static volatile Thread serverThread;
    private static final PreJoinConnections INSTANCE = new PreJoinConnections(PreJoinConnections::isServerThread);

    private final BooleanSupplier onServerThread;
    private final List<Attempt> attempts = new ArrayList<>();
    private final Queue<QueuedResponse> responses = new ArrayDeque<>();
    private volatile Registration registration;

    public PreJoinConnections() {
        this(PreJoinConnections::isServerThread);
    }

    PreJoinConnections(BooleanSupplier onServerThread) {
        this.onServerThread = Objects.requireNonNull(onServerThread, "onServerThread");
    }

    public static AutoCloseable register(Consumer<PreJoinConnection> handler) {
        if (PreJoinServerLifecycle.isIntegratedServer()) {
            throw new IllegalStateException("Pre-join dialogs are not supported on integrated servers");
        }
        return INSTANCE.registerHandler(handler);
    }

    static PreJoinConnections instance() {
        return INSTANCE;
    }

    AutoCloseable registerHandler(Consumer<PreJoinConnection> handler) {
        requireServerThread();
        Objects.requireNonNull(handler, "handler");
        if (registration != null) throw new IllegalStateException("A pre-join dialog handler is already registered");
        Registration current = new Registration(handler);
        registration = current;
        return () -> remove(current);
    }

    Attempt begin(UUID connectionId, GameProfile profile, SocketAddress remoteAddress, Effects effects, long nowNanos) {
        requireServerThread();
        if (registration == null) return null;
        Attempt attempt = new Attempt(
            registration,
            new Connection(connectionId, profile, remoteAddress, effects),
            nowNanos);
        attempts.add(attempt);
        return attempt;
    }

    boolean hasRegistration() {
        return registration != null;
    }

    boolean transportClosed(Attempt attempt) {
        requireServerThread();
        if (!attempts.remove(attempt)) return false;
        attempt.session.close();
        attempt.currentDialog = null;
        discardResponses(attempt, null);
        return true;
    }

    boolean reject(Attempt attempt, IChatComponent reason) {
        return close(attempt, reason);
    }

    void stop() {
        requireServerThread();
        for (Attempt attempt : attempts) {
            attempt.session.close();
            attempt.currentDialog = null;
            discardResponses(attempt, null);
            if (attempt.effects.isOpen())
                attempt.effects.disconnect(new ChatComponentTranslation("necrotempus.prejoin.server_stopping"));
        }
        attempts.clear();
        responses.clear();
    }

    boolean confirmCapability(Attempt attempt) {
        requireServerThread();
        if (!attempts.contains(attempt) || attempt.owner != registration) return false;
        if (!attempt.effects.isOpen()) {
            close(attempt, new ChatComponentTranslation("necrotempus.prejoin.closed"));
            return false;
        }
        if (!attempt.session.confirmCapability()) return false;
        try {
            attempt.owner.handler.accept(attempt.connection);
        } catch (RuntimeException e) {
            LOGGER.warn(
                "[PreJoinDialogs] handler failed exception={}",
                e.getClass()
                    .getName());
            close(attempt, new ChatComponentTranslation("necrotempus.prejoin.internal_error"));
        }
        return true;
    }

    boolean opened(Attempt attempt, UUID sessionId, int revision) {
        requireServerThread();
        CurrentDialog current = attempt.currentDialog;
        if (!isActive(attempt) || !ensureOpen(attempt)
            || current == null
            || current.opened
            || !current.matches(sessionId, revision)) return false;
        current.opened = true;
        return true;
    }

    boolean enqueueResponse(Attempt attempt, UUID sessionId, int revision, int actionIndex,
        Map<String, Object> values) {
        requireServerThread();
        CurrentDialog current = attempt.currentDialog;
        if (!isActive(attempt) || !ensureOpen(attempt)
            || current == null
            || !current.opened
            || current.submitted
            || current.responseQueued
            || !current.matches(sessionId, revision)) return false;
        Map<String, Object> snapshot;
        try {
            snapshot = copyResponseValues(values);
        } catch (IllegalArgumentException exception) {
            close(attempt, new ChatComponentTranslation("necrotempus.prejoin.invalid_response"));
            return false;
        }
        if (attempt.queuedResponses >= MAX_PENDING_PER_CONNECTION || responses.size() >= MAX_PENDING_GLOBAL) {
            close(attempt, new ChatComponentTranslation("necrotempus.prejoin.queue_overflow"));
            return false;
        }
        responses.add(new QueuedResponse(attempt, current, actionIndex, snapshot));
        attempt.queuedResponses++;
        current.responseQueued = true;
        return true;
    }

    int drainResponses() {
        requireServerThread();
        int drained = 0;
        while (drained < MAX_DRAIN_PER_TICK && !responses.isEmpty()) {
            QueuedResponse queued = responses.remove();
            queued.attempt.queuedResponses--;
            drained++;
            process(queued);
        }
        return drained;
    }

    void expire(long nowNanos) {
        requireServerThread();
        Iterator<Attempt> iterator = attempts.iterator();
        while (iterator.hasNext()) {
            Attempt attempt = iterator.next();
            if (attempt.session.expire(nowNanos)) {
                iterator.remove();
                attempt.currentDialog = null;
                discardResponses(attempt, null);
                if (attempt.effects.isOpen())
                    attempt.effects.disconnect(new ChatComponentTranslation("necrotempus.prejoin.timeout"));
            }
        }
    }

    int size() {
        return attempts.size();
    }

    private void remove(Registration removed) {
        requireServerThread();
        if (registration != removed) return;
        registration = null;
        Iterator<Attempt> iterator = attempts.iterator();
        while (iterator.hasNext()) {
            Attempt attempt = iterator.next();
            if (attempt.owner != removed) continue;
            iterator.remove();
            attempt.currentDialog = null;
            discardResponses(attempt, null);
            if (attempt.session.close() && attempt.effects.isOpen()) {
                attempt.effects.disconnect(new ChatComponentTranslation("necrotempus.prejoin.handler_removed"));
            }
        }
    }

    private boolean allow(Attempt attempt) {
        requireServerThread();
        if (!attempts.contains(attempt) || attempt.owner != registration) return false;
        if (!attempt.effects.isOpen()) {
            close(attempt, new ChatComponentTranslation("necrotempus.prejoin.closed"));
            return false;
        }
        if (!attempt.session.allow()) return false;
        attempts.remove(attempt);
        attempt.currentDialog = null;
        discardResponses(attempt, null);
        attempt.effects.allow();
        return true;
    }

    private boolean close(Attempt attempt, IChatComponent reason) {
        requireServerThread();
        if (!attempts.remove(attempt) || !attempt.session.close()) return false;
        attempt.currentDialog = null;
        discardResponses(attempt, null);
        if (attempt.effects.isOpen()) attempt.effects.disconnect(reason);
        return true;
    }

    private boolean isActive(Attempt attempt) {
        return attempt.owner == registration && attempts.contains(attempt) && !attempt.session.isTerminal();
    }

    private boolean ensureOpen(Attempt attempt) {
        if (attempt.effects.isOpen()) return true;
        close(attempt, new ChatComponentTranslation("necrotempus.prejoin.closed"));
        return false;
    }

    private void process(QueuedResponse queued) {
        Attempt attempt = queued.attempt;
        CurrentDialog current = queued.dialog;
        if (!isActive(attempt) || !ensureOpen(attempt)
            || attempt.currentDialog != current
            || !current.opened
            || current.submitted) return;
        current.submitted = true;
        Dialog.Action action = actionFor(current.dialog, queued.actionIndex);
        if (action == null) {
            close(attempt, new ChatComponentTranslation("necrotempus.prejoin.invalid_response"));
            return;
        }
        Map<String, Object> values;
        try {
            if (action.isDynamic() && action.getType() == Dialog.ActionType.CUSTOM) {
                values = DialogFormValues.validate(current.dialog, queued.values);
            } else {
                if (!queued.values.isEmpty()) throw new IllegalArgumentException("Unexpected input values");
                values = Collections.emptyMap();
            }
        } catch (IllegalArgumentException exception) {
            close(attempt, new ChatComponentTranslation("necrotempus.prejoin.invalid_response"));
            return;
        }
        if (current.callback == null) return;
        try {
            current.callback.accept(attempt.connection, new DialogResponse(queued.actionIndex, false, values, action));
        } catch (RuntimeException exception) {
            LOGGER.warn(
                "[PreJoinDialogs] callback failed exception={}",
                exception.getClass()
                    .getName());
            close(attempt, new ChatComponentTranslation("necrotempus.prejoin.internal_error"));
        }
    }

    private static Dialog.Action actionFor(Dialog dialog, int index) {
        if (index >= 0 && index < dialog.getActions()
            .size()) return dialog.getActions()
                .get(index)
                .getAction();
        if (index == dialog.getActions()
            .size() && dialog.getExitAction() != null) return dialog.getExitAction()
                .getAction();
        return null;
    }

    private void discardResponses(Attempt attempt, CurrentDialog dialog) {
        Iterator<QueuedResponse> iterator = responses.iterator();
        while (iterator.hasNext()) {
            QueuedResponse queued = iterator.next();
            if (queued.attempt != attempt || (dialog != null && queued.dialog != dialog)) continue;
            iterator.remove();
            attempt.queuedResponses--;
        }
    }

    private static Map<String, Object> copyResponseValues(Map<String, Object> values) {
        ByteBuf buffer = Unpooled.buffer();
        try {
            DialogFormValues.write(buffer, values);
            return DialogFormValues.read(buffer);
        } finally {
            buffer.release();
        }
    }

    private void requireServerThread() {
        if (!onServerThread.getAsBoolean())
            throw new IllegalStateException("Pre-join dialogs must be used on the server thread");
    }

    static boolean isServerThread() {
        return Thread.currentThread() == serverThread;
    }

    static void bindServerThread(Thread thread) {
        serverThread = thread;
    }

    private static void copyProperties(GameProfile source, GameProfile target) {
        for (Map.Entry<String, Property> entry : source.getProperties()
            .entries())
            target.getProperties()
                .put(entry.getKey(), entry.getValue());
    }

    private final class Connection implements PreJoinConnection {

        private final UUID connectionId;
        private final GameProfile profile;
        private final SocketAddress remoteAddress;
        private final Effects effects;
        private Attempt attempt;

        Connection(UUID connectionId, GameProfile profile, SocketAddress remoteAddress, Effects effects) {
            this.connectionId = Objects.requireNonNull(connectionId, "connectionId");
            GameProfile input = Objects.requireNonNull(profile, "profile");
            this.profile = new GameProfile(input.getId(), input.getName());
            copyProperties(input, this.profile);
            this.remoteAddress = Objects.requireNonNull(remoteAddress, "remoteAddress");
            this.effects = Objects.requireNonNull(effects, "effects");
        }

        @Override
        public UUID getConnectionId() {
            return connectionId;
        }

        @Override
        public GameProfile getProfile() {
            GameProfile copy = new GameProfile(profile.getId(), profile.getName());
            copyProperties(profile, copy);
            return copy;
        }

        @Override
        public SocketAddress getRemoteAddress() {
            return remoteAddress;
        }

        @Override
        public boolean supportsDialogs() {
            return true;
        }

        @Override
        public void show(Dialog dialog, BiConsumer<PreJoinConnection, DialogResponse> callback) {
            requireServerThread();
            if (attempt.session.isTerminal()) throw new IllegalStateException("Pre-join connection is terminal");
            if (!effects.isOpen()) {
                close(attempt, new ChatComponentTranslation("necrotempus.prejoin.closed"));
                throw new IllegalStateException("Pre-join connection is closed");
            }
            PreJoinDialogPolicy.validate(dialog);
            if (attempt.dialogRevision == Integer.MAX_VALUE)
                throw new IllegalStateException("Dialog revision exhausted");
            discardResponses(attempt, attempt.currentDialog);
            CurrentDialog current = new CurrentDialog(
                attempt.dialogSessionId,
                ++attempt.dialogRevision,
                dialog,
                callback);
            attempt.currentDialog = current;
            try {
                effects.show(dialog, current.sessionId, current.revision);
            } catch (RuntimeException exception) {
                LOGGER.warn(
                    "[PreJoinDialogs] show failed exception={}",
                    exception.getClass()
                        .getName());
                close(attempt, new ChatComponentTranslation("necrotempus.prejoin.internal_error"));
            }
        }

        @Override
        public void allow() {
            if (attempt != null) PreJoinConnections.this.allow(attempt);
        }

        @Override
        public void disconnect(IChatComponent reason) {
            if (attempt != null) close(attempt, Objects.requireNonNull(reason, "reason"));
        }
    }

    static final class Attempt {

        final Registration owner;
        final Connection connection;
        final Effects effects;
        final PreJoinSession session;
        final UUID dialogSessionId = UUID.randomUUID();
        int dialogRevision;
        int queuedResponses;
        CurrentDialog currentDialog;

        Attempt(Registration owner, Connection connection, long nowNanos) {
            this.owner = owner;
            this.connection = connection;
            this.effects = connection.effects;
            this.session = new PreJoinSession(connection.connectionId, nowNanos);
            connection.attempt = this;
        }
    }

    static final class CurrentDialog {

        final UUID sessionId;
        final int revision;
        final Dialog dialog;
        final BiConsumer<PreJoinConnection, DialogResponse> callback;
        boolean opened;
        boolean responseQueued;
        boolean submitted;

        CurrentDialog(UUID sessionId, int revision, Dialog dialog,
            BiConsumer<PreJoinConnection, DialogResponse> callback) {
            this.sessionId = sessionId;
            this.revision = revision;
            this.dialog = dialog;
            this.callback = callback;
        }

        boolean matches(UUID candidateId, int candidateRevision) {
            return sessionId.equals(candidateId) && revision == candidateRevision;
        }
    }

    private static final class QueuedResponse {

        final Attempt attempt;
        final CurrentDialog dialog;
        final int actionIndex;
        final Map<String, Object> values;

        QueuedResponse(Attempt attempt, CurrentDialog dialog, int actionIndex, Map<String, Object> values) {
            this.attempt = attempt;
            this.dialog = dialog;
            this.actionIndex = actionIndex;
            this.values = values;
        }
    }

    private static final class Registration {

        final Consumer<PreJoinConnection> handler;

        Registration(Consumer<PreJoinConnection> handler) {
            this.handler = handler;
        }
    }

    interface Effects {

        boolean isOpen();

        void show(Dialog dialog, UUID sessionId, int revision);

        void allow();

        void disconnect(IChatComponent reason);
    }
}
