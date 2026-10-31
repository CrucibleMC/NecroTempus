package io.github.cruciblemc.necrotempus.modules.features.dialog.compat.crafttweaker;

import java.util.Arrays;
import java.util.Locale;

import net.minecraft.event.ClickEvent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

import io.github.cruciblemc.necrotempus.NecroTempus;
import io.github.cruciblemc.necrotempus.api.dialog.Dialog;
import io.github.cruciblemc.necrotempus.api.dialog.DialogManager;
import minetweaker.IUndoableAction;
import minetweaker.MineTweakerAPI;
import minetweaker.annotations.ModOnly;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenClass("necrotempus.crafttweaker.Dialogs")
@ModOnly(NecroTempus.MODID)
public final class Dialogs {

    private Dialogs() {}

    @ZenMethod
    public static void registerNotice(String id, String title, String body, String button) {
        Dialog dialog = Dialog.notice(new ChatComponentText(title))
            .body(new Dialog.TextBody(new ChatComponentText(body)))
            .action(new Dialog.ActionButton(new ChatComponentText(button), Dialog.Action.none()))
            .build();
        MineTweakerAPI.apply(new RegisterAction(id, dialog));
    }

    @ZenMethod
    public static void registerLinkedNotice(String id, String title, String before, String link, String url,
        String after, String button) {
        ChatComponentText linkedText = new ChatComponentText(link);
        linkedText.setChatStyle(
            new ChatStyle().setColor(EnumChatFormatting.BLUE)
                .setUnderlined(true)
                .setChatClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url)));
        IChatComponent body = new ChatComponentText(before).appendSibling(linkedText)
            .appendText(after);
        Dialog dialog = Dialog.notice(new ChatComponentText(title))
            .body(new Dialog.TextBody(body))
            .action(new Dialog.ActionButton(new ChatComponentText(button), Dialog.Action.none()))
            .build();
        MineTweakerAPI.apply(new RegisterAction(id, dialog));
    }

    @ZenMethod
    public static void registerItemNotice(String id, String title, String itemId, int metadata, int count,
        String description, String button) {
        if (itemId == null || itemId.isEmpty()) throw new IllegalArgumentException("Dialog item id is required");
        Item item = (Item) Item.itemRegistry.getObject(itemId);
        if (item == null && itemId.startsWith("minecraft:")) {
            item = (Item) Item.itemRegistry.getObject(itemId.substring("minecraft:".length()));
        }
        if (item == null) throw new IllegalArgumentException("Unknown dialog item: " + itemId);
        if (metadata < Short.MIN_VALUE || metadata > Short.MAX_VALUE || count < 1 || count > Byte.MAX_VALUE) {
            throw new IllegalArgumentException("Dialog item metadata or count is outside the network range");
        }
        Dialog dialog = Dialog.notice(new ChatComponentText(title))
            .body(new Dialog.ItemBody(new ItemStack(item, count, metadata), new ChatComponentText(description)))
            .action(new Dialog.ActionButton(new ChatComponentText(button), Dialog.Action.none()))
            .build();
        MineTweakerAPI.apply(new RegisterAction(id, dialog));
    }

    @ZenMethod
    public static void registerCustomNotice(String id, String title, String body, String actionId, String button) {
        Dialog dialog = Dialog.notice(new ChatComponentText(title))
            .body(new Dialog.TextBody(new ChatComponentText(body)))
            .action(new Dialog.ActionButton(new ChatComponentText(button), Dialog.Action.custom(actionId, null)))
            .build();
        MineTweakerAPI.apply(new RegisterAction(id, dialog));
    }

    @ZenMethod
    public static void registerNavigateNotice(String id, String title, String body, String targetId, String button) {
        Dialog dialog = Dialog.notice(new ChatComponentText(title))
            .body(new Dialog.TextBody(new ChatComponentText(body)))
            .action(new Dialog.ActionButton(new ChatComponentText(button), Dialog.Action.showDialog(targetId)))
            .build();
        MineTweakerAPI.apply(new RegisterAction(id, dialog));
    }

    @ZenMethod
    public static void registerServerLinks(String id, String title, String body, String[] labels, String[] urls) {
        if (labels == null || urls == null || labels.length == 0 || labels.length != urls.length || labels.length > 8) {
            throw new IllegalArgumentException("Server-link labels and URLs must have the same length from 1 to 8");
        }
        Dialog.Builder builder = Dialog.serverLinks(new ChatComponentText(title))
            .body(new Dialog.TextBody(new ChatComponentText(body)));
        for (int i = 0; i < labels.length; i++) {
            builder.serverLink(new Dialog.ServerLink(new ChatComponentText(labels[i]), urls[i]));
        }
        MineTweakerAPI.apply(new RegisterAction(id, builder.build()));
    }

    @ZenMethod
    public static void registerSuggestNotice(String id, String title, String body, String command, String button) {
        Dialog dialog = Dialog.notice(new ChatComponentText(title))
            .body(new Dialog.TextBody(new ChatComponentText(body)))
            .action(new Dialog.ActionButton(new ChatComponentText(button), Dialog.Action.suggestCommand(command)))
            .build();
        MineTweakerAPI.apply(new RegisterAction(id, dialog));
    }

    @ZenMethod
    public static void registerClipboardNotice(String id, String title, String body, String value, String button) {
        Dialog dialog = Dialog.notice(new ChatComponentText(title))
            .body(new Dialog.TextBody(new ChatComponentText(body)))
            .action(new Dialog.ActionButton(new ChatComponentText(button), Dialog.Action.copyToClipboard(value)))
            .build();
        MineTweakerAPI.apply(new RegisterAction(id, dialog));
    }

    @ZenMethod
    public static void registerUrlNotice(String id, String title, String body, String url, String button) {
        Dialog dialog = Dialog.notice(new ChatComponentText(title))
            .body(new Dialog.TextBody(new ChatComponentText(body)))
            .action(new Dialog.ActionButton(new ChatComponentText(button), Dialog.Action.openUrl(url)))
            .build();
        MineTweakerAPI.apply(new RegisterAction(id, dialog));
    }

    @ZenMethod
    public static void registerWaitingNotice(String id, String title, String body, String button) {
        Dialog dialog = Dialog.notice(new ChatComponentText(title))
            .body(new Dialog.TextBody(new ChatComponentText(body)))
            .afterAction(Dialog.AfterAction.WAIT_FOR_RESPONSE)
            .action(new Dialog.ActionButton(new ChatComponentText(button), Dialog.Action.none()))
            .build();
        MineTweakerAPI.apply(new RegisterAction(id, dialog));
    }

    @ZenMethod
    public static void registerRepeatableNotice(String id, String title, String body, String button, int maxUses) {
        Dialog dialog = Dialog.notice(new ChatComponentText(title))
            .body(new Dialog.TextBody(new ChatComponentText(body)))
            .pause(false)
            .afterAction(Dialog.AfterAction.NONE)
            .action(
                new Dialog.ActionButton(
                    new ChatComponentText(button),
                    Dialog.Action.none()
                        .withMaxUses(maxUses)))
            .build();
        MineTweakerAPI.apply(new RegisterAction(id, dialog));
    }

    @ZenMethod
    public static void registerDynamicCommandNotice(String id, String title, String body, String inputKey,
        String inputLabel, String initial, int maxLength, String template, String button) {
        registerDynamicCommandNotice(
            id,
            title,
            body,
            inputKey,
            inputLabel,
            initial,
            maxLength,
            template,
            Dialog.CommandExecutionPolicy.COMPATIBILITY_CONFIRM.name(),
            button);
    }

    @ZenMethod
    public static void registerDynamicCommandNotice(String id, String title, String body, String inputKey,
        String inputLabel, String initial, int maxLength, String template, String commandPolicy, String button) {
        if (commandPolicy == null) throw new IllegalArgumentException("Command execution policy is required");
        Dialog.CommandExecutionPolicy policy;
        try {
            policy = Dialog.CommandExecutionPolicy.valueOf(
                commandPolicy.trim()
                    .toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                "Command policy must be COMPATIBILITY_CONFIRM or NO_CONFIRMATION",
                exception);
        }
        Dialog dialog = Dialog.notice(new ChatComponentText(title))
            .body(new Dialog.TextBody(new ChatComponentText(body)))
            .input(new Dialog.TextInput(inputKey, new ChatComponentText(inputLabel), initial, maxLength, 1))
            .action(new Dialog.ActionButton(new ChatComponentText(button), Dialog.Action.dynamicCommand(template)))
            .build();
        MineTweakerAPI.apply(new RegisterAction(id, dialog, policy));
    }

    @ZenMethod
    public static void registerTextInputNotice(String id, String title, String body, String inputKey, String inputLabel,
        String initial, int maxLength, String button) {
        Dialog dialog = Dialog.notice(new ChatComponentText(title))
            .body(new Dialog.TextBody(new ChatComponentText(body)))
            .input(new Dialog.TextInput(inputKey, new ChatComponentText(inputLabel), initial, maxLength, 1))
            .action(
                new Dialog.ActionButton(
                    new ChatComponentText(button),
                    Dialog.Action.dynamicCustom("necrotempus:form_submit", new NBTTagCompound())))
            .build();
        MineTweakerAPI.apply(new RegisterAction(id, dialog));
    }

    @ZenMethod
    public static void registerMultilineTextInputNotice(String id, String title, String body, String inputKey,
        String inputLabel, String initial, int maxLength, int maxLines, String button) {
        Dialog dialog = Dialog.notice(new ChatComponentText(title))
            .body(new Dialog.TextBody(new ChatComponentText(body)))
            .input(new Dialog.TextInput(inputKey, new ChatComponentText(inputLabel), initial, maxLength, maxLines))
            .action(
                new Dialog.ActionButton(
                    new ChatComponentText(button),
                    Dialog.Action.dynamicCustom("necrotempus:form_submit", new NBTTagCompound())))
            .build();
        MineTweakerAPI.apply(new RegisterAction(id, dialog));
    }

    @ZenMethod
    public static void registerConfirmation(String id, String title, String body, String confirm, String cancel) {
        Dialog dialog = Dialog.confirmation(new ChatComponentText(title))
            .body(new Dialog.TextBody(new ChatComponentText(body)))
            .action(new Dialog.ActionButton(new ChatComponentText(confirm), Dialog.Action.none()))
            .action(new Dialog.ActionButton(new ChatComponentText(cancel), Dialog.Action.none()))
            .build();
        MineTweakerAPI.apply(new RegisterAction(id, dialog));
    }

    @ZenMethod
    public static void registerMultiAction(String id, String title, String body, String[] actions) {
        Dialog.Builder builder = Dialog.multiAction(new ChatComponentText(title))
            .body(new Dialog.TextBody(new ChatComponentText(body)));
        for (String action : actions) {
            builder.action(new Dialog.ActionButton(new ChatComponentText(action), Dialog.Action.none()));
        }
        MineTweakerAPI.apply(new RegisterAction(id, builder.build()));
    }

    @ZenMethod
    public static void registerDialogList(String id, String title, String externalTitle, String body, String[] labels,
        String[] dialogIds) {
        if (labels == null || dialogIds == null || labels.length != dialogIds.length || labels.length == 0) {
            throw new IllegalArgumentException("Dialog list labels and targets must have the same non-empty length");
        }
        Dialog.Builder builder = Dialog.dialogList(new ChatComponentText(title))
            .externalTitle(new ChatComponentText(externalTitle))
            .body(new Dialog.TextBody(new ChatComponentText(body)));
        for (int i = 0; i < labels.length; i++) {
            builder.entry(new Dialog.Entry(new ChatComponentText(labels[i]), dialogIds[i]));
        }
        MineTweakerAPI.apply(new RegisterAction(id, builder.build()));
    }

    @ZenMethod
    public static void registerTaggedDialogList(String id, String title, String externalTitle, String body,
        String[] tagIds) {
        if (tagIds == null || tagIds.length == 0) throw new IllegalArgumentException("Dialog list tags are required");
        Dialog.Builder builder = Dialog.dialogList(new ChatComponentText(title))
            .externalTitle(new ChatComponentText(externalTitle))
            .body(new Dialog.TextBody(new ChatComponentText(body)));
        for (String tagId : tagIds) builder.entryTag(tagId);
        MineTweakerAPI.apply(new RegisterAction(id, builder.build()));
    }

    @ZenMethod
    public static void registerDialogTag(String id, String[] dialogIds) {
        if (dialogIds == null) throw new IllegalArgumentException("Dialog tag entries are required");
        MineTweakerAPI.apply(new RegisterTagAction(id, Arrays.asList(dialogIds)));
    }

    public static final class RegisterAction implements IUndoableAction {

        private final String id;
        private final Dialog dialog;
        private final Dialog.CommandExecutionPolicy commandPolicy;
        private Dialog previous;

        public RegisterAction(String id, Dialog dialog) {
            this(id, dialog, dialog.getCommandExecutionPolicy());
        }

        public RegisterAction(String id, Dialog dialog, Dialog.CommandExecutionPolicy commandPolicy) {
            this.id = id;
            this.dialog = dialog;
            if (commandPolicy == null) throw new IllegalArgumentException("Command execution policy is required");
            this.commandPolicy = commandPolicy;
        }

        @Override
        public void apply() {
            previous = DialogManager.register(id, dialog, commandPolicy);
        }

        @Override
        public boolean canUndo() {
            return true;
        }

        @Override
        public void undo() {
            if (previous == null) DialogManager.unregister(id);
            else DialogManager.register(id, previous);
        }

        @Override
        public String describe() {
            return "Registering dialog " + id + ".";
        }

        @Override
        public String describeUndo() {
            return "Removing dialog " + id + ".";
        }

        @Override
        public Object getOverrideKey() {
            return null;
        }
    }

    public static final class RegisterTagAction implements IUndoableAction {

        private final String id;
        private final java.util.List<String> dialogIds;
        private java.util.List<String> previous;

        public RegisterTagAction(String id, java.util.List<String> dialogIds) {
            this.id = id;
            this.dialogIds = new java.util.ArrayList<>(dialogIds);
        }

        @Override
        public void apply() {
            previous = DialogManager.registerTag(id, dialogIds);
        }

        @Override
        public boolean canUndo() {
            return true;
        }

        @Override
        public void undo() {
            if (previous == null) DialogManager.unregisterTag(id);
            else DialogManager.registerTag(id, previous);
        }

        @Override
        public String describe() {
            return "Registering dialog tag " + id + ".";
        }

        @Override
        public String describeUndo() {
            return "Removing dialog tag " + id + ".";
        }

        @Override
        public Object getOverrideKey() {
            return null;
        }
    }
}
