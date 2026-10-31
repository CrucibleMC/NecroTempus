package io.github.cruciblemc.necrotempus.api.dialog;

import java.net.SocketAddress;
import java.util.UUID;
import java.util.function.BiConsumer;

import net.minecraft.util.IChatComponent;

import com.mojang.authlib.GameProfile;

public interface PreJoinConnection {

    UUID getConnectionId();

    GameProfile getProfile();

    SocketAddress getRemoteAddress();

    boolean supportsDialogs();

    void show(Dialog dialog, BiConsumer<PreJoinConnection, DialogResponse> callback);

    void allow();

    void disconnect(IChatComponent reason);
}
