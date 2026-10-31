package io.github.cruciblemc.necrotempus.api.dialog;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;

import net.minecraft.entity.player.EntityPlayerMP;

import io.github.cruciblemc.necrotempus.modules.features.dialog.DialogCodec;
import io.github.cruciblemc.necrotempus.modules.features.dialog.server.DialogMenuService;
import io.github.cruciblemc.necrotempus.modules.features.dialog.server.DialogSessions;
import io.github.cruciblemc.necrotempus.modules.features.packet.NTClientPacketHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public final class DialogManager {

    private static final Pattern DIALOG_ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9/._-]+");
    private static final ConcurrentHashMap<String, Dialog> REGISTERED = new ConcurrentHashMap<>();

    public enum ShowResult {
        SHOWN,
        INVALID_PLAYER,
        UNSUPPORTED_CLIENT,
        BUSY_CONTAINER,
        UNSUPPORTED_DIALOG
    }

    private DialogManager() {}

    public static boolean supports(EntityPlayerMP player) {
        return player != null && player.playerNetServerHandler != null
            && NTClientPacketHandler.supportsDialogs(player.playerNetServerHandler);
    }

    public static Dialog register(String id, Dialog dialog) {
        if (dialog == null) throw new IllegalArgumentException("Dialog is required");
        return register(id, dialog, dialog.getCommandExecutionPolicy());
    }

    /** Register with a command policy; compatibility confirmation remains the default. */
    public static Dialog register(String id, Dialog dialog, Dialog.CommandExecutionPolicy commandPolicy) {
        requireId(id);
        if (commandPolicy == null) throw new IllegalArgumentException("Command execution policy is required");
        if (dialog == null) throw new IllegalArgumentException("Dialog is required");
        Dialog configured = dialog.withCommandExecutionPolicy(commandPolicy);
        validate(configured);
        Dialog previous = REGISTERED.put(id, configured);
        DialogMenuService.refreshTagsForDialog(id);
        return previous;
    }

    public static boolean unregister(String id) {
        requireId(id);
        if (REGISTERED.remove(id) == null) return false;
        DialogMenuService.refreshTagsForDialog(id);
        return true;
    }

    /** Replace a named legacy dialog tag. Tag members may be registered before or after their definitions. */
    public static List<String> registerTag(String id, List<String> dialogIds) {
        return DialogMenuService.registerTag(id, dialogIds);
    }

    public static boolean unregisterTag(String id) {
        return DialogMenuService.unregisterTag(id);
    }

    public static Dialog getRegistered(String id) {
        requireId(id);
        return REGISTERED.get(id);
    }

    /** Configure this player's pause-menu and quick-action dialog IDs on the server thread. */
    public static void setMenu(EntityPlayerMP player, List<String> pauseIds, List<String> quickActionIds) {
        DialogMenuService.setMenu(player, pauseIds, quickActionIds);
    }

    /** Keep named pause and quick-action tags live for this player, preserving their declared order. */
    public static void setMenuTags(EntityPlayerMP player, List<String> pauseTagIds, List<String> quickActionTagIds) {
        DialogMenuService.setMenuTags(player, pauseTagIds, quickActionTagIds);
    }

    /** Configure this player's server-link list on the server thread. An empty list removes it. */
    public static void setServerLinks(EntityPlayerMP player, List<Dialog.ServerLink> links) {
        DialogMenuService.setServerLinks(player, links);
    }

    public static ShowResult show(EntityPlayerMP player, String id,
        BiConsumer<EntityPlayerMP, DialogResponse> callback) {
        Dialog dialog = REGISTERED.get(id);
        return dialog == null ? ShowResult.UNSUPPORTED_DIALOG : show(player, dialog, callback);
    }

    /** Clear the player's active dialog without invoking its cancel action. Call on the server thread. */
    public static void clear(EntityPlayerMP player) {
        DialogSessions.clear(player);
    }

    /** Call on the server thread; the response callback also runs there. */
    public static ShowResult show(EntityPlayerMP player, Dialog dialog,
        BiConsumer<EntityPlayerMP, DialogResponse> callback) {
        if (player == null) return ShowResult.INVALID_PLAYER;
        if (dialog == null) return ShowResult.UNSUPPORTED_DIALOG;
        if (!supports(player)) return ShowResult.UNSUPPORTED_CLIENT;
        if (player.openContainer != player.inventoryContainer) return ShowResult.BUSY_CONTAINER;
        try {
            dialog = DialogMenuService.expandDialogListTags(dialog);
            validate(dialog);
        } catch (IllegalArgumentException exception) {
            return ShowResult.UNSUPPORTED_DIALOG;
        }
        return DialogSessions.show(player, dialog, callback);
    }

    private static void validate(Dialog dialog) {
        dialog = DialogMenuService.expandDialogListTags(dialog);
        ByteBuf validation = Unpooled.buffer();
        try {
            DialogCodec.write(validation, dialog);
        } finally {
            validation.release();
        }
    }

    static void requireId(String id) {
        if (id == null || id.length() > 128
            || !DIALOG_ID.matcher(id)
                .matches()) {
            throw new IllegalArgumentException("Dialog IDs must be namespaced");
        }
    }
}
