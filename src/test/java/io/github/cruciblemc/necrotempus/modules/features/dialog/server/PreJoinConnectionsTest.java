package io.github.cruciblemc.necrotempus.modules.features.dialog.server;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;

import org.junit.jupiter.api.Test;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;

import io.github.cruciblemc.necrotempus.api.dialog.Dialog;
import io.github.cruciblemc.necrotempus.api.dialog.DialogResponse;
import io.github.cruciblemc.necrotempus.api.dialog.PreJoinConnection;
import io.github.cruciblemc.necrotempus.modules.features.dialog.DialogCodec;
import io.github.cruciblemc.necrotempus.modules.features.dialog.PreJoinDialogPolicy;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

class PreJoinConnectionsTest {

    @Test
    void registrationSnapshotCanBeReadByNetworkThread() throws Exception {
        Thread serverThread = Thread.currentThread();
        PreJoinConnections registry = new PreJoinConnections(() -> Thread.currentThread() == serverThread);
        AutoCloseable registration = registry.registerHandler(connection -> {});
        AtomicBoolean observed = new AtomicBoolean();
        Thread networkThread = new Thread(() -> observed.set(registry.hasRegistration()));
        networkThread.start();
        networkThread.join();

        assertTrue(observed.get());
        registration.close();
    }

    @Test
    void releasesExpiredNegotiationsOnceWithoutAConnectionCap() throws Exception {
        PreJoinConnections registry = new PreJoinConnections(() -> true);
        AutoCloseable registration = registry.registerHandler(connection -> {});
        RecordingEffects effects = new RecordingEffects();
        UUID sameAccount = UUID.randomUUID();
        for (int i = 0; i < 2; i++) {
            assertTrue(
                registry.begin(
                    UUID.randomUUID(),
                    new GameProfile(sameAccount, "player"),
                    new InetSocketAddress("127.0.0.1", 25565),
                    effects,
                    10L) != null);
        }
        assertEquals(2, registry.size());
        registry.expire(10L + 120_000_000_000L);
        assertEquals(0, registry.size());
        assertEquals(2, effects.disconnects.get());
        registry.expire(10L + 120_000_000_001L);
        assertEquals(2, effects.disconnects.get());
        registration.close();
    }

    @Test
    void removalClosesOwnedAttemptsAndOldTokenCannotRemoveNewRegistration() throws Exception {
        PreJoinConnections registry = new PreJoinConnections(() -> true);
        AtomicInteger calls = new AtomicInteger();
        AutoCloseable old = registry.registerHandler(connection -> calls.incrementAndGet());
        RecordingEffects effects = new RecordingEffects();
        PreJoinConnections.Attempt oldAttempt = attempt(registry, effects, 0L);
        assertTrue(registry.confirmCapability(oldAttempt));
        old.close();
        assertEquals(1, effects.disconnects.get());
        AutoCloseable current = registry.registerHandler(connection -> calls.addAndGet(10));
        old.close();
        PreJoinConnections.Attempt currentAttempt = attempt(registry, effects, 0L);
        assertTrue(registry.confirmCapability(currentAttempt));
        assertEquals(11, calls.get());
        current.close();
    }

    @Test
    void preservesAllowedConnectionOnRemovalAndSkipsClosedChannel() throws Exception {
        PreJoinConnections registry = new PreJoinConnections(() -> true);
        AutoCloseable registration = registry.registerHandler(PreJoinConnection::allow);
        RecordingEffects effects = new RecordingEffects();
        assertTrue(registry.confirmCapability(attempt(registry, effects, 0L)));
        assertEquals(1, effects.allows.get());
        registration.close();
        assertEquals(0, effects.disconnects.get());

        AutoCloseable next = registry.registerHandler(connection -> {});
        RecordingEffects closedEffects = new RecordingEffects();
        closedEffects.open = false;
        PreJoinConnections.Attempt closed = attempt(registry, closedEffects, 0L);
        assertFalse(registry.confirmCapability(closed));
        assertEquals(0, registry.size());
        next.close();
    }

    @Test
    void transportCloseAndServerStopReleaseAttemptsWithoutAllowingThem() throws Exception {
        PreJoinConnections registry = new PreJoinConnections(() -> true);
        AutoCloseable registration = registry.registerHandler(connection -> {});
        RecordingEffects effects = new RecordingEffects();
        PreJoinConnections.Attempt attempt = attempt(registry, effects, 0L);

        assertTrue(registry.transportClosed(attempt));
        assertEquals(0, registry.size());
        assertEquals(0, effects.allows.get());

        RecordingEffects stoppingEffects = new RecordingEffects();
        attempt(registry, stoppingEffects, 1L);
        registry.stop();
        assertEquals(0, registry.size());
        assertEquals(1, stoppingEffects.disconnects.get());
        registration.close();
    }

    @Test
    void removalBeforeLateAllowClosesNegotiatingAndPendingAttempts() throws Exception {
        PreJoinConnections registry = new PreJoinConnections(() -> true);
        PreJoinConnection[] delivered = new PreJoinConnection[1];
        AutoCloseable registration = registry.registerHandler(connection -> delivered[0] = connection);
        RecordingEffects negotiatingEffects = new RecordingEffects();
        PreJoinConnections.Attempt negotiating = attempt(registry, negotiatingEffects, 0L);
        registration.close();
        assertEquals(1, negotiatingEffects.disconnects.get());
        assertEquals(0, negotiatingEffects.allows.get());

        AutoCloseable next = registry.registerHandler(connection -> delivered[0] = connection);
        RecordingEffects pendingEffects = new RecordingEffects();
        PreJoinConnections.Attempt pending = attempt(registry, pendingEffects, 0L);
        assertTrue(registry.confirmCapability(pending));
        next.close();
        delivered[0].allow();
        assertEquals(1, pendingEffects.disconnects.get());
        assertEquals(0, pendingEffects.allows.get());
    }

    @Test
    void validatesShowBeforeSendingAndCopiesProfile() throws Exception {
        PreJoinConnections registry = new PreJoinConnections(() -> true);
        PreJoinConnection[] delivered = new PreJoinConnection[1];
        AutoCloseable registration = registry.registerHandler(connection -> delivered[0] = connection);
        RecordingEffects effects = new RecordingEffects();
        GameProfile input = new GameProfile(UUID.randomUUID(), "player");
        input.getProperties()
            .put("textures", new Property("textures", "skin-a"));
        input.getProperties()
            .put("textures", new Property("textures", "skin-b"));
        PreJoinConnections.Attempt attempt = registry
            .begin(UUID.randomUUID(), input, new InetSocketAddress("127.0.0.1", 25565), effects, 0L);
        input.getProperties()
            .put("textures", new Property("textures", "changed-input"));
        assertTrue(registry.confirmCapability(attempt));
        GameProfile exposed = delivered[0].getProfile();
        exposed.getProperties()
            .put("test", new com.mojang.authlib.properties.Property("test", "changed"));
        exposed.getProperties()
            .get("textures")
            .clear();
        Collection<Property> copiedTextures = delivered[0].getProfile()
            .getProperties()
            .get("textures");
        assertEquals(2, copiedTextures.size());
        assertTrue(
            copiedTextures.stream()
                .anyMatch(property -> "skin-a".equals(property.getValue())));
        assertTrue(
            copiedTextures.stream()
                .anyMatch(property -> "skin-b".equals(property.getValue())));
        assertTrue(
            delivered[0].getProfile()
                .getProperties()
                .get("test")
                .isEmpty());
        assertTrue(delivered[0].supportsDialogs());
        assertThrows(IllegalArgumentException.class, () -> delivered[0].show(null, null));
        assertEquals(0, effects.shows.get());
        Dialog valid = Dialog.notice(new ChatComponentText("Title"))
            .afterAction(Dialog.AfterAction.WAIT_FOR_RESPONSE)
            .action(new Dialog.ActionButton(new ChatComponentText("Continue"), Dialog.Action.none()))
            .build();
        delivered[0].show(valid, (connection, response) -> { throw new IllegalStateException("callback failure"); });
        assertTrue(registry.opened(attempt, effects.sessionId, effects.revision));
        assertTrue(registry.enqueueResponse(attempt, effects.sessionId, effects.revision, 0, Collections.emptyMap()));
        registry.drainResponses();
        assertEquals(1, effects.disconnects.get());
        assertThrows(IllegalStateException.class, () -> delivered[0].show(valid, null));
        registration.close();
    }

    @Test
    void queuedCallbackFromRemovedRegistrationIsIgnored() throws Exception {
        PreJoinConnections registry = new PreJoinConnections(() -> true);
        PreJoinConnection[] delivered = new PreJoinConnection[1];
        AutoCloseable registration = registry.registerHandler(connection -> delivered[0] = connection);
        RecordingEffects effects = new RecordingEffects();
        PreJoinConnections.Attempt attempt = attempt(registry, effects, 0L);
        assertTrue(registry.confirmCapability(attempt));
        AtomicInteger callbackCalls = new AtomicInteger();
        Dialog dialog = Dialog.notice(new ChatComponentText("Title"))
            .afterAction(Dialog.AfterAction.WAIT_FOR_RESPONSE)
            .action(new Dialog.ActionButton(new ChatComponentText("Continue"), Dialog.Action.none()))
            .build();
        delivered[0].show(dialog, (connection, response) -> callbackCalls.incrementAndGet());
        assertTrue(registry.opened(attempt, effects.sessionId, effects.revision));
        assertTrue(registry.enqueueResponse(attempt, effects.sessionId, effects.revision, 0, Collections.emptyMap()));
        registration.close();
        delivered[0].allow();
        registry.drainResponses();
        assertEquals(1, effects.disconnects.get());
        assertEquals(0, effects.allows.get());
        assertEquals(0, registry.size());
        assertEquals(0, callbackCalls.get());
    }

    @Test
    void showClosesAndReleasesInsteadOfForwardingToClosedChannel() throws Exception {
        PreJoinConnections registry = new PreJoinConnections(() -> true);
        PreJoinConnection[] delivered = new PreJoinConnection[1];
        AutoCloseable registration = registry.registerHandler(connection -> delivered[0] = connection);
        RecordingEffects effects = new RecordingEffects();
        assertTrue(registry.confirmCapability(attempt(registry, effects, 0L)));
        Dialog dialog = Dialog.notice(new ChatComponentText("Title"))
            .afterAction(Dialog.AfterAction.WAIT_FOR_RESPONSE)
            .action(new Dialog.ActionButton(new ChatComponentText("Continue"), Dialog.Action.none()))
            .build();

        effects.open = false;
        assertThrows(IllegalStateException.class, () -> delivered[0].show(dialog, null));
        assertEquals(0, effects.shows.get());
        assertEquals(0, registry.size());
        registration.close();
    }

    @Test
    void onlyTheBoundThreadPassesEvenWhenAnotherThreadCopiesItsName() throws Exception {
        PreJoinConnections.bindServerThread(Thread.currentThread());
        try {
            PreJoinConnections registry = new PreJoinConnections(PreJoinConnections::isServerThread);
            AutoCloseable registration = registry.registerHandler(connection -> {});
            AtomicReference<Throwable> failure = new AtomicReference<>();
            Thread spoofed = new Thread(() -> {
                try {
                    registry.registerHandler(connection -> {});
                } catch (Throwable e) {
                    failure.set(e);
                }
            }, "Server thread");
            spoofed.start();
            spoofed.join();
            assertTrue(failure.get() instanceof IllegalStateException);
            registration.close();
        } finally {
            PreJoinConnections.bindServerThread(null);
        }
    }

    @Test
    void terminalDecisionsAreIdempotentAndOppositeDecisionHasNoEffect() throws Exception {
        PreJoinConnections allowedRegistry = new PreJoinConnections(() -> true);
        PreJoinConnection[] allowedConnection = new PreJoinConnection[1];
        AutoCloseable allowedRegistration = allowedRegistry
            .registerHandler(connection -> allowedConnection[0] = connection);
        RecordingEffects allowedEffects = new RecordingEffects();
        assertTrue(allowedRegistry.confirmCapability(attempt(allowedRegistry, allowedEffects, 0L)));
        allowedConnection[0].allow();
        allowedConnection[0].allow();
        allowedConnection[0].disconnect(new ChatComponentText("late"));
        assertEquals(1, allowedEffects.allows.get());
        assertEquals(0, allowedEffects.disconnects.get());
        allowedRegistration.close();

        PreJoinConnections closedRegistry = new PreJoinConnections(() -> true);
        PreJoinConnection[] closedConnection = new PreJoinConnection[1];
        AutoCloseable closedRegistration = closedRegistry
            .registerHandler(connection -> closedConnection[0] = connection);
        RecordingEffects closedEffects = new RecordingEffects();
        assertTrue(closedRegistry.confirmCapability(attempt(closedRegistry, closedEffects, 0L)));
        closedConnection[0].disconnect(new ChatComponentText("stop"));
        closedConnection[0].disconnect(new ChatComponentText("again"));
        closedConnection[0].allow();
        assertEquals(0, closedEffects.allows.get());
        assertEquals(1, closedEffects.disconnects.get());
        closedRegistration.close();
    }

    @Test
    void catchesHandlerExceptionAndRejectsOffThreadCalls() throws Exception {
        PreJoinConnections registry = new PreJoinConnections(() -> true);
        AutoCloseable registration = registry
            .registerHandler(connection -> { throw new IllegalStateException("failure"); });
        RecordingEffects effects = new RecordingEffects();
        PreJoinConnections.Attempt attempt = attempt(registry, effects, 0L);
        assertTrue(registry.confirmCapability(attempt));
        assertEquals(1, effects.disconnects.get());
        registration.close();
        assertThrows(
            IllegalStateException.class,
            () -> new PreJoinConnections(() -> false).registerHandler(connection -> {}));
    }

    @Test
    void showReplacementPublishesRevisionAndInvalidDefinitionKeepsCurrentSession() throws Exception {
        Fixture fixture = fixture();
        PreJoinConnections.Attempt attempt = fixture.attempt;
        Dialog first = notice("first");
        fixture.connection.show(first, (connection, response) -> {});
        UUID firstId = fixture.effects.sessionId;
        int firstRevision = fixture.effects.revision;
        assertTrue(fixture.registry.opened(attempt, firstId, firstRevision));

        Dialog invalid = Dialog.notice(new ChatComponentText("invalid"))
            .afterAction(Dialog.AfterAction.CLOSE)
            .action(button("close", Dialog.Action.none()))
            .build();
        assertThrows(IllegalArgumentException.class, () -> fixture.connection.show(invalid, null));
        assertEquals(firstId, fixture.effects.sessionId);
        assertEquals(firstRevision, fixture.effects.revision);

        Dialog oversized = dialogWithEncodedSize(DialogCodec.MAX_PACKET_BYTES);
        assertDoesNotThrow(() -> PreJoinDialogPolicy.validate(dialogWithEncodedSize(DialogCodec.MAX_PACKET_BYTES - 1)));
        int showsBeforeOversized = fixture.effects.shows.get();
        assertThrows(IllegalArgumentException.class, () -> fixture.connection.show(oversized, null));
        assertEquals(showsBeforeOversized, fixture.effects.shows.get());
        assertEquals(firstRevision, fixture.effects.revision);
        assertEquals(0, fixture.effects.disconnects.get());

        fixture.connection.show(notice("second"), (connection, response) -> {});
        assertEquals(firstId, fixture.effects.sessionId);
        assertEquals(firstRevision + 1, fixture.effects.revision);
        assertFalse(fixture.registry.opened(attempt, firstId, firstRevision));
        assertTrue(fixture.registry.opened(attempt, fixture.effects.sessionId, fixture.effects.revision));
        fixture.close();
    }

    @Test
    void acceptsOnlyFirstCurrentOpenedSubmissionAndCallbackCanReplaceOrRemainPending() throws Exception {
        Fixture fixture = fixture();
        AtomicInteger callbacks = new AtomicInteger();
        fixture.connection.show(notice("first"), (connection, response) -> {
            callbacks.incrementAndGet();
            connection.show(notice("next"), (next, nextResponse) -> callbacks.incrementAndGet());
        });
        UUID id = fixture.effects.sessionId;
        int revision = fixture.effects.revision;
        assertFalse(fixture.registry.enqueueResponse(fixture.attempt, id, revision, 0, Collections.emptyMap()));
        assertTrue(fixture.registry.opened(fixture.attempt, id, revision));
        assertTrue(fixture.registry.enqueueResponse(fixture.attempt, id, revision, 0, Collections.emptyMap()));
        assertFalse(fixture.registry.enqueueResponse(fixture.attempt, id, revision, 0, Collections.emptyMap()));
        assertEquals(1, fixture.registry.drainResponses());
        assertEquals(1, callbacks.get());
        assertEquals(revision + 1, fixture.effects.revision);
        fixture.close();

        Fixture waiting = fixture();
        waiting.connection.show(notice("waiting"), (connection, response) -> callbacks.incrementAndGet());
        assertTrue(waiting.registry.opened(waiting.attempt, waiting.effects.sessionId, waiting.effects.revision));
        assertTrue(
            waiting.registry.enqueueResponse(
                waiting.attempt,
                waiting.effects.sessionId,
                waiting.effects.revision,
                0,
                Collections.emptyMap()));
        waiting.registry.drainResponses();
        assertFalse(waiting.attempt.session.isTerminal());
        waiting.close();
    }

    @Test
    void duplicateSubmissionsAreRejectedBeforeDrainWithoutFillingQueueOrClosingConnection() throws Exception {
        Fixture fixture = fixture();
        AtomicInteger callbacks = new AtomicInteger();
        fixture.connection.show(notice("once"), (connection, response) -> callbacks.incrementAndGet());
        UUID id = fixture.effects.sessionId;
        int revision = fixture.effects.revision;
        assertTrue(fixture.registry.opened(fixture.attempt, id, revision));
        assertTrue(fixture.registry.enqueueResponse(fixture.attempt, id, revision, 0, Collections.emptyMap()));
        for (int i = 0; i < 16; i++) {
            assertFalse(fixture.registry.enqueueResponse(fixture.attempt, id, revision, 0, Collections.emptyMap()));
        }
        assertEquals(1, fixture.attempt.queuedResponses);
        assertEquals(0, fixture.effects.disconnects.get());
        assertEquals(1, fixture.registry.drainResponses());
        assertEquals(1, callbacks.get());
        assertEquals(0, fixture.effects.disconnects.get());
        fixture.close();
    }

    @Test
    void resolvesStaticActionToEmptyValuesAndValidatesDynamicCustomValues() throws Exception {
        Fixture fixture = fixture();
        AtomicReference<DialogResponse> received = new AtomicReference<>();
        Dialog staticDialog = Dialog.notice(new ChatComponentText("static"))
            .afterAction(Dialog.AfterAction.WAIT_FOR_RESPONSE)
            .input(new Dialog.TextInput("name", new ChatComponentText("Name"), "", 8, 1))
            .action(button("static", Dialog.Action.none()))
            .build();
        fixture.connection.show(staticDialog, (connection, response) -> received.set(response));
        UUID staticId = fixture.effects.sessionId;
        int staticRevision = fixture.effects.revision;
        assertTrue(fixture.registry.opened(fixture.attempt, staticId, staticRevision));
        assertTrue(
            fixture.registry.enqueueResponse(fixture.attempt, staticId, staticRevision, 0, Collections.emptyMap()));
        fixture.registry.drainResponses();
        assertEquals(
            Collections.emptyMap(),
            received.get()
                .getInputValues());

        Dialog dynamic = Dialog.notice(new ChatComponentText("dynamic"))
            .afterAction(Dialog.AfterAction.WAIT_FOR_RESPONSE)
            .input(new Dialog.TextInput("name", new ChatComponentText("Name"), "", 8, 1))
            .action(
                button("submit", Dialog.Action.dynamicCustom("test:submit", new net.minecraft.nbt.NBTTagCompound())))
            .build();
        fixture.connection.show(dynamic, (connection, response) -> received.set(response));
        UUID dynamicId = fixture.effects.sessionId;
        int dynamicRevision = fixture.effects.revision;
        assertTrue(fixture.registry.opened(fixture.attempt, dynamicId, dynamicRevision));
        Map<String, Object> values = Collections.singletonMap("name", "Ada");
        assertTrue(fixture.registry.enqueueResponse(fixture.attempt, dynamicId, dynamicRevision, 0, values));
        fixture.registry.drainResponses();
        assertEquals(
            "Ada",
            received.get()
                .getText("name"));
        assertEquals(
            "test:submit",
            received.get()
                .getActionId());
        fixture.close();
    }

    @Test
    void staleWrongConnectionAndInvalidIndexNeverInvokeCallback() throws Exception {
        Fixture first = fixture();
        Fixture second = fixture();
        AtomicInteger callbacks = new AtomicInteger();
        first.connection.show(notice("old"), (connection, response) -> callbacks.incrementAndGet());
        UUID oldId = first.effects.sessionId;
        int oldRevision = first.effects.revision;
        first.connection.show(notice("current"), (connection, response) -> callbacks.incrementAndGet());
        UUID currentId = first.effects.sessionId;
        int currentRevision = first.effects.revision;
        assertFalse(first.registry.enqueueResponse(first.attempt, oldId, oldRevision, 0, Collections.emptyMap()));
        assertFalse(
            second.registry.enqueueResponse(second.attempt, currentId, currentRevision, 0, Collections.emptyMap()));
        assertTrue(first.registry.opened(first.attempt, currentId, currentRevision));
        assertTrue(
            first.registry.enqueueResponse(first.attempt, currentId, currentRevision, 99, Collections.emptyMap()));
        first.registry.drainResponses();
        assertEquals(0, callbacks.get());
        assertEquals(1, first.effects.disconnects.get());
        first.close();
        second.close();
    }

    @Test
    void invalidFormValuesAndOversizedResponseCloseOnlyTheirSourceAttempt() throws Exception {
        Fixture invalid = fixture();
        AtomicInteger callbacks = new AtomicInteger();
        Dialog dialog = Dialog.notice(new ChatComponentText("form"))
            .afterAction(Dialog.AfterAction.WAIT_FOR_RESPONSE)
            .input(new Dialog.TextInput("name", new ChatComponentText("Name"), "", 3, 1))
            .action(
                button("submit", Dialog.Action.dynamicCustom("test:submit", new net.minecraft.nbt.NBTTagCompound())))
            .build();
        invalid.connection.show(dialog, (connection, response) -> callbacks.incrementAndGet());
        assertTrue(invalid.registry.opened(invalid.attempt, invalid.effects.sessionId, invalid.effects.revision));
        assertTrue(
            invalid.registry.enqueueResponse(
                invalid.attempt,
                invalid.effects.sessionId,
                invalid.effects.revision,
                0,
                Collections.singletonMap("name", "long")));
        invalid.registry.drainResponses();
        assertEquals(0, callbacks.get());
        assertEquals(1, invalid.effects.disconnects.get());
        invalid.close();

        Fixture oversized = fixture();
        oversized.connection.show(dialog, (connection, response) -> callbacks.incrementAndGet());
        assertTrue(
            oversized.registry.opened(oversized.attempt, oversized.effects.sessionId, oversized.effects.revision));
        assertFalse(
            oversized.registry.enqueueResponse(
                oversized.attempt,
                oversized.effects.sessionId,
                oversized.effects.revision,
                0,
                Collections.singletonMap("name", repeat('x', 5000))));
        assertEquals(1, oversized.effects.disconnects.get());
        oversized.close();
    }

    @Test
    void showingAndReplacingDialogsDoesNotRenewConnectionDeadline() throws Exception {
        PreJoinConnections registry = new PreJoinConnections(() -> true);
        PreJoinConnection[] delivered = new PreJoinConnection[1];
        AutoCloseable registration = registry.registerHandler(connection -> delivered[0] = connection);
        RecordingEffects effects = new RecordingEffects();
        PreJoinConnections.Attempt attempt = attempt(registry, effects, 50L);
        assertTrue(registry.confirmCapability(attempt));
        delivered[0].show(notice("one"), (connection, response) -> {});
        delivered[0].show(notice("two"), (connection, response) -> {});
        registry.expire(50L + 120_000_000_000L);
        assertEquals(0, registry.size());
        assertEquals(1, effects.disconnects.get());
        registration.close();
    }

    @Test
    void closedChannelBeforeResponseDrainReleasesSlotWithoutCallingCallback() throws Exception {
        Fixture fixture = fixture();
        AtomicInteger callbacks = new AtomicInteger();
        fixture.connection.show(notice("closed"), (connection, response) -> callbacks.incrementAndGet());
        assertTrue(fixture.registry.opened(fixture.attempt, fixture.effects.sessionId, fixture.effects.revision));
        assertTrue(
            fixture.registry.enqueueResponse(
                fixture.attempt,
                fixture.effects.sessionId,
                fixture.effects.revision,
                0,
                Collections.emptyMap()));
        fixture.effects.open = false;
        assertEquals(1, fixture.registry.drainResponses());
        assertEquals(0, callbacks.get());
        assertEquals(0, fixture.registry.size());
        fixture.close();
    }

    @Test
    void queueKeepsOneResponsePerDialogAndDrainsDistinctConnections() throws Exception {
        Fixture fixture = fixture();
        fixture.connection.show(notice("capacity"), (connection, response) -> {});
        UUID id = fixture.effects.sessionId;
        int revision = fixture.effects.revision;
        assertTrue(fixture.registry.opened(fixture.attempt, id, revision));
        assertTrue(fixture.registry.enqueueResponse(fixture.attempt, id, revision, 0, Collections.emptyMap()));
        for (int i = 0; i < 16; i++) {
            assertFalse(fixture.registry.enqueueResponse(fixture.attempt, id, revision, 0, Collections.emptyMap()));
        }
        assertEquals(1, fixture.attempt.queuedResponses);
        assertEquals(0, fixture.effects.disconnects.get());
        assertEquals(1, fixture.registry.drainResponses());
        fixture.close();

        PreJoinConnections registry = new PreJoinConnections(() -> true);
        RecordingEffects[] effects = new RecordingEffects[65];
        PreJoinConnection[] connections = new PreJoinConnection[65];
        PreJoinConnections.Attempt[] attempts = new PreJoinConnections.Attempt[65];
        AtomicInteger nextConnection = new AtomicInteger();
        AutoCloseable registration = registry
            .registerHandler(connection -> { connections[nextConnection.getAndIncrement()] = connection; });
        for (int i = 0; i < 65; i++) {
            effects[i] = new RecordingEffects();
            attempts[i] = attempt(registry, effects[i], 0L);
            registry.confirmCapability(attempts[i]);
            connections[i].show(notice("global"), (connection, response) -> {});
            registry.opened(attempts[i], effects[i].sessionId, effects[i].revision);
        }
        for (int i = 0; i < 65; i++) {
            assertTrue(
                registry.enqueueResponse(
                    attempts[i],
                    effects[i].sessionId,
                    effects[i].revision,
                    0,
                    Collections.emptyMap()));
        }
        assertEquals(0, effects[0].disconnects.get());
        assertEquals(0, effects[64].disconnects.get());
        assertEquals(65, registry.drainResponses());
        registration.close();
    }

    private static Fixture fixture() throws Exception {
        PreJoinConnections registry = new PreJoinConnections(() -> true);
        PreJoinConnection[] connection = new PreJoinConnection[1];
        AutoCloseable registration = registry.registerHandler(value -> connection[0] = value);
        RecordingEffects effects = new RecordingEffects();
        PreJoinConnections.Attempt attempt = attempt(registry, effects, 0L);
        registry.confirmCapability(attempt);
        return new Fixture(registry, registration, effects, attempt, connection[0]);
    }

    private static Dialog notice(String title) {
        return Dialog.notice(new ChatComponentText(title))
            .afterAction(Dialog.AfterAction.WAIT_FOR_RESPONSE)
            .action(button("ok", Dialog.Action.none()))
            .build();
    }

    private static Dialog.ActionButton button(String label, Dialog.Action action) {
        return new Dialog.ActionButton(new ChatComponentText(label), action);
    }

    private static String repeat(char value, int count) {
        char[] chars = new char[count];
        Arrays.fill(chars, value);
        return new String(chars);
    }

    private static Dialog dialogWithEncodedSize(int targetSize) {
        Dialog base = largeNotice(repeat('x', 32000), repeat('x', 32000), "");
        ByteBuf buffer = Unpooled.buffer();
        int baseSize;
        try {
            DialogCodec.write(buffer, base);
            baseSize = buffer.readableBytes();
        } finally {
            buffer.release();
        }
        Dialog result = largeNotice(repeat('x', 32000), repeat('x', 32000), repeat('x', targetSize - baseSize));
        buffer = Unpooled.buffer();
        try {
            DialogCodec.write(buffer, result);
            assertEquals(targetSize, buffer.readableBytes());
        } finally {
            buffer.release();
        }
        return result;
    }

    private static Dialog largeNotice(String... bodies) {
        Dialog.Builder builder = Dialog.notice(new ChatComponentText("large"))
            .afterAction(Dialog.AfterAction.WAIT_FOR_RESPONSE);
        for (String body : bodies) builder.body(new Dialog.TextBody(new ChatComponentText(body)));
        return builder.action(button("ok", Dialog.Action.none()))
            .build();
    }

    private static final class Fixture {

        final PreJoinConnections registry;
        final AutoCloseable registration;
        final RecordingEffects effects;
        final PreJoinConnections.Attempt attempt;
        final PreJoinConnection connection;

        Fixture(PreJoinConnections registry, AutoCloseable registration, RecordingEffects effects,
            PreJoinConnections.Attempt attempt, PreJoinConnection connection) {
            this.registry = registry;
            this.registration = registration;
            this.effects = effects;
            this.attempt = attempt;
            this.connection = connection;
        }

        void close() throws Exception {
            registration.close();
        }
    }

    private static PreJoinConnections.Attempt attempt(PreJoinConnections registry, RecordingEffects effects, long now) {
        return registry.begin(
            UUID.randomUUID(),
            new GameProfile(UUID.randomUUID(), "player"),
            new InetSocketAddress("127.0.0.1", 25565),
            effects,
            now);
    }

    private static final class RecordingEffects implements PreJoinConnections.Effects {

        final AtomicInteger allows = new AtomicInteger();
        final AtomicInteger disconnects = new AtomicInteger();
        final AtomicInteger shows = new AtomicInteger();
        boolean open = true;
        UUID sessionId;
        int revision;

        @Override
        public boolean isOpen() {
            return open;
        }

        @Override
        public void show(Dialog dialog, UUID sessionId, int revision) {
            shows.incrementAndGet();
            this.sessionId = sessionId;
            this.revision = revision;
        }

        @Override
        public void allow() {
            allows.incrementAndGet();
        }

        @Override
        public void disconnect(IChatComponent reason) {
            disconnects.incrementAndGet();
        }
    }
}
