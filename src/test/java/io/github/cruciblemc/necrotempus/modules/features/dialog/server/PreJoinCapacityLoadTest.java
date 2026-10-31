package io.github.cruciblemc.necrotempus.modules.features.dialog.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import net.minecraft.util.IChatComponent;

import org.junit.jupiter.api.Test;

import com.mojang.authlib.GameProfile;

import cpw.mods.fml.common.gameevent.TickEvent;
import io.github.cruciblemc.necrotempus.modules.features.dialog.network.PreJoinPacket;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;

class PreJoinCapacityLoadTest {

    @Test
    void closesAttemptsAndReusesReleasedConnections() throws Exception {
        Thread serverThread = Thread.currentThread();
        PreJoinConnections registry = new PreJoinConnections(() -> Thread.currentThread() == serverThread);
        AutoCloseable registration = registry.registerHandler(connection -> {});
        RecordingEffects[] effects = new RecordingEffects[2];
        List<PreJoinConnections.Attempt> attempts = new ArrayList<>(2);
        try {
            for (int i = 0; i < 2; i++) {
                effects[i] = new RecordingEffects();
                PreJoinConnections.Attempt attempt = begin(registry, effects[i]);
                assertNotNull(attempt, "attempt " + i + " should fit");
                attempts.add(attempt);
            }
            assertEquals(2, registry.size());

            for (PreJoinConnections.Attempt attempt : attempts) assertTrue(registry.transportClosed(attempt));
            assertEquals(0, registry.size());
            assertNotNull(begin(registry, new RecordingEffects()), "released slots should be reusable");
        } finally {
            registry.stop();
            registration.close();
        }
    }

    @Test
    void inboundQueueLimitsEachChannelAndLeavesOtherChannelsUsable() throws Exception {
        Thread previousServerThread = serverThread();
        ArrayBlockingQueue<Object> inbound = inboundPackets();
        List<Object> queuedBefore = clearAndSave(inbound);
        EmbeddedChannel limited = new EmbeddedChannel(new ChannelInboundHandlerAdapter());
        EmbeddedChannel healthy = new EmbeddedChannel(new ChannelInboundHandlerAdapter());
        try {
            PreJoinConnections.bindServerThread(Thread.currentThread());
            for (int i = 0; i < 16; i++) PreJoinServerAdapter.handle(PreJoinPacket.hello(), limited);
            assertEquals(16, inbound.size());
            assertTrue(limited.isOpen());
            PreJoinServerAdapter.handle(PreJoinPacket.hello(), limited);
            assertFalse(limited.isOpen(), "the 17th packet closes only its channel");

            PreJoinServerAdapter.handle(PreJoinPacket.hello(), healthy);
            assertTrue(healthy.isOpen(), "another channel can still enqueue");
            assertEquals(17, inbound.size());
        } finally {
            inbound.clear();
            inbound.addAll(queuedBefore);
            limited.close();
            healthy.close();
            PreJoinConnections.bindServerThread(previousServerThread);
        }
        assertEquals(previousServerThread, serverThread());
    }

    @Test
    void concurrentInboundBurstFillsGlobalLimitAndTicksDrainAt256() throws Exception {
        Thread previousServerThread = serverThread();
        ArrayBlockingQueue<Object> inbound = inboundPackets();
        List<Object> queuedBefore = clearAndSave(inbound);
        int workers = 64;
        CountDownLatch ready = new CountDownLatch(workers);
        CountDownLatch start = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        List<EmbeddedChannel> channels = new ArrayList<>(workers);
        List<Thread> senders = new ArrayList<>(workers);
        EmbeddedChannel overflow = new EmbeddedChannel(new ChannelInboundHandlerAdapter());
        try {
            PreJoinConnections.bindServerThread(Thread.currentThread());
            for (int i = 0; i < workers; i++) {
                EmbeddedChannel channel = new EmbeddedChannel(new ChannelInboundHandlerAdapter());
                channels.add(channel);
                Thread sender = new Thread(() -> {
                    ready.countDown();
                    try {
                        start.await();
                        for (int packet = 0; packet < 16; packet++)
                            PreJoinServerAdapter.handle(PreJoinPacket.hello(), channel);
                    } catch (Throwable throwable) {
                        failure.compareAndSet(null, throwable);
                    }
                });
                senders.add(sender);
                sender.start();
            }
            ready.await();
            start.countDown();
            for (Thread sender : senders) sender.join();
            if (failure.get() != null) throw new AssertionError("inbound sender failed", failure.get());

            assertEquals(1024, inbound.size());
            for (EmbeddedChannel channel : channels) assertTrue(channel.isOpen());
            PreJoinServerAdapter.handle(PreJoinPacket.hello(), overflow);
            assertFalse(overflow.isOpen(), "global overflow closes only the channel that could not enqueue");
            for (EmbeddedChannel channel : channels) assertTrue(channel.isOpen());

            TickEvent.ServerTickEvent tick = new TickEvent.ServerTickEvent(TickEvent.Phase.END);
            adapter().onServerTick(tick);
            assertEquals(768, inbound.size());
            for (EmbeddedChannel channel : channels) assertTrue(channel.isOpen());
            adapter().onServerTick(tick);
            assertEquals(512, inbound.size());
            adapter().onServerTick(tick);
            assertEquals(256, inbound.size());
            adapter().onServerTick(tick);
            assertEquals(0, inbound.size());
        } finally {
            start.countDown();
            for (Thread sender : senders) sender.join();
            inbound.clear();
            inbound.addAll(queuedBefore);
            for (EmbeddedChannel channel : channels) channel.close();
            overflow.close();
            PreJoinConnections.bindServerThread(previousServerThread);
        }
        assertEquals(previousServerThread, serverThread());
    }

    private static List<Object> clearAndSave(ArrayBlockingQueue<Object> inbound) {
        List<Object> previous = new ArrayList<>();
        inbound.drainTo(previous);
        return previous;
    }

    private static PreJoinConnections.Attempt begin(PreJoinConnections registry, RecordingEffects effects) {
        return registry.begin(
            UUID.randomUUID(),
            new GameProfile(UUID.randomUUID(), "player"),
            new InetSocketAddress("127.0.0.1", 25565),
            effects,
            0L);
    }

    private static Thread serverThread() throws ReflectiveOperationException {
        Field field = PreJoinConnections.class.getDeclaredField("serverThread");
        field.setAccessible(true);
        return (Thread) field.get(null);
    }

    private static PreJoinServerAdapter adapter() throws ReflectiveOperationException {
        Field field = PreJoinServerAdapter.class.getDeclaredField("INSTANCE");
        field.setAccessible(true);
        return (PreJoinServerAdapter) field.get(null);
    }

    @SuppressWarnings("unchecked")
    private static ArrayBlockingQueue<Object> inboundPackets() throws ReflectiveOperationException {
        Field field = PreJoinServerAdapter.class.getDeclaredField("inboundPackets");
        field.setAccessible(true);
        return (ArrayBlockingQueue<Object>) field.get(adapter());
    }

    private static final class RecordingEffects implements PreJoinConnections.Effects {

        @Override
        public boolean isOpen() {
            return true;
        }

        @Override
        public void show(io.github.cruciblemc.necrotempus.api.dialog.Dialog dialog, UUID sessionId, int revision) {}

        @Override
        public void allow() {}

        @Override
        public void disconnect(IChatComponent reason) {}
    }
}
