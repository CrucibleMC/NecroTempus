package io.github.cruciblemc.necrotempus.modules.features.dialog.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import net.minecraft.network.NetworkManager;
import net.minecraft.util.IChatComponent;

import org.junit.jupiter.api.Test;

import com.mojang.authlib.GameProfile;

import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.handshake.NetworkDispatcher;
import cpw.mods.fml.relauncher.Side;
import io.github.cruciblemc.necrotempus.modules.features.dialog.network.PreJoinPacket;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.AttributeKey;

class PreJoinServerAdapterTest {

    @Test
    void timeoutOnServerTickCleansTransportAndConnectionAttribute() throws Exception {
        CrossLayerFixture fixture = new CrossLayerFixture();
        try {
            fixture.start(TimeUnit.SECONDS.toNanos(121), connection -> {});

            adapter().onServerTick(new TickEvent.ServerTickEvent(TickEvent.Phase.END));

            fixture.assertCleaned();
        } finally {
            fixture.close();
        }
    }

    @Test
    void handlerExceptionDuringCapabilityConfirmationCleansTransportAndConnectionAttribute() throws Exception {
        CrossLayerFixture fixture = new CrossLayerFixture();
        try {
            fixture.start(System.nanoTime(), connection -> { throw new IllegalStateException("test"); });
            PreJoinServerAdapter
                .handle(PreJoinPacket.capability(PreJoinPacket.PROTOCOL_VERSION), fixture.connectionChannel);

            adapter().onServerTick(new TickEvent.ServerTickEvent(TickEvent.Phase.END));

            fixture.assertCleaned();
        } finally {
            fixture.close();
        }
    }

    @Test
    void serverStopCleansActiveTransportAndConnectionAttribute() throws Exception {
        CrossLayerFixture fixture = new CrossLayerFixture();
        try {
            fixture.start(System.nanoTime(), connection -> {});

            PreJoinServerAdapter.stop();

            fixture.assertCleaned();
        } finally {
            fixture.close();
        }
    }

    private static final class CrossLayerFixture {

        final Map<Object, Object> previousTransports;
        final List<Object> previousClosedTransports;
        final List<Object> previousInboundPackets;
        final Thread previousServerThread;
        final boolean previousStopping;
        final PreJoinConnections registry;
        EmbeddedChannel connectionChannel;
        EmbeddedChannel handshakeChannel;
        NetworkDispatcher dispatcher;
        PreJoinConnections.Attempt attempt;
        PreJoinServerAdapter.Transport transport;
        AutoCloseable registration;
        boolean started;

        CrossLayerFixture() throws Exception {
            previousTransports = new HashMap<>(transports());
            previousClosedTransports = new ArrayList<>(closedTransports());
            previousInboundPackets = new ArrayList<>(inboundPackets());
            previousServerThread = serverThread();
            previousStopping = stopping();
            registry = PreJoinConnections.instance();
        }

        void start(long ageNanos, Consumer<io.github.cruciblemc.necrotempus.api.dialog.PreJoinConnection> handler)
            throws Exception {
            assertEquals(0, registry.size(), "Pre-join singleton must start without attempts");
            assertNull(registrationState(), "Pre-join singleton must start without a registration");
            assertTrue(pendingResponses().isEmpty(), "Pre-join singleton must start without queued responses");
            assertTrue(transports().isEmpty(), "Adapter must start without transports");
            assertTrue(closedTransports().isEmpty(), "Adapter must start without closed transports");
            assertTrue(inboundPackets().isEmpty(), "Adapter must start without queued inbound work");
            assertFalse(stopping(), "Adapter must not already be stopping");
            started = true;
            PreJoinConnections.bindServerThread(Thread.currentThread());
            inboundPackets().clear();
            registration = registry.registerHandler(handler);
            NetworkManager manager = new NetworkManager(false);
            connectionChannel = new EmbeddedChannel(manager);
            dispatcher = new NetworkDispatcher(manager, null);
            handshakeChannel = new EmbeddedChannel(new ChannelInboundHandlerAdapter());
            handshakeChannel.attr(NetworkDispatcher.FML_DISPATCHER)
                .set(dispatcher);
            transport = new PreJoinServerAdapter.Transport(handshakeChannel, dispatcher, Side.SERVER);
            long started = System.nanoTime() - ageNanos;
            attempt = registry.begin(
                UUID.randomUUID(),
                new GameProfile(UUID.randomUUID(), "cross-layer"),
                new InetSocketAddress("127.0.0.1", 25565),
                transport,
                started);
            transport.attempt = attempt;
            transports().put(attempt, transport);
            connectionChannel.attr(attemptAttribute())
                .set(attempt);
            PreJoinServerAdapter.registerCloseListener(connectionChannel, transport);
            assertTrue(transports().containsKey(attempt));
            assertEquals(
                attempt,
                connectionChannel.attr(attemptAttribute())
                    .get());
        }

        void assertCleaned() throws ReflectiveOperationException {
            assertEquals(0, registry.size());
            assertFalse(transports().containsKey(attempt));
            assertTrue(transport.terminal);
            assertNull(
                connectionChannel.attr(attemptAttribute())
                    .get());
        }

        void close() throws Exception {
            try {
                if (connectionChannel != null) connectionChannel.close();
            } finally {
                try {
                    if (handshakeChannel != null) handshakeChannel.close();
                } finally {
                    try {
                        if (started) registry.stop();
                    } finally {
                        try {
                            if (registration != null) registration.close();
                        } finally {
                            try {
                                if (started) PreJoinServerAdapter.serverStarting();
                            } finally {
                                try {
                                    transports().clear();
                                    transports().putAll(previousTransports);
                                } finally {
                                    try {
                                        closedTransports().clear();
                                        closedTransports().addAll(previousClosedTransports);
                                    } finally {
                                        try {
                                            inboundPackets().clear();
                                            inboundPackets().addAll(previousInboundPackets);
                                        } finally {
                                            try {
                                                setStopping(previousStopping);
                                            } finally {
                                                PreJoinConnections.bindServerThread(previousServerThread);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static AttributeKey<PreJoinConnections.Attempt> attemptAttribute() throws ReflectiveOperationException {
        Field field = PreJoinServerAdapter.class.getDeclaredField("ATTEMPT");
        field.setAccessible(true);
        return (AttributeKey<PreJoinConnections.Attempt>) field.get(null);
    }

    @Test
    void timeoutOnServerTickReleasesAttempt() throws Exception {
        Thread previousServerThread = serverThread();
        PreJoinConnections.bindServerThread(Thread.currentThread());
        PreJoinConnections registry = PreJoinConnections.instance();
        AutoCloseable registration = null;
        try {
            registry.stop();
            registration = registry.registerHandler(connection -> {});
            RecordingEffects expiredEffects = new RecordingEffects();
            long expiredStartNanos = System.nanoTime() - TimeUnit.SECONDS.toNanos(121);

            for (int index = 0; index < 2; index++) {
                assertTrue(
                    registry.begin(
                        UUID.randomUUID(),
                        new GameProfile(UUID.randomUUID(), "attempt" + index),
                        new InetSocketAddress("127.0.0.1", 25565 + index),
                        index == 0 ? expiredEffects : new RecordingEffects(),
                        index == 0 ? expiredStartNanos : System.nanoTime()) != null);
            }
            assertEquals(2, registry.size());

            PreJoinServerAdapter.serverStarting();
            adapter().onServerTick(new TickEvent.ServerTickEvent(TickEvent.Phase.END));

            assertEquals(1, registry.size());
            assertEquals(1, expiredEffects.disconnects.get());
            adapter().onServerTick(new TickEvent.ServerTickEvent(TickEvent.Phase.END));
            assertEquals(1, registry.size());
            assertEquals(1, expiredEffects.disconnects.get());
            assertTrue(
                registry.begin(
                    UUID.randomUUID(),
                    new GameProfile(UUID.randomUUID(), "replacement"),
                    new InetSocketAddress("127.0.0.1", 25566),
                    new RecordingEffects(),
                    System.nanoTime()) != null);
        } finally {
            try {
                registry.stop();
                if (registration != null) registration.close();
            } finally {
                PreJoinConnections.bindServerThread(previousServerThread);
                PreJoinServerAdapter.serverStarting();
            }
        }
    }

    @Test
    void terminalTransportDoesNotQueueAStaleCloseEvent() {
        assertTrue(PreJoinServerAdapter.shouldQueueClose(false, false));
        assertFalse(PreJoinServerAdapter.shouldQueueClose(true, false));
        assertFalse(PreJoinServerAdapter.shouldQueueClose(false, true));
    }

    @Test
    void activeCloseIsCleanedOnTickButTerminalCloseIsNotQueued() throws Exception {
        Map<Object, Object> previousTransports = new HashMap<>(transports());
        List<Object> previousClosedTransports = new ArrayList<>(closedTransports());
        PreJoinConnections registry = null;
        AutoCloseable registration = null;
        EmbeddedChannel activeChannel = null;
        EmbeddedChannel terminalChannel = null;
        try {
            PreJoinServerAdapter.serverStarting();
            registry = new PreJoinConnections(() -> true);
            registration = registry.registerHandler(connection -> {});
            RecordingEffects activeEffects = new RecordingEffects();
            PreJoinConnections.Attempt activeAttempt = registry.begin(
                UUID.randomUUID(),
                new GameProfile(UUID.randomUUID(), "active"),
                new InetSocketAddress("127.0.0.1", 25565),
                activeEffects,
                0L);
            activeChannel = new EmbeddedChannel(new ChannelInboundHandlerAdapter());
            PreJoinServerAdapter.Transport activeTransport = new PreJoinServerAdapter.Transport(
                activeChannel,
                null,
                Side.SERVER);
            activeTransport.attempt = activeAttempt;
            transports().put(activeAttempt, activeTransport);
            assertTrue(transports().containsKey(activeAttempt));
            PreJoinServerAdapter.registerCloseListener(activeChannel, activeTransport);

            activeChannel.close();
            activeChannel.runPendingTasks();
            assertEquals(1, PreJoinServerAdapter.drainClosedTransports(registry));
            assertFalse(transports().containsKey(activeAttempt));
            assertEquals(0, registry.size());
            assertTrue(activeTransport.terminal);

            RecordingEffects terminalEffects = new RecordingEffects();
            PreJoinConnections.Attempt terminalAttempt = registry.begin(
                UUID.randomUUID(),
                new GameProfile(UUID.randomUUID(), "terminal"),
                new InetSocketAddress("127.0.0.1", 25566),
                terminalEffects,
                1L);
            terminalChannel = new EmbeddedChannel(new ChannelInboundHandlerAdapter());
            PreJoinServerAdapter.Transport terminalTransport = new PreJoinServerAdapter.Transport(
                terminalChannel,
                null,
                Side.SERVER);
            terminalTransport.attempt = terminalAttempt;
            terminalTransport.terminal = true;
            PreJoinServerAdapter.registerCloseListener(terminalChannel, terminalTransport);

            terminalChannel.close();
            terminalChannel.runPendingTasks();
            assertEquals(0, PreJoinServerAdapter.drainClosedTransports(registry));
            assertEquals(1, registry.size());
        } finally {
            try {
                if (activeChannel != null) {
                    activeChannel.close();
                    activeChannel.runPendingTasks();
                }
                if (terminalChannel != null) {
                    terminalChannel.close();
                    terminalChannel.runPendingTasks();
                }
                if (registry != null) {
                    PreJoinServerAdapter.drainClosedTransports(registry);
                    registry.stop();
                }
                if (registration != null) registration.close();
                PreJoinServerAdapter.serverStarting();
            } finally {
                transports().clear();
                transports().putAll(previousTransports);
                closedTransports().clear();
                closedTransports().addAll(previousClosedTransports);
            }
        }
    }

    private static final class RecordingEffects implements PreJoinConnections.Effects {

        final AtomicInteger disconnects = new AtomicInteger();

        @Override
        public boolean isOpen() {
            return true;
        }

        @Override
        public void show(io.github.cruciblemc.necrotempus.api.dialog.Dialog dialog, UUID sessionId, int revision) {}

        @Override
        public void allow() {}

        @Override
        public void disconnect(IChatComponent reason) {
            disconnects.incrementAndGet();
        }
    }

    private static Thread serverThread() throws ReflectiveOperationException {
        Field field = PreJoinConnections.class.getDeclaredField("serverThread");
        field.setAccessible(true);
        return (Thread) field.get(null);
    }

    private static Object registrationState() throws ReflectiveOperationException {
        Field field = PreJoinConnections.class.getDeclaredField("registration");
        field.setAccessible(true);
        return field.get(PreJoinConnections.instance());
    }

    @SuppressWarnings("unchecked")
    private static Queue<Object> pendingResponses() throws ReflectiveOperationException {
        Field field = PreJoinConnections.class.getDeclaredField("responses");
        field.setAccessible(true);
        return (java.util.Queue<Object>) field.get(PreJoinConnections.instance());
    }

    private static PreJoinServerAdapter adapter() throws ReflectiveOperationException {
        Field field = PreJoinServerAdapter.class.getDeclaredField("INSTANCE");
        field.setAccessible(true);
        return (PreJoinServerAdapter) field.get(null);
    }

    @SuppressWarnings("unchecked")
    private static Map<Object, Object> transports() throws ReflectiveOperationException {
        Field field = PreJoinServerAdapter.class.getDeclaredField("transports");
        field.setAccessible(true);
        return (Map<Object, Object>) field.get(adapter());
    }

    @SuppressWarnings("unchecked")
    private static ArrayBlockingQueue<Object> closedTransports() throws ReflectiveOperationException {
        Field field = PreJoinServerAdapter.class.getDeclaredField("closedTransports");
        field.setAccessible(true);
        return (ArrayBlockingQueue<Object>) field.get(adapter());
    }

    @SuppressWarnings("unchecked")
    private static ArrayBlockingQueue<Object> inboundPackets() throws ReflectiveOperationException {
        Field field = PreJoinServerAdapter.class.getDeclaredField("inboundPackets");
        field.setAccessible(true);
        return (ArrayBlockingQueue<Object>) field.get(adapter());
    }

    private static boolean stopping() throws ReflectiveOperationException {
        Field field = PreJoinServerAdapter.class.getDeclaredField("stopping");
        field.setAccessible(true);
        return field.getBoolean(adapter());
    }

    private static void setStopping(boolean value) throws ReflectiveOperationException {
        Field field = PreJoinServerAdapter.class.getDeclaredField("stopping");
        field.setAccessible(true);
        field.setBoolean(adapter(), value);
    }
}
