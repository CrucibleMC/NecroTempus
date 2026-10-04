package io.github.cruciblemc.necrotempus.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.IChatComponent;

public final class ChatSenderContext {

    private static final ThreadLocal<SenderState> SENDER = new ThreadLocal<>();

    private ChatSenderContext() {}

    public static Optional<UUID> currentSenderUuid() {
        SenderState state = SENDER.get();
        return state == null ? null : Optional.ofNullable(state.senderUuid);
    }

    public static String currentSenderName() {
        SenderState state = SENDER.get();
        return state == null ? null : state.senderName;
    }

    public static String currentSenderDisplayName() {
        SenderState state = SENDER.get();
        return state == null ? null : state.senderDisplayName;
    }

    public static boolean isChatMessage() {
        SenderState state = SENDER.get();
        return state != null && state.chatMessage;
    }

    public static void setSender(UUID senderUuid, String senderName) {
        setSender(senderUuid, senderName, null);
    }

    public static void setSender(UUID senderUuid, String senderName, String displayName) {
        setReceivedSender(senderUuid, senderName, displayName, true);
    }

    public static void setReceivedSender(UUID senderUuid, String senderName, boolean chatMessage) {
        setReceivedSender(senderUuid, senderName, null, chatMessage);
    }

    public static void setReceivedSender(UUID senderUuid, String senderName, String displayName, boolean chatMessage) {
        SENDER.set(new SenderState(senderUuid, senderName, displayName, chatMessage));
    }

    public static void setSender(EntityPlayer player, String senderName) {
        if (player == null) clear();
        else setSender(player.getUniqueID(), senderName);
    }

    public static void withSender(EntityPlayer player, Runnable action) {
        withSender(player, null, action);
    }

    public static void withSender(EntityPlayer player, String displayName, Runnable action) {
        Snapshot previous = snapshot();
        if (player == null) clear();
        else setSender(player.getUniqueID(), player.getCommandSenderName(), displayName);
        try {
            action.run();
        } finally {
            restore(previous);
        }
    }

    public static void setHeads(List<ChatHead> heads) {
        SenderState previous = SENDER.get();
        SenderState state = previous == null ? new SenderState(null, null, null, false)
            : new SenderState(
                previous.senderUuid,
                previous.senderName,
                previous.senderDisplayName,
                previous.chatMessage);
        state.heads = Collections.unmodifiableList(new ArrayList<>(heads));
        SENDER.set(state);
    }

    public static List<ChatHead> currentHeads() {
        SenderState state = SENDER.get();
        return state == null ? Collections.emptyList() : state.heads;
    }

    public static int wrappingHeadCount(int visibleLength) {
        SenderState state = SENDER.get();
        if (state == null) return 0;
        int count = 0;
        for (ChatHead head : state.heads) {
            if (head.offset >= state.wrapStart && head.offset < state.wrapStart + visibleLength) count++;
        }
        return count;
    }

    public static void advanceWrapping(int visibleLength) {
        SenderState state = SENDER.get();
        if (state != null) state.wrapStart += visibleLength;
    }

    public static List<ChatHead> getLineHeads(IChatComponent component) {
        SenderState state = SENDER.get();
        if (state == null || state.heads.isEmpty()) return Collections.emptyList();
        int start = state.nextLineStart;
        int length = ChatFormattingUtils.visibleCharacterCount(component.getUnformattedText());
        state.nextLineStart += length;
        List<ChatHead> lineHeads = new ArrayList<>();
        for (ChatHead head : state.heads) {
            if (head.offset >= start && head.offset < start + length) {
                lineHeads.add(new ChatHead(head.profile, head.offset - start));
            }
        }
        return Collections.unmodifiableList(lineHeads);
    }

    public static Snapshot snapshot() {
        return new Snapshot(SENDER.get());
    }

    public static void restore(Snapshot snapshot) {
        if (snapshot == null || snapshot.state == null) SENDER.remove();
        else SENDER.set(snapshot.state);
    }

    public static void clear() {
        SENDER.remove();
    }

    public static final class Snapshot {

        private final SenderState state;

        private Snapshot(SenderState state) {
            this.state = state;
        }
    }

    private static final class SenderState {

        private final UUID senderUuid;
        private final String senderName;
        private final String senderDisplayName;
        private final boolean chatMessage;
        private List<ChatHead> heads = Collections.emptyList();
        private int nextLineStart;
        private int wrapStart;

        private SenderState(UUID senderUuid, String senderName, String senderDisplayName, boolean chatMessage) {
            this.senderUuid = senderUuid;
            this.senderName = senderName;
            this.senderDisplayName = senderDisplayName;
            this.chatMessage = chatMessage;
        }
    }
}
