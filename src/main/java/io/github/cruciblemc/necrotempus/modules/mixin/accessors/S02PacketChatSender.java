package io.github.cruciblemc.necrotempus.modules.mixin.accessors;

import java.util.UUID;

public interface S02PacketChatSender {

    UUID necrotempus$getSenderUuid();

    void necrotempus$setSenderUuid(UUID senderUuid);

    String necrotempus$getSenderName();

    void necrotempus$setSenderName(String senderName);

}
