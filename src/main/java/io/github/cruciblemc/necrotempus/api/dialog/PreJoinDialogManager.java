package io.github.cruciblemc.necrotempus.api.dialog;

import java.util.Objects;
import java.util.function.Consumer;

import io.github.cruciblemc.necrotempus.modules.features.dialog.server.PreJoinConnections;

public final class PreJoinDialogManager {

    private PreJoinDialogManager() {}

    public static AutoCloseable register(Consumer<PreJoinConnection> handler) {
        return PreJoinConnections.register(Objects.requireNonNull(handler, "handler"));
    }
}
