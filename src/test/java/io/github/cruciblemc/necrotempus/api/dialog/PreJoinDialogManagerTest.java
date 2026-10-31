package io.github.cruciblemc.necrotempus.api.dialog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Method;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

class PreJoinDialogManagerTest {

    @Test
    void exposesOneHandlerRegistrationReturningAnAutoCloseableToken() throws Exception {
        Method method = PreJoinDialogManager.class.getMethod("register", Consumer.class);
        assertEquals(AutoCloseable.class, method.getReturnType());
    }

    @Test
    void publicRegistrationFailsOutsideTheServerThread() {
        assertThrows(IllegalStateException.class, () -> PreJoinDialogManager.register(connection -> {}));
    }
}
