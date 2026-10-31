package io.github.cruciblemc.necrotempus.api.dialog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;

public final class Dialog {

    public enum Type {
        NOTICE,
        CONFIRMATION,
        MULTI_ACTION,
        DIALOG_LIST,
        SERVER_LINKS
    }

    public enum AfterAction {
        CLOSE,
        NONE,
        WAIT_FOR_RESPONSE
    }

    public enum CommandExecutionPolicy {

        /** Compatibility behavior: ask before every command because the 1.7.10 client cannot validate commands. */
        COMPATIBILITY_CONFIRM,
        /** Skip the client prompt; the server still executes commands as the player. */
        NO_CONFIRMATION;

        public boolean requiresConfirmation() {
            return this == COMPATIBILITY_CONFIRM;
        }
    }

    private static final int MAX_BODY = 255;
    private static final int MAX_INPUTS = 255;
    private static final int MAX_ACTIONS = 255;
    private static final int MAX_URL_LENGTH = 4096;
    private static final Pattern DIALOG_ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9/._-]+");

    private final Type type;
    private final IChatComponent title;
    private final IChatComponent externalTitle;
    private final boolean canCloseWithEscape;
    private final boolean pause;
    private final AfterAction afterAction;
    private final CommandExecutionPolicy commandExecutionPolicy;
    private final List<Body> body;
    private final List<Input> inputs;
    private final List<ActionButton> actions;
    private final ActionButton exitAction;
    private final List<Entry> entries;
    private final List<ServerLink> serverLinks;
    private final int columns;
    private final int buttonWidth;

    private Dialog(Builder builder) {
        type = builder.type;
        title = copy(builder.title);
        externalTitle = copy(builder.externalTitle);
        canCloseWithEscape = builder.canCloseWithEscape;
        pause = builder.pause;
        afterAction = builder.afterAction;
        commandExecutionPolicy = builder.commandExecutionPolicy;
        body = immutable(builder.body);
        inputs = immutable(builder.inputs);
        actions = immutable(builder.actions);
        exitAction = builder.exitAction;
        entries = immutable(builder.entries);
        serverLinks = immutable(builder.serverLinks);
        columns = builder.columns;
        buttonWidth = builder.buttonWidth;
        validate();
    }

    private Dialog(Dialog source, CommandExecutionPolicy commandExecutionPolicy) {
        type = source.type;
        title = copy(source.title);
        externalTitle = copy(source.externalTitle);
        canCloseWithEscape = source.canCloseWithEscape;
        pause = source.pause;
        afterAction = source.afterAction;
        this.commandExecutionPolicy = commandExecutionPolicy;
        body = source.body;
        inputs = source.inputs;
        actions = source.actions;
        exitAction = source.exitAction;
        entries = source.entries;
        serverLinks = source.serverLinks;
        columns = source.columns;
        buttonWidth = source.buttonWidth;
        validate();
    }

    public static Builder notice(IChatComponent title) {
        return new Builder(Type.NOTICE, title);
    }

    public static Builder confirmation(IChatComponent title) {
        return new Builder(Type.CONFIRMATION, title);
    }

    public static Builder multiAction(IChatComponent title) {
        return new Builder(Type.MULTI_ACTION, title);
    }

    public static Builder dialogList(IChatComponent title) {
        return new Builder(Type.DIALOG_LIST, title);
    }

    public static Builder serverLinks(IChatComponent title) {
        return new Builder(Type.SERVER_LINKS, title);
    }

    private void validate() {
        if (title == null) throw new IllegalArgumentException("Dialog title is required");
        if (afterAction == null) throw new IllegalArgumentException("Dialog after action is required");
        if (commandExecutionPolicy == null) throw new IllegalArgumentException("Command execution policy is required");
        if (pause && afterAction == AfterAction.NONE) {
            throw new IllegalArgumentException("A paused dialog must close or wait after an action");
        }
        if (body.size() > MAX_BODY) throw new IllegalArgumentException("Too many dialog body elements");
        if (inputs.size() > MAX_INPUTS) throw new IllegalArgumentException("Too many dialog inputs");
        if (actions.size() + (exitAction == null ? 0 : 1) > MAX_ACTIONS) {
            throw new IllegalArgumentException("Too many dialog actions");
        }
        if (columns < 1) throw new IllegalArgumentException("Dialog columns must be positive");
        requireWidth(buttonWidth);
        if (type != Type.MULTI_ACTION && type != Type.DIALOG_LIST && type != Type.SERVER_LINKS && columns != 2) {
            throw new IllegalArgumentException("Dialog columns do not match dialog type");
        }
        if (type != Type.DIALOG_LIST && type != Type.SERVER_LINKS && buttonWidth != 150) {
            throw new IllegalArgumentException("Dialog button width does not match dialog type");
        }

        switch (type) {
            case NOTICE:
                requireCount(actions, 1, "notice action");
                requireEmpty(entries, serverLinks);
                break;
            case CONFIRMATION:
                requireCount(actions, 2, "confirmation actions");
                requireEmpty(entries, serverLinks);
                break;
            case MULTI_ACTION:
                if (actions.isEmpty()) throw new IllegalArgumentException("Multi-action dialog requires an action");
                requireEmpty(entries, serverLinks);
                break;
            case DIALOG_LIST:
                if (entries.isEmpty()) throw new IllegalArgumentException("Dialog list requires an entry");
                requireEmpty(actions, serverLinks);
                if (entries.size() > MAX_ACTIONS) throw new IllegalArgumentException("Too many dialog entries");
                break;
            case SERVER_LINKS:
                requireEmpty(actions, entries);
                if (serverLinks.size() > MAX_ACTIONS) throw new IllegalArgumentException("Too many server links");
                break;
            default:
                throw new IllegalArgumentException("Unsupported dialog type: " + type);
        }
    }

    private static void requireEmpty(List<?> first, List<?> second) {
        if (!first.isEmpty() || !second.isEmpty())
            throw new IllegalArgumentException("Fields do not match dialog type");
    }

    private static void requireCount(List<?> values, int count, String name) {
        if (values.size() != count) throw new IllegalArgumentException("Expected " + count + " " + name);
    }

    private static IChatComponent copy(IChatComponent component) {
        return component == null ? null : component.createCopy();
    }

    private static <T> List<T> immutable(List<T> values) {
        return Collections.unmodifiableList(new ArrayList<>(values));
    }

    public Type getType() {
        return type;
    }

    public IChatComponent getTitle() {
        return copy(title);
    }

    public IChatComponent getExternalTitle() {
        return copy(externalTitle);
    }

    public boolean canCloseWithEscape() {
        return canCloseWithEscape;
    }

    public boolean pausesGame() {
        return pause;
    }

    public AfterAction getAfterAction() {
        return afterAction;
    }

    public CommandExecutionPolicy getCommandExecutionPolicy() {
        return commandExecutionPolicy;
    }

    public Dialog withCommandExecutionPolicy(CommandExecutionPolicy value) {
        if (value == null) throw new IllegalArgumentException("Command execution policy is required");
        return value == commandExecutionPolicy ? this : new Dialog(this, value);
    }

    public List<Body> getBody() {
        return body;
    }

    public List<Input> getInputs() {
        return inputs;
    }

    public List<ActionButton> getActions() {
        return actions;
    }

    public ActionButton getExitAction() {
        return exitAction;
    }

    public ActionButton getCancelAction() {
        int index = getCancelActionIndex();
        if (index >= 0 && index < actions.size()) return actions.get(index);
        return index == controlCount() ? exitAction : null;
    }

    public int getCancelActionIndex() {
        if (exitAction != null) return controlCount();
        if (type == Type.NOTICE) return 0;
        if (type == Type.CONFIRMATION) return 1;
        return -1;
    }

    private int controlCount() {
        if (type == Type.DIALOG_LIST) return entries.size();
        if (type == Type.SERVER_LINKS) return serverLinks.size();
        return actions.size();
    }

    public List<Entry> getEntries() {
        return entries;
    }

    public List<ServerLink> getServerLinks() {
        return serverLinks;
    }

    public int getColumns() {
        return columns;
    }

    public int getButtonWidth() {
        return buttonWidth;
    }

    private static void requireWidth(int width) {
        if (width < 1 || width > 1024) throw new IllegalArgumentException("Dialog width must be 1-1024");
    }

    public static final class Builder {

        private final Type type;
        private final IChatComponent title;
        private IChatComponent externalTitle;
        private boolean canCloseWithEscape = true;
        private boolean pause = true;
        private AfterAction afterAction = AfterAction.CLOSE;
        private CommandExecutionPolicy commandExecutionPolicy = CommandExecutionPolicy.COMPATIBILITY_CONFIRM;
        private final List<Body> body = new ArrayList<>();
        private final List<Input> inputs = new ArrayList<>();
        private final List<ActionButton> actions = new ArrayList<>();
        private ActionButton exitAction;
        private final List<Entry> entries = new ArrayList<>();
        private final List<ServerLink> serverLinks = new ArrayList<>();
        private int columns = 2;
        private int buttonWidth = 150;

        private Builder(Type type, IChatComponent title) {
            if (type == null || title == null) throw new IllegalArgumentException("Dialog type and title are required");
            this.type = type;
            this.title = copy(title);
        }

        public Builder externalTitle(IChatComponent value) {
            externalTitle = copy(value);
            return this;
        }

        public Builder canCloseWithEscape(boolean value) {
            canCloseWithEscape = value;
            return this;
        }

        public Builder pause(boolean value) {
            pause = value;
            return this;
        }

        public Builder afterAction(AfterAction value) {
            if (value == null) throw new IllegalArgumentException("Dialog after action is required");
            afterAction = value;
            return this;
        }

        public Builder commandExecutionPolicy(CommandExecutionPolicy value) {
            if (value == null) throw new IllegalArgumentException("Command execution policy is required");
            commandExecutionPolicy = value;
            return this;
        }

        public Builder body(Body value) {
            if (value == null) throw new IllegalArgumentException("Dialog body is required");
            body.add(value.copy());
            return this;
        }

        public Builder input(Input value) {
            if (value == null) throw new IllegalArgumentException("Dialog input is required");
            inputs.add(value.copy());
            return this;
        }

        public Builder action(ActionButton value) {
            if (value == null) throw new IllegalArgumentException("Dialog action is required");
            actions.add(value.copy());
            return this;
        }

        public Builder exitAction(ActionButton value) {
            exitAction = value == null ? null : value.copy();
            return this;
        }

        public Builder entry(Entry value) {
            if (value == null) throw new IllegalArgumentException("Dialog entry is required");
            entries.add(value.copy());
            return this;
        }

        public Builder entryTag(String tagId) {
            entries.add(Entry.tag(tagId));
            return this;
        }

        public Builder serverLink(ServerLink value) {
            if (value == null) throw new IllegalArgumentException("Server link is required");
            serverLinks.add(value);
            return this;
        }

        public Builder columns(int value) {
            columns = value;
            return this;
        }

        public Builder buttonWidth(int value) {
            buttonWidth = value;
            return this;
        }

        public Dialog build() {
            if (type == Type.NOTICE && actions.isEmpty()) {
                actions.add(new ActionButton(new ChatComponentTranslation("gui.ok"), Action.none()));
            }
            return new Dialog(this);
        }
    }

    public abstract static class Body {

        private Body() {}

        public abstract Body copy();
    }

    public static final class ActionSpan {

        private final int start;
        private final int end;
        private final Action action;

        public ActionSpan(int start, int end, Action action) {
            if (start < 0 || end <= start || action == null) {
                throw new IllegalArgumentException("Invalid dialog body action span");
            }
            this.start = start;
            this.end = end;
            this.action = action.copy();
        }

        public int getStart() {
            return start;
        }

        public int getEnd() {
            return end;
        }

        public Action getAction() {
            return action.copy();
        }
    }

    public static final class TextBody extends Body {

        private final IChatComponent text;
        private final int width;
        private final List<ActionSpan> actionSpans;

        public TextBody(IChatComponent text) {
            this(text, 200);
        }

        public TextBody(IChatComponent text, int width) {
            this(text, width, Collections.emptyList());
        }

        public TextBody(IChatComponent text, int width, List<ActionSpan> actionSpans) {
            if (text == null) throw new IllegalArgumentException("Body text is required");
            requireWidth(width);
            this.text = Dialog.copy(text);
            this.width = width;
            this.actionSpans = copyActionSpans(actionSpans, this.text);
        }

        public IChatComponent getText() {
            return Dialog.copy(text);
        }

        public int getWidth() {
            return width;
        }

        public List<ActionSpan> getActionSpans() {
            return actionSpans;
        }

        @Override
        public Body copy() {
            return new TextBody(text, width, actionSpans);
        }
    }

    public static final class ItemBody extends Body {

        private final ItemStack item;
        private final IChatComponent description;
        private final boolean showDecorations;
        private final boolean showTooltip;
        private final int width;
        private final int height;
        private final int descriptionWidth;
        private final List<ActionSpan> descriptionActionSpans;

        public ItemBody(ItemStack item, IChatComponent description) {
            this(item, description, true, true, 16, 16, 200);
        }

        public ItemBody(ItemStack item, IChatComponent description, boolean showDecorations, boolean showTooltip,
            int width, int height) {
            this(item, description, showDecorations, showTooltip, width, height, 200);
        }

        public ItemBody(ItemStack item, IChatComponent description, boolean showDecorations, boolean showTooltip,
            int width, int height, int descriptionWidth) {
            this(
                item,
                description,
                showDecorations,
                showTooltip,
                width,
                height,
                descriptionWidth,
                Collections.emptyList());
        }

        public ItemBody(ItemStack item, IChatComponent description, boolean showDecorations, boolean showTooltip,
            int width, int height, int descriptionWidth, List<ActionSpan> descriptionActionSpans) {
            if (item == null || item.stackSize <= 0) throw new IllegalArgumentException("A non-empty item is required");
            if (item.getItem() == null) throw new IllegalArgumentException("A registered item is required");
            if (width < 1 || width > 256 || height < 1 || height > 256) {
                throw new IllegalArgumentException("Item body dimensions must be 1-256");
            }
            requireWidth(descriptionWidth);
            this.item = item.copy();
            this.description = Dialog.copy(description);
            this.showDecorations = showDecorations;
            this.showTooltip = showTooltip;
            this.width = width;
            this.height = height;
            this.descriptionWidth = descriptionWidth;
            this.descriptionActionSpans = copyActionSpans(descriptionActionSpans, this.description);
        }

        public ItemStack getItem() {
            return item.copy();
        }

        public IChatComponent getDescription() {
            return Dialog.copy(description);
        }

        public boolean showDecorations() {
            return showDecorations;
        }

        public boolean showTooltip() {
            return showTooltip;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public int getDescriptionWidth() {
            return descriptionWidth;
        }

        public List<ActionSpan> getDescriptionActionSpans() {
            return descriptionActionSpans;
        }

        @Override
        public Body copy() {
            return new ItemBody(
                item,
                description,
                showDecorations,
                showTooltip,
                width,
                height,
                descriptionWidth,
                descriptionActionSpans);
        }
    }

    private static List<ActionSpan> copyActionSpans(List<ActionSpan> spans, IChatComponent text) {
        if (spans == null || spans.size() > 255 || text == null && !spans.isEmpty()) {
            throw new IllegalArgumentException("Invalid dialog body action spans");
        }
        int textLength = text == null ? 0
            : text.getUnformattedTextForChat()
                .length();
        List<ActionSpan> copy = new ArrayList<>(spans.size());
        int previousEnd = 0;
        for (ActionSpan span : spans) {
            if (span == null || span.start < previousEnd || span.end > textLength) {
                throw new IllegalArgumentException("Dialog body action spans must be ordered and inside their text");
            }
            copy.add(new ActionSpan(span.start, span.end, span.action));
            previousEnd = span.end;
        }
        return Collections.unmodifiableList(copy);
    }

    public abstract static class Input {

        private final String key;
        private final IChatComponent label;

        private Input(String key, IChatComponent label) {
            if (!DialogCommandTemplate.validKey(key)) throw new IllegalArgumentException("Invalid input key");
            if (label == null) throw new IllegalArgumentException("Input label is required");
            this.key = key;
            this.label = Dialog.copy(label);
        }

        public String getKey() {
            return key;
        }

        public IChatComponent getLabel() {
            return Dialog.copy(label);
        }

        public abstract Input copy();
    }

    public static final class TextInput extends Input {

        private final String initial;
        private final int maxLength;
        private final int maxLines;
        private final int width;
        private final boolean labelVisible;
        private final Integer height;

        public TextInput(String key, IChatComponent label) {
            this(key, label, "", 32, 1);
        }

        public TextInput(String key, IChatComponent label, String initial, int maxLength, int maxLines) {
            this(key, label, initial, maxLength, maxLines, 200, true, null);
        }

        public TextInput(String key, IChatComponent label, String initial, int maxLength, int maxLines, int width,
            boolean labelVisible, Integer height) {
            super(key, label);
            if (initial == null || initial.indexOf('\r') >= 0 || maxLength < 1 || initial.length() > maxLength) {
                throw new IllegalArgumentException("Invalid text input length");
            }
            if (maxLines < 1) throw new IllegalArgumentException("Invalid text input line limit");
            if (initial.split("\\n", -1).length > maxLines) {
                throw new IllegalArgumentException("Initial text exceeds the line limit");
            }
            requireWidth(width);
            if (height != null && (height < 1 || height > 512)) {
                throw new IllegalArgumentException("Text input height must be 1-512");
            }
            this.initial = initial;
            this.maxLength = maxLength;
            this.maxLines = maxLines;
            this.width = width;
            this.labelVisible = labelVisible;
            this.height = height;
        }

        public String getInitial() {
            return initial;
        }

        public int getMaxLength() {
            return maxLength;
        }

        public int getMaxLines() {
            return maxLines;
        }

        public int getWidth() {
            return width;
        }

        public boolean isLabelVisible() {
            return labelVisible;
        }

        public Integer getHeight() {
            return height;
        }

        @Override
        public Input copy() {
            return new TextInput(getKey(), getLabel(), initial, maxLength, maxLines, width, labelVisible, height);
        }
    }

    public static final class BooleanInput extends Input {

        private final boolean initial;
        private final IChatComponent onTrue;
        private final IChatComponent onFalse;

        public BooleanInput(String key, IChatComponent label, boolean initial, IChatComponent onTrue,
            IChatComponent onFalse) {
            super(key, label);
            this.initial = initial;
            this.onTrue = Dialog.copy(onTrue);
            this.onFalse = Dialog.copy(onFalse);
        }

        public boolean getInitial() {
            return initial;
        }

        public IChatComponent getOnTrue() {
            return Dialog.copy(onTrue);
        }

        public IChatComponent getOnFalse() {
            return Dialog.copy(onFalse);
        }

        @Override
        public Input copy() {
            return new BooleanInput(getKey(), getLabel(), initial, onTrue, onFalse);
        }
    }

    public static final class Option {

        private final String id;
        private final IChatComponent label;

        public Option(String id, IChatComponent label) {
            if (id == null || id.isEmpty() || id.length() > 128)
                throw new IllegalArgumentException("Invalid option id");
            if (label == null) throw new IllegalArgumentException("Option label is required");
            this.id = id;
            this.label = Dialog.copy(label);
        }

        public Option(String id) {
            this(id, new net.minecraft.util.ChatComponentText(id));
        }

        public String getId() {
            return id;
        }

        public IChatComponent getLabel() {
            return Dialog.copy(label);
        }

        private Option copy() {
            return new Option(id, label);
        }
    }

    public static final class SingleOptionInput extends Input {

        private final List<Option> options;
        private final String initial;
        private final int width;
        private final boolean labelVisible;

        public SingleOptionInput(String key, IChatComponent label, List<Option> options, String initial) {
            this(key, label, options, initial, 200, true);
        }

        public SingleOptionInput(String key, IChatComponent label, List<Option> options, String initial, int width,
            boolean labelVisible) {
            super(key, label);
            if (options == null || options.isEmpty() || options.size() > MAX_ACTIONS) {
                throw new IllegalArgumentException("Invalid option list");
            }
            requireWidth(width);
            List<Option> copy = new ArrayList<>(options.size());
            Set<String> ids = new HashSet<>();
            for (Option option : options) {
                if (option == null || !ids.add(option.getId()))
                    throw new IllegalArgumentException("Duplicate option id");
                copy.add(option.copy());
            }
            if (initial != null && !ids.contains(initial)) {
                throw new IllegalArgumentException("Initial option must be present");
            }
            this.options = Collections.unmodifiableList(copy);
            this.initial = initial == null ? copy.get(0)
                .getId() : initial;
            this.width = width;
            this.labelVisible = labelVisible;
        }

        public List<Option> getOptions() {
            List<Option> copy = new ArrayList<>(options.size());
            for (Option option : options) copy.add(option.copy());
            return Collections.unmodifiableList(copy);
        }

        public String getInitial() {
            return initial;
        }

        public int getWidth() {
            return width;
        }

        public boolean isLabelVisible() {
            return labelVisible;
        }

        @Override
        public Input copy() {
            return new SingleOptionInput(getKey(), getLabel(), options, initial, width, labelVisible);
        }
    }

    public static final class NumberRangeInput extends Input {

        private final float start;
        private final float end;
        private final Float step;
        private final float initial;
        private final int width;
        private final String labelFormat;

        public NumberRangeInput(String key, IChatComponent label, float start, float end, Float step) {
            this(key, label, start, end, step, (start + end) / 2.0f);
        }

        public NumberRangeInput(String key, IChatComponent label, float start, float end, Float step, float initial) {
            this(key, label, start, end, step, initial, 200, "options.generic_value");
        }

        public NumberRangeInput(String key, IChatComponent label, float start, float end, Float step, float initial,
            int width, String labelFormat) {
            super(key, label);
            if (!Float.isFinite(start) || !Float.isFinite(end) || !Float.isFinite(initial)) {
                throw new IllegalArgumentException("Range values must be finite");
            }
            if (initial < Math.min(start, end) || initial > Math.max(start, end)) {
                throw new IllegalArgumentException("Initial value must be within the range");
            }
            if (step != null && (!Float.isFinite(step) || step <= 0)) {
                throw new IllegalArgumentException("Step must be finite and positive");
            }
            requireWidth(width);
            if (labelFormat == null) throw new IllegalArgumentException("Slider label format is required");
            this.start = start;
            this.end = end;
            this.step = step;
            this.initial = initial;
            this.width = width;
            this.labelFormat = labelFormat;
        }

        public float getStart() {
            return start;
        }

        public float getEnd() {
            return end;
        }

        public Float getStep() {
            return step;
        }

        public float getInitial() {
            return initial;
        }

        public int getWidth() {
            return width;
        }

        public String getLabelFormat() {
            return labelFormat;
        }

        @Override
        public Input copy() {
            return new NumberRangeInput(getKey(), getLabel(), start, end, step, initial, width, labelFormat);
        }
    }

    public enum ActionType {
        NONE,
        CUSTOM,
        RUN_COMMAND,
        OPEN_URL,
        SUGGEST_COMMAND,
        COPY_TO_CLIPBOARD,
        SHOW_DIALOG
    }

    public static final class Action {

        private static final int DEFAULT_MAX_USES = Integer.MAX_VALUE;

        private final ActionType type;
        private final String value;
        private final NBTBase payload;
        private final int maxUses;
        private final boolean dynamic;
        private final Dialog inlineDialog;

        private Action(ActionType type, String value, NBTBase payload, int maxUses, boolean dynamic) {
            this(type, value, payload, maxUses, dynamic, null);
        }

        private Action(ActionType type, String value, NBTBase payload, int maxUses, boolean dynamic,
            Dialog inlineDialog) {
            if (type == null) throw new IllegalArgumentException("Action type is required");
            if (maxUses < 1) throw new IllegalArgumentException("Action uses must be positive");
            if (inlineDialog != null && type != ActionType.SHOW_DIALOG) {
                throw new IllegalArgumentException("Only show-dialog actions accept inline dialogs");
            }
            if (type == ActionType.NONE && (value != null || payload != null || dynamic)) {
                throw new IllegalArgumentException("An empty action cannot have a payload");
            }
            if (type == ActionType.CUSTOM && (value == null || value.isEmpty())) {
                throw new IllegalArgumentException("Custom action id is required");
            }
            if (type != ActionType.CUSTOM && payload != null)
                throw new IllegalArgumentException("Only custom actions accept a payload");
            if (type != ActionType.NONE && type != ActionType.CUSTOM
                && type != ActionType.SHOW_DIALOG
                && (value == null || value.isEmpty())) {
                throw new IllegalArgumentException("Action value is required");
            }
            if (type == ActionType.SHOW_DIALOG) {
                if ((value == null) == (inlineDialog == null)) {
                    throw new IllegalArgumentException("Show-dialog actions require exactly one target");
                }
                if (value != null && (value.length() > 128 || !DIALOG_ID.matcher(value)
                    .matches())) {
                    throw new IllegalArgumentException("Show-dialog actions require a namespaced dialog ID");
                }
            }
            if (dynamic && type != ActionType.CUSTOM && type != ActionType.RUN_COMMAND) {
                throw new IllegalArgumentException("Only custom and command actions support inputs");
            }
            if (dynamic && payload != null && !(payload instanceof NBTTagCompound)) {
                throw new IllegalArgumentException("Dynamic custom additions must be a compound");
            }
            if (type == ActionType.RUN_COMMAND) DialogCommandTemplate.validate(value, dynamic);
            this.type = type;
            this.value = value;
            this.payload = payload == null ? null : payload.copy();
            this.maxUses = maxUses;
            this.dynamic = dynamic;
            this.inlineDialog = inlineDialog;
        }

        public static Action none() {
            return new Action(ActionType.NONE, null, null, DEFAULT_MAX_USES, false);
        }

        public static Action custom(String id, NBTBase payload) {
            return new Action(ActionType.CUSTOM, id, payload, DEFAULT_MAX_USES, false);
        }

        public static Action dynamicCustom(String id, NBTTagCompound additions) {
            if (additions == null) throw new IllegalArgumentException("Dynamic custom additions are required");
            return new Action(ActionType.CUSTOM, id, additions, DEFAULT_MAX_USES, true);
        }

        public static Action runCommand(String command) {
            return new Action(ActionType.RUN_COMMAND, command, null, DEFAULT_MAX_USES, false);
        }

        public static Action dynamicCommand(String template) {
            return new Action(ActionType.RUN_COMMAND, template, null, DEFAULT_MAX_USES, true);
        }

        public static Action openUrl(String url) {
            return new Action(ActionType.OPEN_URL, url, null, DEFAULT_MAX_USES, false);
        }

        public static Action suggestCommand(String command) {
            return new Action(ActionType.SUGGEST_COMMAND, command, null, DEFAULT_MAX_USES, false);
        }

        public static Action copyToClipboard(String value) {
            return new Action(ActionType.COPY_TO_CLIPBOARD, value, null, DEFAULT_MAX_USES, false);
        }

        public static Action showDialog(String id) {
            return new Action(ActionType.SHOW_DIALOG, id, null, DEFAULT_MAX_USES, false);
        }

        public static Action showDialog(Dialog dialog) {
            if (dialog == null) throw new IllegalArgumentException("Inline dialog is required");
            return new Action(ActionType.SHOW_DIALOG, null, null, DEFAULT_MAX_USES, false, dialog);
        }

        public Action withMaxUses(int value) {
            return new Action(type, this.value, payload, value, dynamic, inlineDialog);
        }

        public ActionType getType() {
            return type;
        }

        public String getValue() {
            return value;
        }

        public Dialog getInlineDialog() {
            return inlineDialog;
        }

        public NBTBase getPayload() {
            return payload == null ? null : payload.copy();
        }

        public int getMaxUses() {
            return maxUses;
        }

        public boolean isDynamic() {
            return dynamic;
        }

        public String resolveCommand(Dialog dialog, Map<String, Object> values) {
            if (type != ActionType.RUN_COMMAND) throw new IllegalStateException("Action is not a command");
            return dynamic ? DialogCommandTemplate.expand(value, dialog, values) : value;
        }

        private Action copy() {
            return new Action(type, value, payload, maxUses, dynamic, inlineDialog);
        }
    }

    public static final class ActionButton {

        private final IChatComponent label;
        private final IChatComponent tooltip;
        private final int width;
        private final Action action;

        public ActionButton(IChatComponent label, Action action) {
            this(label, action, null, 150);
        }

        public ActionButton(IChatComponent label, Action action, IChatComponent tooltip, int width) {
            if (label == null || action == null)
                throw new IllegalArgumentException("Action label and action are required");
            requireWidth(width);
            this.label = Dialog.copy(label);
            this.tooltip = Dialog.copy(tooltip);
            this.width = width;
            this.action = action.copy();
        }

        public IChatComponent getLabel() {
            return Dialog.copy(label);
        }

        public Action getAction() {
            return action.copy();
        }

        public IChatComponent getTooltip() {
            return Dialog.copy(tooltip);
        }

        public int getWidth() {
            return width;
        }

        private ActionButton copy() {
            return new ActionButton(label, action, tooltip, width);
        }
    }

    public static final class Entry {

        private final IChatComponent label;
        private final String dialogId;
        private final Dialog inlineDialog;
        private final String tagId;
        private final boolean displayOnly;

        public Entry(IChatComponent label, String dialogId) {
            if (label == null || dialogId == null
                || dialogId.length() > 128
                || !DIALOG_ID.matcher(dialogId)
                    .matches()) {
                throw new IllegalArgumentException("Invalid dialog entry");
            }
            this.label = Dialog.copy(label);
            this.dialogId = dialogId;
            inlineDialog = null;
            tagId = null;
            displayOnly = false;
        }

        public Entry(IChatComponent label, Dialog inlineDialog) {
            if (label == null || inlineDialog == null)
                throw new IllegalArgumentException("Invalid inline dialog entry");
            this.label = Dialog.copy(label);
            dialogId = null;
            this.inlineDialog = inlineDialog;
            tagId = null;
            displayOnly = false;
        }

        private Entry(IChatComponent label) {
            if (label == null) throw new IllegalArgumentException("Dialog entry label is required");
            this.label = Dialog.copy(label);
            dialogId = null;
            inlineDialog = null;
            tagId = null;
            displayOnly = true;
        }

        private Entry(String tagId) {
            if (tagId == null || tagId.length() > 128
                || !DIALOG_ID.matcher(tagId)
                    .matches()) {
                throw new IllegalArgumentException("Dialog tag IDs must be namespaced");
            }
            label = null;
            dialogId = null;
            inlineDialog = null;
            this.tagId = tagId;
            displayOnly = false;
        }

        public static Entry displayOnly(IChatComponent label) {
            return new Entry(label);
        }

        public static Entry tag(String tagId) {
            return new Entry(tagId);
        }

        public IChatComponent getLabel() {
            return label == null ? null : Dialog.copy(label);
        }

        public String getDialogId() {
            return dialogId;
        }

        public Dialog getInlineDialog() {
            return inlineDialog;
        }

        public String getTagId() {
            return tagId;
        }

        public boolean isTagReference() {
            return tagId != null;
        }

        public boolean hasTarget() {
            return !displayOnly;
        }

        private Entry copy() {
            if (displayOnly) return new Entry(label);
            if (tagId != null) return new Entry(tagId);
            if (inlineDialog != null) return new Entry(label, inlineDialog);
            return new Entry(label, dialogId);
        }
    }

    public static final class ServerLink {

        private final IChatComponent label;
        private final String url;

        public ServerLink(IChatComponent label, String url) {
            if (label == null || url == null || url.isEmpty() || url.length() > MAX_URL_LENGTH) {
                throw new IllegalArgumentException("Invalid server link");
            }
            this.label = Dialog.copy(label);
            this.url = url;
        }

        public IChatComponent getLabel() {
            return Dialog.copy(label);
        }

        public String getUrl() {
            return url;
        }
    }
}
