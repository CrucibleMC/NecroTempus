package io.github.cruciblemc.necrotempus.modules.features.dialog.server;

import net.minecraft.server.MinecraftServer;

/** Minimal thread bridge used by the MinecraftServer lifecycle mixin. */
public final class PreJoinServerLifecycle {

    private static volatile boolean integratedServer;

    private PreJoinServerLifecycle() {}

    public static void serverStarting(MinecraftServer server) {
        bindThread(server.isDedicatedServer(), Thread.currentThread());
        PreJoinServerAdapter.serverStarting();
    }

    public static void serverStopped() {
        PreJoinServerAdapter.stop();
        integratedServer = false;
        PreJoinConnections.bindServerThread(null);
    }

    static boolean bindThread(boolean dedicated, Thread thread) {
        integratedServer = !dedicated;
        PreJoinConnections.bindServerThread(dedicated ? thread : null);
        return dedicated && PreJoinConnections.isServerThread();
    }

    static boolean isIntegratedServer() {
        return integratedServer;
    }
}
