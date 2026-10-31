package io.github.cruciblemc.necrotempus.modules.features.dialog.server;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

class PreJoinServerLifecycleTest {

    @Test
    void bindsOnlyDedicatedServerThreadAndClearsForIntegratedServer() throws Exception {
        Thread previousServerThread = serverThread();
        boolean previousIntegratedServer = PreJoinServerLifecycle.isIntegratedServer();
        try {
            Thread dedicatedThread = Thread.currentThread();
            assertTrue(PreJoinServerLifecycle.bindThread(true, dedicatedThread));
            assertTrue(PreJoinConnections.isServerThread());

            Thread other = new Thread();
            assertFalse(PreJoinServerLifecycle.bindThread(true, other));
            assertFalse(PreJoinConnections.isServerThread());

            assertFalse(PreJoinServerLifecycle.bindThread(false, dedicatedThread));
            assertFalse(PreJoinConnections.isServerThread());
        } finally {
            PreJoinServerLifecycle.bindThread(!previousIntegratedServer, previousServerThread);
        }
    }

    @Test
    void registrationExplainsThatIntegratedServersAreUnsupported() throws Exception {
        Thread previousServerThread = serverThread();
        boolean previousIntegratedServer = PreJoinServerLifecycle.isIntegratedServer();
        try {
            PreJoinServerLifecycle.bindThread(false, Thread.currentThread());

            IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> io.github.cruciblemc.necrotempus.api.dialog.PreJoinDialogManager.register(connection -> {}));

            assertTrue(
                exception.getMessage()
                    .contains("integrated"),
                exception.getMessage());
        } finally {
            PreJoinServerLifecycle.bindThread(!previousIntegratedServer, previousServerThread);
        }
    }

    private static Thread serverThread() throws ReflectiveOperationException {
        Field field = PreJoinConnections.class.getDeclaredField("serverThread");
        field.setAccessible(true);
        return (Thread) field.get(null);
    }
}
