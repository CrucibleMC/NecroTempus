package io.github.cruciblemc.necrotempus.modules.mixin.accessors;

import java.util.UUID;

import net.minecraft.util.IChatComponent;

public interface S02PacketChatSender {

    IChatComponent necrotempus$getChatComponent();

    UUID necrotempus$getSenderUuid();

    void necrotempus$setSenderUuid(UUID senderUuid);

    String necrotempus$getSenderName();

    void necrotempus$setSenderName(String senderName);

    String necrotempus$getSenderDisplayName();

    void necrotempus$setSenderDisplayName(String displayName);

}
