package io.github.cruciblemc.necrotempus.modules.features.dialog.server;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mojang.authlib.GameProfile;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.handshake.NetworkDispatcher;
import cpw.mods.fml.relauncher.Side;
import io.github.cruciblemc.necrotempus.modules.features.dialog.network.PreJoinPacket;
import io.github.cruciblemc.necrotempus.modules.features.dialog.prejoin.PreJoinNetwork;
import io.github.cruciblemc.necrotempus.modules.mixin.mixins.fml.NetworkDispatcherPlayerAccessor;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;

/** Bridges the FML handshake transport to the pre-join connection registry. */
public final class PreJoinServerAdapter {

    enum HookDecision {
        PASS_THROUGH,
        REJECT_UNSUPPORTED,
        START
    }

    private static final Logger LOGGER = LogManager.getLogger();
    private static final AttributeKey<PreJoinConnections.Attempt> ATTEMPT = new AttributeKey<>(
        "necrotempus:prejoin-attempt");
    private static final AttributeKey<Boolean> CLIENT_SUPPORTED = new AttributeKey<>("necrotempus:prejoin-supported");
    private static final AttributeKey<AtomicInteger> QUEUED_INBOUND = new AttributeKey<>(
        "necrotempus:prejoin-queued-inbound");
    private static final int MAX_CLOSE_WORK = 128;
    private static final int MAX_INBOUND_PER_CHANNEL = 16;
    private static final int MAX_INBOUND_WORK = 1024;
    private static final int MAX_DRAIN_PER_TICK = 256;
    private static final long HEARTBEAT_INTERVAL_NANOS = 10_000_000_000L;
    private static final PreJoinServerAdapter INSTANCE = new PreJoinServerAdapter();

    private final ArrayBlockingQueue<ClosedTransport> closedTransports = new ArrayBlockingQueue<>(MAX_CLOSE_WORK);
    private final ArrayBlockingQueue<PacketWork> inboundPackets = new ArrayBlockingQueue<>(MAX_INBOUND_WORK);
    private final Map<PreJoinConnections.Attempt, Transport> transports = new HashMap<>();
    private volatile boolean stopping;

    private PreJoinServerAdapter() {}

    public static void register() {
        FMLCommonHandler.instance()
            .bus()
            .register(INSTANCE);
    }

    public static void markClientSupported(NetHandlerPlayServer handler) {
        if (handler != null) markClientSupported(handler.netManager.channel());
    }

    static void markClientSupported(Channel channel) {
        channel.attr(CLIENT_SUPPORTED)
            .set(Boolean.TRUE);
    }

    static boolean isClientSupported(Channel channel) {
        return Boolean.TRUE.equals(
            channel.attr(CLIENT_SUPPORTED)
                .get());
    }

    static HookDecision decideHook(boolean handlerRegistered, boolean clientSupported) {
        if (!handlerRegistered) return HookDecision.PASS_THROUGH;
        return clientSupported ? HookDecision.START : HookDecision.REJECT_UNSUPPORTED;
    }

    static boolean shouldQueueClose(boolean stopping, boolean terminal) {
        return !stopping && !terminal;
    }

    public static boolean suspend(ChannelHandlerContext context, Side target) {
        if (target != Side.SERVER) return false;
        PreJoinConnections registry = PreJoinConnections.instance();
        boolean handlerRegistered = registry.hasRegistration();

        NetworkDispatcher dispatcher = context.channel()
            .attr(NetworkDispatcher.FML_DISPATCHER)
            .get();
        if (dispatcher == null) {
            if (!handlerRegistered) return false;
            context.close();
            return true;
        }
        Channel connectionChannel = dispatcher.manager.channel();
        HookDecision decision = decideHook(handlerRegistered, isClientSupported(connectionChannel));
        if (decision == HookDecision.PASS_THROUGH) return false;
        if (decision == HookDecision.REJECT_UNSUPPORTED) {
            removeDispatcher(context.channel(), dispatcher, "client lacks pre-join channel");
            dispatcher.manager.closeChannel(new ChatComponentTranslation("necrotempus.prejoin.unsupported"));
            return true;
        }
        if (!PreJoinConnections.isServerThread()) {
            failClosed(context, dispatcher, "hook ran outside server thread");
            return true;
        }
        if (!connectionChannel.isActive()) {
            removeDispatcher(context.channel(), dispatcher, "inactive connection");
            return true;
        }
        EntityPlayerMP player = ((NetworkDispatcherPlayerAccessor) dispatcher).necrotempus$getPlayer();
        if (player == null) {
            failClosed(context, dispatcher, "player unavailable");
            return true;
        }

        GameProfile profile = player.getGameProfile();
        SocketAddress remoteAddress = connectionChannel.remoteAddress();
        if (profile == null || remoteAddress == null) {
            failClosed(context, dispatcher, "connection identity unavailable");
            return true;
        }
        Transport transport = new Transport(context.channel(), dispatcher, target);
        PreJoinConnections.Attempt attempt = registry
            .begin(UUID.randomUUID(), profile, remoteAddress, transport, System.nanoTime());
        if (attempt == null) {
            removeDispatcher(context.channel(), dispatcher, "pre-join handler unavailable");
            dispatcher.manager.closeChannel(new ChatComponentTranslation("necrotempus.prejoin.handler_removed"));
            return true;
        }

        transport.attempt = attempt;
        INSTANCE.transports.put(attempt, transport);
        connectionChannel.attr(ATTEMPT)
            .set(attempt);
        registerCloseListener(connectionChannel, transport);
        try {
            PreJoinNetwork.send(dispatcher, PreJoinPacket.hello());
            return true;
        } catch (RuntimeException exception) {
            registry.reject(attempt, new ChatComponentTranslation("necrotempus.prejoin.internal_error"));
            return true;
        }
    }

    public static void handle(PreJoinPacket packet, Channel channel) {
        AtomicInteger freshCount = new AtomicInteger();
        AtomicInteger queued = channel.attr(QUEUED_INBOUND)
            .setIfAbsent(freshCount);
        if (queued == null) queued = freshCount;
        while (true) {
            int count = queued.get();
            if (count >= MAX_INBOUND_PER_CHANNEL) {
                channel.close();
                return;
            }
            if (queued.compareAndSet(count, count + 1)) break;
        }
        if (!INSTANCE.inboundPackets.offer(new PacketWork(packet, channel, queued))) {
            queued.decrementAndGet();
            channel.close();
        }
    }

    private static void process(PreJoinPacket packet, Channel channel) {
        PreJoinConnections registry = PreJoinConnections.instance();
        PreJoinConnections.Attempt attempt = channel.attr(ATTEMPT)
            .get();
        if (attempt == null) return;
        if (!packet.isValidFor(Side.SERVER)) {
            registry.reject(attempt, new ChatComponentTranslation("necrotempus.prejoin.invalid_packet"));
            return;
        }
        switch (packet.getOperation()) {
            case CAPABILITY:
                if (packet.getCapabilityVersion() != PreJoinPacket.PROTOCOL_VERSION) {
                    registry.reject(attempt, new ChatComponentTranslation("necrotempus.prejoin.version_mismatch"));
                    return;
                }
                if (attempt.session.isNegotiating()) registry.confirmCapability(attempt);
                break;
            case OPENED:
                registry.opened(attempt, packet.getSessionId(), packet.getRevision());
                break;
            case SUBMIT:
                registry.enqueueResponse(
                    attempt,
                    packet.getSessionId(),
                    packet.getRevision(),
                    packet.getActionIndex(),
                    packet.getValues());
                break;
            case DISCONNECT:
                registry.reject(attempt, new ChatComponentTranslation("necrotempus.prejoin.client_disconnected"));
                break;
            default:
                registry.reject(attempt, new ChatComponentTranslation("necrotempus.prejoin.invalid_packet"));
        }
    }

    public static void stop() {
        if (!PreJoinConnections.isServerThread()) return;
        INSTANCE.stopping = true;
        INSTANCE.closedTransports.clear();
        INSTANCE.clearInboundPackets();
        PreJoinConnections.instance()
            .stop();
        INSTANCE.transports.clear();
        INSTANCE.closedTransports.clear();
        INSTANCE.clearInboundPackets();
    }

    public static void serverStarting() {
        INSTANCE.closedTransports.clear();
        INSTANCE.stopping = false;
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !PreJoinConnections.isServerThread()) return;
        drainClosedTransports(PreJoinConnections.instance());
        PreJoinConnections.instance()
            .expire(System.nanoTime());
        PacketWork work;
        int drained = 0;
        while (drained < MAX_DRAIN_PER_TICK && (work = inboundPackets.poll()) != null) {
            work.queued.decrementAndGet();
            process(work.packet, work.channel);
            drained++;
        }
        long now = System.nanoTime();
        List<Transport> activeTransports = new ArrayList<>(transports.values());
        for (Transport transport : activeTransports) {
            transport.sendHeartbeatIfDue(now);
        }
        PreJoinConnections.instance()
            .drainResponses();
    }

    static void registerCloseListener(Channel channel, Transport transport) {
        channel.closeFuture()
            .addListener(ignored -> {
                if (shouldQueueClose(INSTANCE.stopping, transport.terminal))
                    INSTANCE.closedTransports.offer(new ClosedTransport(channel, transport));
            });
    }

    static int drainClosedTransports(PreJoinConnections registry) {
        int drained = 0;
        ClosedTransport closed;
        while ((closed = INSTANCE.closedTransports.poll()) != null) {
            INSTANCE.transports.remove(closed.attempt);
            registry.transportClosed(closed.attempt);
            closed.channel.attr(ATTEMPT)
                .compareAndSet(closed.attempt, null);
            closed.transport.closeObserved();
            drained++;
        }
        return drained;
    }

    private static final class ClosedTransport {

        final Channel channel;
        final Transport transport;
        final PreJoinConnections.Attempt attempt;

        ClosedTransport(Channel channel, Transport transport) {
            this.channel = channel;
            this.transport = transport;
            this.attempt = transport.attempt;
        }
    }

    private static final class PacketWork {

        final PreJoinPacket packet;
        final Channel channel;
        final AtomicInteger queued;

        PacketWork(PreJoinPacket packet, Channel channel, AtomicInteger queued) {
            this.packet = packet;
            this.channel = channel;
            this.queued = queued;
        }
    }

    private void clearInboundPackets() {
        PacketWork work;
        while ((work = inboundPackets.poll()) != null) work.queued.decrementAndGet();
    }

    static final class Transport implements PreJoinConnections.Effects {

        final Channel handshakeChannel;
        final NetworkDispatcher dispatcher;
        final Side target;
        volatile boolean terminal;
        PreJoinConnections.Attempt attempt;
        long lastHeartbeatNanos;
        UUID sessionId;
        int revision;

        Transport(Channel handshakeChannel, NetworkDispatcher dispatcher, Side target) {
            this.handshakeChannel = handshakeChannel;
            this.dispatcher = dispatcher;
            this.target = target;
            this.lastHeartbeatNanos = System.nanoTime();
        }

        void sendHeartbeatIfDue(long nowNanos) {
            if (terminal || nowNanos - lastHeartbeatNanos < HEARTBEAT_INTERVAL_NANOS) return;
            lastHeartbeatNanos = nowNanos;
            try {
                PreJoinNetwork.send(dispatcher, PreJoinPacket.hello());
            } catch (RuntimeException exception) {
                PreJoinConnections.instance()
                    .reject(attempt, new ChatComponentTranslation("necrotempus.prejoin.internal_error"));
            }
        }

        void closeObserved() {
            terminal = true;
            INSTANCE.transports.remove(attempt);
            removeDispatcher(handshakeChannel, dispatcher, "during close cleanup");
        }

        @Override
        public boolean isOpen() {
            return !terminal && dispatcher.manager.channel()
                .isActive();
        }

        @Override
        public void show(io.github.cruciblemc.necrotempus.api.dialog.Dialog dialog, UUID sessionId, int revision) {
            clearCurrent();
            PreJoinNetwork.send(dispatcher, PreJoinPacket.show(sessionId, revision, dialog));
            this.sessionId = sessionId;
            this.revision = revision;
        }

        @Override
        public void allow() {
            if (terminal) return;
            terminal = true;
            INSTANCE.transports.remove(attempt);
            try {
                clearCurrent();
            } catch (RuntimeException exception) {
                LOGGER.debug("Pre-join clear failed during allow", exception);
            }
            dispatcher.manager.channel()
                .attr(ATTEMPT)
                .compareAndSet(attempt, null);
            boolean dispatcherRemoved = removeDispatcher(handshakeChannel, dispatcher, "during allow");
            if (dispatcherRemoved && dispatcher.manager.channel()
                .isActive()) {
                try {
                    dispatcher.completeHandshake(target);
                } catch (RuntimeException exception) {
                    LOGGER.warn("Pre-join handshake continuation failed", exception);
                    dispatcher.manager.channel()
                        .close();
                }
            } else {
                dispatcher.manager.channel()
                    .close();
            }
        }

        @Override
        public void disconnect(IChatComponent reason) {
            if (terminal) return;
            terminal = true;
            INSTANCE.transports.remove(attempt);
            dispatcher.manager.channel()
                .attr(ATTEMPT)
                .compareAndSet(attempt, null);
            removeDispatcher(handshakeChannel, dispatcher, "before disconnect");
            dispatcher.manager.closeChannel(reason);
        }

        private void clearCurrent() {
            if (sessionId == null) return;
            UUID previousSessionId = sessionId;
            int previousRevision = revision;
            sessionId = null;
            revision = 0;
            PreJoinNetwork.send(dispatcher, PreJoinPacket.clear(previousSessionId, previousRevision));
        }
    }

    private static boolean removeDispatcher(Channel channel, NetworkDispatcher expected, String phase) {
        io.netty.util.Attribute<NetworkDispatcher> attribute = channel.attr(NetworkDispatcher.FML_DISPATCHER);
        if (attribute.compareAndSet(expected, null)) return true;
        LOGGER.debug("Pre-join dispatcher changed {}", phase);
        return false;
    }

    private static void failClosed(ChannelHandlerContext context, NetworkDispatcher dispatcher, String phase) {
        removeDispatcher(context.channel(), dispatcher, phase);
        dispatcher.manager.closeChannel(new ChatComponentTranslation("necrotempus.prejoin.internal_error"));
    }
}
