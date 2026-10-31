package io.github.cruciblemc.necrotempus.modules.features.dialog;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTSizeTracker;
import net.minecraft.util.IChatComponent;

import io.github.cruciblemc.necrotempus.api.dialog.Dialog;
import io.github.cruciblemc.necrotempus.api.dialog.DialogBodyActions;
import io.netty.buffer.ByteBuf;

public final class DialogCodec {

    public static final int MAX_PACKET_BYTES = 65515;
    private static final int MAX_COMPONENT_BYTES = 32767;
    private static final int MAX_BODY_ELEMENTS = 255;
    private static final int MAX_ACTIONS = 255;
    private static final int MAX_LIST_ENTRIES = 255;
    private static final int MAX_INPUTS = 255;
    private static final int MAX_OPTIONS = 255;
    private static final int MAX_SERVER_LINKS = 255;
    private static final int MAX_INPUT_STRING_BYTES = 4096;
    private static final int MAX_ACTION_VALUE_BYTES = 4096;
    private static final int MAX_ITEM_NBT_BYTES = 262144;
    private static final int MAX_ITEM_NBT_TAGS = 4096;
    private static final int MAX_ITEM_NBT_DEPTH = 16;
    private static final int VERSION = 12;

    private DialogCodec() {}

    public static void write(ByteBuf buffer, Dialog dialog) {
        int start = buffer.writerIndex();
        try {
            requireSupported(dialog);
            buffer.writeByte(VERSION);
            buffer.writeByte(
                dialog.getType()
                    .ordinal());
            buffer.writeByte((dialog.canCloseWithEscape() ? 1 : 0) | (dialog.pausesGame() ? 2 : 0));
            buffer.writeByte(
                dialog.getCommandExecutionPolicy()
                    .ordinal());
            buffer.writeByte(
                dialog.getAfterAction()
                    .ordinal());
            buffer.writeInt(dialog.getColumns());
            buffer.writeShort(dialog.getButtonWidth());
            writeComponent(buffer, dialog.getTitle());
            buffer.writeBoolean(dialog.getExternalTitle() != null);
            if (dialog.getExternalTitle() != null) writeComponent(buffer, dialog.getExternalTitle());
            buffer.writeByte(
                dialog.getBody()
                    .size());
            for (Dialog.Body body : dialog.getBody()) writeBody(buffer, body);
            buffer.writeByte(
                dialog.getInputs()
                    .size());
            for (Dialog.Input input : dialog.getInputs()) writeInput(buffer, input);
            if (dialog.getType() == Dialog.Type.SERVER_LINKS) {
                buffer.writeByte(
                    dialog.getServerLinks()
                        .size());
                for (Dialog.ServerLink link : dialog.getServerLinks()) {
                    writeComponent(buffer, link.getLabel());
                    writeClientAction(buffer, Dialog.Action.openUrl(link.getUrl()));
                }
            } else {
                buffer.writeByte(
                    dialog.getActions()
                        .size());
                for (Dialog.ActionButton action : dialog.getActions()) {
                    writeActionButton(buffer, action);
                }
            }
            buffer.writeByte(
                dialog.getEntries()
                    .size());
            for (Dialog.Entry entry : dialog.getEntries()) {
                writeComponent(buffer, entry.getLabel());
            }
            buffer.writeBoolean(dialog.getExitAction() != null);
            if (dialog.getExitAction() != null) {
                writeActionButton(buffer, dialog.getExitAction());
            }
            if (buffer.writerIndex() - start > MAX_PACKET_BYTES)
                throw new IllegalArgumentException("Dialog is too large");
        } catch (RuntimeException exception) {
            buffer.writerIndex(start);
            throw exception;
        }
    }

    public static Dialog read(ByteBuf buffer) {
        int start = buffer.readerIndex();
        if (buffer.readableBytes() > MAX_PACKET_BYTES) throw new IllegalArgumentException("Dialog is too large");
        try {
            if (buffer.readUnsignedByte() != VERSION) throw new IllegalArgumentException("Unsupported dialog version");
            int typeId = buffer.readUnsignedByte();
            if (typeId >= Dialog.Type.values().length) throw new IllegalArgumentException("Invalid dialog type");
            Dialog.Type type = Dialog.Type.values()[typeId];
            int flags = buffer.readUnsignedByte();
            if ((flags & ~3) != 0) throw new IllegalArgumentException("Invalid dialog flags");
            int commandPolicyId = buffer.readUnsignedByte();
            if (commandPolicyId >= Dialog.CommandExecutionPolicy.values().length) {
                throw new IllegalArgumentException("Invalid command execution policy");
            }
            Dialog.CommandExecutionPolicy commandPolicy = Dialog.CommandExecutionPolicy.values()[commandPolicyId];
            int afterActionId = buffer.readUnsignedByte();
            if (afterActionId >= Dialog.AfterAction.values().length) {
                throw new IllegalArgumentException("Invalid dialog after action");
            }
            Dialog.AfterAction afterAction = Dialog.AfterAction.values()[afterActionId];
            int columns = buffer.readInt();
            int buttonWidth = buffer.readUnsignedShort();
            IChatComponent title = readComponent(buffer);
            IChatComponent externalTitle = readBoolean(buffer) ? readComponent(buffer) : null;
            int bodyCount = buffer.readUnsignedByte();
            if (bodyCount > MAX_BODY_ELEMENTS) throw new IllegalArgumentException("Too many dialog body elements");
            List<Dialog.Body> bodies = new ArrayList<>(bodyCount);
            for (int i = 0; i < bodyCount; i++) bodies.add(readBody(buffer));
            int inputCount = buffer.readUnsignedByte();
            if (inputCount > MAX_INPUTS) throw new IllegalArgumentException("Too many dialog inputs");
            List<Dialog.Input> inputs = new ArrayList<>(inputCount);
            for (int i = 0; i < inputCount; i++) inputs.add(readInput(buffer));
            int actionCount = buffer.readUnsignedByte();
            boolean list = type == Dialog.Type.DIALOG_LIST;
            boolean serverLinks = type == Dialog.Type.SERVER_LINKS;
            if (actionCount > MAX_ACTIONS || (!list && actionCount < 1) || (list && actionCount != 0)) {
                throw new IllegalArgumentException("Invalid action count");
            }
            IChatComponent[] actions = new IChatComponent[actionCount];
            IChatComponent[] actionTooltips = new IChatComponent[actionCount];
            int[] actionWidths = new int[actionCount];
            Dialog.Action[] actionData = new Dialog.Action[actionCount];
            for (int i = 0; i < actionCount; i++) {
                actions[i] = readComponent(buffer);
                if (!serverLinks) {
                    actionTooltips[i] = readOptionalComponent(buffer);
                    actionWidths[i] = buffer.readUnsignedShort();
                }
                actionData[i] = readClientAction(buffer);
            }
            int entryCount = buffer.readUnsignedByte();
            if (entryCount > MAX_LIST_ENTRIES || (list && entryCount < 1) || (!list && entryCount != 0)) {
                throw new IllegalArgumentException("Invalid dialog entry count");
            }
            List<Dialog.Entry> entries = new ArrayList<>(entryCount);
            for (int i = 0; i < entryCount; i++) {
                entries.add(Dialog.Entry.displayOnly(readComponent(buffer)));
            }
            IChatComponent exitAction = null;
            IChatComponent exitTooltip = null;
            int exitWidth = 150;
            Dialog.Action exitActionData = null;
            if (readBoolean(buffer)) {
                exitAction = readComponent(buffer);
                exitTooltip = readOptionalComponent(buffer);
                exitWidth = buffer.readUnsignedShort();
                exitActionData = readClientAction(buffer);
            }
            if (buffer.isReadable()) throw new IllegalArgumentException("Trailing dialog data");

            Dialog.Builder builder;
            switch (type) {
                case NOTICE:
                    builder = Dialog.notice(title);
                    break;
                case CONFIRMATION:
                    builder = Dialog.confirmation(title);
                    break;
                case MULTI_ACTION:
                    builder = Dialog.multiAction(title);
                    break;
                case DIALOG_LIST:
                    builder = Dialog.dialogList(title);
                    break;
                case SERVER_LINKS:
                    builder = Dialog.serverLinks(title);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported dialog type");
            }
            builder.externalTitle(externalTitle)
                .canCloseWithEscape((flags & 1) != 0)
                .pause((flags & 2) != 0)
                .commandExecutionPolicy(commandPolicy)
                .afterAction(afterAction)
                .columns(columns)
                .buttonWidth(buttonWidth);
            for (Dialog.Body body : bodies) builder.body(body);
            for (Dialog.Input input : inputs) builder.input(input);
            for (int i = 0; i < actions.length; i++) {
                if (serverLinks) {
                    if (actionData[i].getType() != Dialog.ActionType.OPEN_URL) {
                        throw new IllegalArgumentException("Server links require URL actions");
                    }
                    builder.serverLink(new Dialog.ServerLink(actions[i], actionData[i].getValue()));
                } else {
                    builder
                        .action(new Dialog.ActionButton(actions[i], actionData[i], actionTooltips[i], actionWidths[i]));
                }
            }
            for (Dialog.Entry entry : entries) builder.entry(entry);
            if (exitAction != null) {
                builder.exitAction(new Dialog.ActionButton(exitAction, exitActionData, exitTooltip, exitWidth));
            }
            Dialog result = builder.build();
            requireSupported(result, false);
            return result;
        } catch (IndexOutOfBoundsException exception) {
            buffer.readerIndex(start);
            throw new IllegalArgumentException("Truncated dialog data", exception);
        } catch (RuntimeException exception) {
            buffer.readerIndex(start);
            throw exception;
        }
    }

    public static void requireSupported(Dialog dialog) {
        requireSupported(dialog, true);
    }

    private static void requireSupported(Dialog dialog, boolean validateServerActions) {
        if (dialog == null) throw new IllegalArgumentException("Dialog is required");
        if (dialog.getType() == Dialog.Type.DIALOG_LIST && !dialog.getInputs()
            .isEmpty()) {
            throw new IllegalArgumentException(
                "Dialog lists cannot contain inputs until navigation preserves their values");
        }
        if (validateServerActions && dialog.getType() == Dialog.Type.DIALOG_LIST) {
            for (Dialog.Entry entry : dialog.getEntries()) {
                if (!entry.hasTarget()) throw new IllegalArgumentException("Server dialog entries require a target");
            }
        }
        if (dialog.getType() == Dialog.Type.SERVER_LINKS) {
            if (!dialog.getInputs()
                .isEmpty() || dialog.getServerLinks()
                    .isEmpty()
                || dialog.getServerLinks()
                    .size() > MAX_SERVER_LINKS) {
                throw new IllegalArgumentException("Server-link dialogs require at least one link and no inputs");
            }
            for (Dialog.ServerLink link : dialog.getServerLinks()) requireHttpUrl(link.getUrl());
        }
        if (validateServerActions && !dialog.getInputs()
            .isEmpty()) {
            for (Dialog.ActionButton action : dialog.getActions()) requireDynamicFormAction(action);
            if (dialog.getExitAction() != null) requireDynamicFormAction(dialog.getExitAction());
        }
        if (dialog.getBody()
            .size() > MAX_BODY_ELEMENTS
            || dialog.getInputs()
                .size() > MAX_INPUTS
            || dialog.getActions()
                .size() > MAX_ACTIONS
            || dialog.getEntries()
                .size() > MAX_LIST_ENTRIES) {
            throw new IllegalArgumentException("Dialog exceeds the legacy codec array limits");
        }
        long maxResponseBytes = 1;
        for (Dialog.Input input : dialog.getInputs()) {
            requireSupportedInput(input);
            maxResponseBytes += 5L + input.getKey()
                .length();
            if (input instanceof Dialog.TextInput) {
                maxResponseBytes += 4L + ((Dialog.TextInput) input).getMaxLength() * 4L;
            } else if (input instanceof Dialog.BooleanInput) {
                maxResponseBytes++;
            } else if (input instanceof Dialog.SingleOptionInput) {
                int maxOptionBytes = 0;
                for (Dialog.Option option : ((Dialog.SingleOptionInput) input).getOptions()) {
                    maxOptionBytes = Math.max(
                        maxOptionBytes,
                        option.getId()
                            .getBytes(StandardCharsets.UTF_8).length);
                }
                maxResponseBytes += 4L + maxOptionBytes;
            } else if (input instanceof Dialog.NumberRangeInput) {
                maxResponseBytes += 4;
            }
            if (maxResponseBytes > DialogFormValues.MAX_RESPONSE_BYTES) {
                throw new IllegalArgumentException("Maximum dialog form response exceeds the packet limit");
            }
        }
        for (Dialog.Body body : dialog.getBody()) {
            if (body instanceof Dialog.ItemBody) {
                Dialog.ItemBody itemBody = (Dialog.ItemBody) body;
                ItemStack item = itemBody.getItem();
                if (item.getItem() == null) throw new IllegalArgumentException("Dialog item must be registered");
                int itemId = Item.getIdFromItem(item.getItem());
                int damage = item.getItemDamage();
                if (itemId < 0 || itemId > Short.MAX_VALUE
                    || item.stackSize > Byte.MAX_VALUE
                    || damage < Short.MIN_VALUE
                    || damage > Short.MAX_VALUE) {
                    throw new IllegalArgumentException("Dialog item cannot be represented on the network");
                }
            } else if (!(body instanceof Dialog.TextBody)) {
                throw new IllegalArgumentException("Unsupported dialog body");
            }
        }
        for (Dialog.ActionButton action : dialog.getActions()) requireSupportedAction(action);
        if (dialog.getExitAction() != null) requireSupportedAction(dialog.getExitAction());
        for (Dialog.Action action : DialogBodyActions.collect(dialog)) requireSupportedAction(action);
    }

    private static void requireSupportedAction(Dialog.ActionButton button) {
        requireSupportedAction(button.getAction());
    }

    private static void requireSupportedAction(Dialog.Action action) {
        if (action.getType() == Dialog.ActionType.NONE || action.getType() == Dialog.ActionType.CUSTOM) return;
        if (action.getType() == Dialog.ActionType.SHOW_DIALOG) return;
        if (action.getType() == Dialog.ActionType.RUN_COMMAND) return;
        if (action.isDynamic()) throw new IllegalArgumentException("Dynamic client actions are not supported");
        if (action.getType() == Dialog.ActionType.SUGGEST_COMMAND) {
            String command = action.getValue();
            if (command.getBytes(StandardCharsets.UTF_8).length > MAX_ACTION_VALUE_BYTES || command.indexOf('\n') >= 0
                || command.indexOf('\r') >= 0) {
                throw new IllegalArgumentException("Invalid suggested command");
            }
            return;
        }
        if (action.getType() == Dialog.ActionType.COPY_TO_CLIPBOARD) {
            if (action.getValue()
                .getBytes(StandardCharsets.UTF_8).length > MAX_ACTION_VALUE_BYTES) {
                throw new IllegalArgumentException("Clipboard action is too large");
            }
            return;
        }
        if (action.getType() == Dialog.ActionType.OPEN_URL) {
            requireHttpUrl(action.getValue());
            return;
        }
        throw new IllegalArgumentException(
            "Only callback, URL, clipboard, and suggested-command actions are supported");
    }

    private static void writeClientAction(ByteBuf buffer, Dialog.Action action) {
        buffer.writeInt(action.getMaxUses());
        if (action.getType() == Dialog.ActionType.SUGGEST_COMMAND) {
            buffer.writeByte(1);
            writeString(buffer, action.getValue(), MAX_ACTION_VALUE_BYTES);
        } else if (action.getType() == Dialog.ActionType.COPY_TO_CLIPBOARD) {
            buffer.writeByte(2);
            writeString(buffer, action.getValue(), MAX_ACTION_VALUE_BYTES);
        } else if (action.getType() == Dialog.ActionType.OPEN_URL) {
            buffer.writeByte(3);
            writeString(buffer, action.getValue(), MAX_ACTION_VALUE_BYTES);
        } else if (action.getType() == Dialog.ActionType.RUN_COMMAND) {
            buffer.writeByte(action.isDynamic() ? 5 : 4);
            writeString(buffer, action.getValue(), MAX_ACTION_VALUE_BYTES);
        } else if (action.getType() == Dialog.ActionType.SHOW_DIALOG) {
            buffer.writeByte(6);
        } else if (action.getType() == Dialog.ActionType.CUSTOM && action.isDynamic()) {
            buffer.writeByte(7);
        } else {
            buffer.writeByte(0);
        }
    }

    private static void writeActionButton(ByteBuf buffer, Dialog.ActionButton button) {
        writeComponent(buffer, button.getLabel());
        writeOptionalComponent(buffer, button.getTooltip());
        buffer.writeShort(button.getWidth());
        writeClientAction(buffer, button.getAction());
    }

    private static Dialog.Action readClientAction(ByteBuf buffer) {
        int maxUses = buffer.readInt();
        if (maxUses < 1) throw new IllegalArgumentException("Invalid action use limit");
        int type = buffer.readUnsignedByte();
        if (type == 0) return Dialog.Action.none()
            .withMaxUses(maxUses);
        if (type == 1) return Dialog.Action.suggestCommand(readString(buffer, MAX_ACTION_VALUE_BYTES))
            .withMaxUses(maxUses);
        if (type == 2) return Dialog.Action.copyToClipboard(readString(buffer, MAX_ACTION_VALUE_BYTES))
            .withMaxUses(maxUses);
        if (type == 3) return Dialog.Action.openUrl(readString(buffer, MAX_ACTION_VALUE_BYTES))
            .withMaxUses(maxUses);
        if (type == 4) return Dialog.Action.runCommand(readString(buffer, MAX_ACTION_VALUE_BYTES))
            .withMaxUses(maxUses);
        if (type == 5) return Dialog.Action.dynamicCommand(readString(buffer, MAX_ACTION_VALUE_BYTES))
            .withMaxUses(maxUses);
        if (type == 6) {
            // The server resolves the destination from the session; keep registry IDs off the client packet.
            return Dialog.Action.showDialog("necrotempus:server_action")
                .withMaxUses(maxUses);
        }
        if (type == 7) {
            // The server resolves the CUSTOM action and payload by its authorized session index.
            return Dialog.Action.dynamicCustom("necrotempus:server_action", new net.minecraft.nbt.NBTTagCompound())
                .withMaxUses(maxUses);
        }
        throw new IllegalArgumentException("Invalid client dialog action");
    }

    private static void requireHttpUrl(String value) {
        if (value.getBytes(StandardCharsets.UTF_8).length > MAX_ACTION_VALUE_BYTES) {
            throw new IllegalArgumentException("URL action is too large");
        }
        try {
            URI uri = new URI(value);
            String scheme = uri.getScheme();
            if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))
                || uri.getHost() == null
                || uri.getUserInfo() != null
                || uri.getPort() > 65535) {
                throw new IllegalArgumentException("URL actions require an HTTP or HTTPS host");
            }
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException("Invalid URL action", exception);
        }
    }

    private static void requireDynamicFormAction(Dialog.ActionButton button) {
        Dialog.Action action = button.getAction();
        if (action.isDynamic() && action.getType() != Dialog.ActionType.CUSTOM
            && action.getType() != Dialog.ActionType.RUN_COMMAND) {
            throw new IllegalArgumentException("Dialog forms require dynamic custom actions");
        }
    }

    private static void requireSupportedInput(Dialog.Input input) {
        if (input instanceof Dialog.TextInput) {
            // Text inputs are rendered by the line or multiline client editor.
        } else if (input instanceof Dialog.BooleanInput) {
            // Supported by the legacy GUI toggle.
        } else if (input instanceof Dialog.SingleOptionInput) {
            if (((Dialog.SingleOptionInput) input).getOptions()
                .size() > MAX_OPTIONS) {
                throw new IllegalArgumentException("Too many options for the legacy codec");
            }
        } else if (input instanceof Dialog.NumberRangeInput) {
            Dialog.NumberRangeInput range = (Dialog.NumberRangeInput) input;
            if (range.getStep() != null
                && Math.abs(((double) range.getEnd() - range.getInitial()) / range.getStep()) > 10000) {
                throw new IllegalArgumentException("Too many number steps for the current screen control");
            }
        } else {
            throw new IllegalArgumentException("Unsupported dialog input");
        }
    }

    private static void writeInput(ByteBuf buffer, Dialog.Input input) {
        writeString(buffer, input.getKey(), 64);
        writeComponent(buffer, input.getLabel());
        if (input instanceof Dialog.TextInput) {
            Dialog.TextInput value = (Dialog.TextInput) input;
            buffer.writeByte(0);
            buffer.writeShort(value.getWidth());
            buffer.writeBoolean(value.isLabelVisible());
            writeString(buffer, value.getInitial(), MAX_INPUT_STRING_BYTES);
            buffer.writeInt(value.getMaxLength());
            buffer.writeInt(value.getMaxLines());
            buffer.writeBoolean(value.getHeight() != null);
            if (value.getHeight() != null) buffer.writeShort(value.getHeight());
        } else if (input instanceof Dialog.BooleanInput) {
            Dialog.BooleanInput value = (Dialog.BooleanInput) input;
            buffer.writeByte(1);
            buffer.writeBoolean(value.getInitial());
            writeOptionalComponent(buffer, value.getOnTrue());
            writeOptionalComponent(buffer, value.getOnFalse());
        } else if (input instanceof Dialog.SingleOptionInput) {
            Dialog.SingleOptionInput value = (Dialog.SingleOptionInput) input;
            buffer.writeByte(2);
            buffer.writeShort(value.getWidth());
            buffer.writeBoolean(value.isLabelVisible());
            buffer.writeByte(
                value.getOptions()
                    .size());
            for (Dialog.Option option : value.getOptions()) {
                writeString(buffer, option.getId(), 512);
                writeComponent(buffer, option.getLabel());
            }
            writeString(buffer, value.getInitial(), 512);
        } else if (input instanceof Dialog.NumberRangeInput) {
            Dialog.NumberRangeInput value = (Dialog.NumberRangeInput) input;
            buffer.writeByte(3);
            buffer.writeShort(value.getWidth());
            writeString(buffer, value.getLabelFormat(), MAX_ACTION_VALUE_BYTES);
            buffer.writeFloat(value.getStart());
            buffer.writeFloat(value.getEnd());
            buffer.writeBoolean(value.getStep() != null);
            if (value.getStep() != null) buffer.writeFloat(value.getStep());
            buffer.writeFloat(value.getInitial());
        } else {
            throw new IllegalArgumentException("Unsupported dialog input");
        }
    }

    private static Dialog.Input readInput(ByteBuf buffer) {
        String key = readString(buffer, 64);
        IChatComponent label = readComponent(buffer);
        int type = buffer.readUnsignedByte();
        switch (type) {
            case 0:
                int textWidth = buffer.readUnsignedShort();
                boolean labelVisible = readBoolean(buffer);
                String initial = readString(buffer, MAX_INPUT_STRING_BYTES);
                int maxLength = buffer.readInt();
                int maxLines = buffer.readInt();
                Integer height = readBoolean(buffer) ? buffer.readUnsignedShort() : null;
                return new Dialog.TextInput(key, label, initial, maxLength, maxLines, textWidth, labelVisible, height);
            case 1:
                return new Dialog.BooleanInput(
                    key,
                    label,
                    readBoolean(buffer),
                    readOptionalComponent(buffer),
                    readOptionalComponent(buffer));
            case 2:
                int optionWidth = buffer.readUnsignedShort();
                boolean optionLabelVisible = readBoolean(buffer);
                int optionCount = buffer.readUnsignedByte();
                if (optionCount < 1 || optionCount > MAX_OPTIONS)
                    throw new IllegalArgumentException("Invalid dialog option count");
                List<Dialog.Option> options = new ArrayList<>(optionCount);
                for (int i = 0; i < optionCount; i++) {
                    options.add(new Dialog.Option(readString(buffer, 512), readComponent(buffer)));
                }
                return new Dialog.SingleOptionInput(
                    key,
                    label,
                    options,
                    readString(buffer, 512),
                    optionWidth,
                    optionLabelVisible);
            case 3:
                int rangeWidth = buffer.readUnsignedShort();
                String labelFormat = readString(buffer, MAX_ACTION_VALUE_BYTES);
                float start = buffer.readFloat();
                float end = buffer.readFloat();
                Float step = readBoolean(buffer) ? buffer.readFloat() : null;
                return new Dialog.NumberRangeInput(
                    key,
                    label,
                    start,
                    end,
                    step,
                    buffer.readFloat(),
                    rangeWidth,
                    labelFormat);
            default:
                throw new IllegalArgumentException("Unknown dialog input type");
        }
    }

    private static void writeOptionalComponent(ByteBuf buffer, IChatComponent component) {
        buffer.writeBoolean(component != null);
        if (component != null) writeComponent(buffer, component);
    }

    private static IChatComponent readOptionalComponent(ByteBuf buffer) {
        return readBoolean(buffer) ? readComponent(buffer) : null;
    }

    private static boolean readBoolean(ByteBuf buffer) {
        int value = buffer.readUnsignedByte();
        if (value > 1) throw new IllegalArgumentException("Invalid dialog boolean");
        return value == 1;
    }

    private static void writeString(ByteBuf buffer, String value, int maxBytes) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > maxBytes) throw new IllegalArgumentException("Dialog input value is too large");
        buffer.writeShort(bytes.length);
        buffer.writeBytes(bytes);
    }

    private static void writeBody(ByteBuf buffer, Dialog.Body body) {
        if (body instanceof Dialog.TextBody) {
            buffer.writeByte(0);
            Dialog.TextBody textBody = (Dialog.TextBody) body;
            writeComponent(buffer, textBody.getText());
            buffer.writeShort(textBody.getWidth());
            writeActionSpans(buffer, textBody.getActionSpans());
        } else if (body instanceof Dialog.ItemBody) {
            Dialog.ItemBody itemBody = (Dialog.ItemBody) body;
            ItemStack item = itemBody.getItem();
            buffer.writeByte(1);
            buffer.writeShort(Item.getIdFromItem(item.getItem()));
            buffer.writeByte(item.stackSize);
            buffer.writeShort(item.getItemDamage());
            try {
                byte[] tag = item.getTagCompound() == null ? null
                    : CompressedStreamTools.compress(item.getTagCompound());
                if (tag == null) {
                    buffer.writeInt(-1);
                } else {
                    if (tag.length > MAX_PACKET_BYTES)
                        throw new IllegalArgumentException("Dialog item NBT is too large");
                    validateNbt(decompressNbt(tag));
                    buffer.writeInt(tag.length);
                    buffer.writeBytes(tag);
                }
            } catch (IOException exception) {
                throw new IllegalArgumentException("Could not encode dialog item NBT", exception);
            }
            IChatComponent description = itemBody.getDescription();
            buffer.writeBoolean(description != null);
            if (description != null) writeComponent(buffer, description);
            buffer.writeBoolean(itemBody.showDecorations());
            buffer.writeBoolean(itemBody.showTooltip());
            buffer.writeShort(itemBody.getWidth());
            buffer.writeShort(itemBody.getHeight());
            buffer.writeShort(itemBody.getDescriptionWidth());
            writeActionSpans(buffer, itemBody.getDescriptionActionSpans());
        } else {
            throw new IllegalArgumentException("Unsupported dialog body");
        }
    }

    private static Dialog.Body readBody(ByteBuf buffer) {
        int type = buffer.readUnsignedByte();
        if (type == 0) {
            IChatComponent text = readComponent(buffer);
            int width = buffer.readUnsignedShort();
            return new Dialog.TextBody(text, width, readActionSpans(buffer));
        }
        if (type != 1) throw new IllegalArgumentException("Invalid dialog body type");
        int itemId = buffer.readUnsignedShort();
        int count = buffer.readUnsignedByte();
        int damage = buffer.readShort();
        int nbtLength = buffer.readInt();
        if (count == 0 || count > Byte.MAX_VALUE
            || nbtLength < -1
            || nbtLength > MAX_PACKET_BYTES
            || nbtLength > buffer.readableBytes()) {
            throw new IllegalArgumentException("Invalid dialog item data");
        }
        Item item = Item.getItemById(itemId);
        if (item == null) throw new IllegalArgumentException("Unknown dialog item id");
        ItemStack stack = new ItemStack(item, count, damage);
        if (nbtLength >= 0) {
            byte[] compressed = new byte[nbtLength];
            buffer.readBytes(compressed);
            try {
                byte[] uncompressed = decompressNbt(compressed);
                validateNbt(uncompressed);
                stack.setTagCompound(
                    CompressedStreamTools.func_152457_a(compressed, new NBTSizeTracker(MAX_ITEM_NBT_BYTES)));
            } catch (IOException | RuntimeException exception) {
                throw new IllegalArgumentException("Invalid dialog item NBT", exception);
            }
        }
        IChatComponent description = readBoolean(buffer) ? readComponent(buffer) : null;
        boolean showDecorations = readBoolean(buffer);
        boolean showTooltip = readBoolean(buffer);
        int width = buffer.readUnsignedShort();
        int height = buffer.readUnsignedShort();
        int descriptionWidth = buffer.readUnsignedShort();
        return new Dialog.ItemBody(
            stack,
            description,
            showDecorations,
            showTooltip,
            width,
            height,
            descriptionWidth,
            readActionSpans(buffer));
    }

    private static void writeActionSpans(ByteBuf buffer, List<Dialog.ActionSpan> spans) {
        buffer.writeByte(spans.size());
        for (Dialog.ActionSpan span : spans) {
            buffer.writeInt(span.getStart());
            buffer.writeInt(span.getEnd());
            writeClientAction(buffer, span.getAction());
        }
    }

    private static List<Dialog.ActionSpan> readActionSpans(ByteBuf buffer) {
        int count = buffer.readUnsignedByte();
        List<Dialog.ActionSpan> spans = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            int start = buffer.readInt();
            int end = buffer.readInt();
            spans.add(new Dialog.ActionSpan(start, end, readClientAction(buffer)));
        }
        return spans;
    }

    private static byte[] decompressNbt(byte[] compressed) throws IOException {
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(compressed));
            ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] chunk = new byte[4096];
            int read;
            while ((read = gzip.read(chunk, 0, Math.min(chunk.length, MAX_ITEM_NBT_BYTES + 1 - output.size()))) != -1) {
                output.write(chunk, 0, read);
                if (output.size() > MAX_ITEM_NBT_BYTES) throw new IOException("Dialog item NBT is too large");
            }
            return output.toByteArray();
        }
    }

    static void validateNbt(byte[] bytes) throws IOException {
        DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes));
        if (input.readUnsignedByte() != 10) throw new IOException("Dialog item NBT root must be a compound");
        input.readUTF();
        scanNbtPayload(10, input, 0, new int[1]);
        if (input.available() != 0) throw new IOException("Trailing dialog item NBT data");
    }

    private static void scanNbtPayload(int type, DataInputStream input, int depth, int[] tagCount) throws IOException {
        if (++tagCount[0] > MAX_ITEM_NBT_TAGS || depth > MAX_ITEM_NBT_DEPTH) {
            throw new IOException("Dialog item NBT exceeds complexity limits");
        }
        switch (type) {
            case 1:
                input.readByte();
                return;
            case 2:
                input.readShort();
                return;
            case 3:
            case 5:
                input.readInt();
                return;
            case 4:
            case 6:
                input.readLong();
                return;
            case 7:
                skipNbtBytes(input, input.readInt());
                return;
            case 8:
                input.readUTF();
                return;
            case 9:
                int childType = input.readUnsignedByte();
                int childCount = input.readInt();
                if (childCount < 0 || childCount > MAX_ITEM_NBT_TAGS
                    || childType > 11
                    || (childCount != 0 && childType == 0)) {
                    throw new IOException("Invalid dialog item NBT list");
                }
                for (int i = 0; i < childCount; i++) scanNbtPayload(childType, input, depth + 1, tagCount);
                return;
            case 10:
                while (true) {
                    int namedType = input.readUnsignedByte();
                    if (namedType == 0) return;
                    if (namedType > 11) throw new IOException("Invalid dialog item NBT tag type");
                    input.readUTF();
                    scanNbtPayload(namedType, input, depth + 1, tagCount);
                }
            case 11:
                int intCount = input.readInt();
                if (intCount < 0 || intCount > input.available() / 4) {
                    throw new IOException("Invalid dialog item NBT int array");
                }
                for (int i = 0; i < intCount; i++) input.readInt();
                return;
            default:
                throw new IOException("Invalid dialog item NBT tag type");
        }
    }

    private static void skipNbtBytes(DataInputStream input, int length) throws IOException {
        if (length < 0 || length > input.available()) throw new IOException("Invalid dialog item NBT byte array");
        int skipped = input.skipBytes(length);
        if (skipped != length) throw new IOException("Truncated dialog item NBT byte array");
    }

    private static String readString(ByteBuf buffer, int maxBytes) {
        int length = buffer.readUnsignedShort();
        if (length > maxBytes || length > buffer.readableBytes())
            throw new IllegalArgumentException("Invalid dialog input string length");
        byte[] bytes = new byte[length];
        buffer.readBytes(bytes);
        try {
            return StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString();
        } catch (CharacterCodingException exception) {
            throw new IllegalArgumentException("Invalid dialog input string", exception);
        }
    }

    private static void writeComponent(ByteBuf buffer, IChatComponent component) {
        byte[] bytes = IChatComponent.Serializer.func_150696_a(component)
            .getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_COMPONENT_BYTES) throw new IllegalArgumentException("Dialog text is too large");
        buffer.writeInt(bytes.length);
        buffer.writeBytes(bytes);
    }

    private static IChatComponent readComponent(ByteBuf buffer) {
        int length = buffer.readInt();
        if (length < 0 || length > MAX_COMPONENT_BYTES || length > buffer.readableBytes()) {
            throw new IllegalArgumentException("Invalid dialog text length");
        }
        byte[] bytes = new byte[length];
        buffer.readBytes(bytes);
        IChatComponent result = IChatComponent.Serializer.func_150699_a(new String(bytes, StandardCharsets.UTF_8));
        if (result == null) throw new IllegalArgumentException("Invalid dialog text component");
        return result;
    }
}
