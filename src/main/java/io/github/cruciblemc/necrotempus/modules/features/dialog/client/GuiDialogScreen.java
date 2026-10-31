package io.github.cruciblemc.necrotempus.modules.features.dialog.client;

import java.awt.Desktop;
import java.net.URI;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiConfirmOpenLink;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.GuiYesNo;
import net.minecraft.client.gui.GuiYesNoCallback;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;

import org.lwjgl.BufferUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import io.github.cruciblemc.necrotempus.api.dialog.Dialog;
import io.github.cruciblemc.necrotempus.api.dialog.DialogBodyActions;
import io.github.cruciblemc.necrotempus.modules.features.dialog.DialogFormValues;

public final class GuiDialogScreen extends GuiScreen {

    private static final int INPUT_BUTTON_BASE = 1000;
    private static final int WARNING_BUTTON_ID = 20000;
    private static final int INPUT_ROW_HEIGHT = 42;
    private final Dialog dialog;
    private final UUID sessionId;
    private final int revision;
    private final DialogScreenContext context;
    private final int[] remainingUses;
    private final List<Object> inputValues = new ArrayList<>();
    private final Map<Integer, GuiTextField> textFields = new LinkedHashMap<>();
    private final Map<Integer, DialogTextArea> textAreas = new LinkedHashMap<>();
    private final Map<Integer, SliderBounds> sliders = new LinkedHashMap<>();
    private final Map<Integer, Dialog.Input> inputButtons = new LinkedHashMap<>();
    private final List<Object> focusOrder = new ArrayList<>();
    private final List<BodyClickTarget> bodyClickTargets = new ArrayList<>();
    private int panelTop;
    private int panelHeight;
    private int panelWidth;
    private int panelLeft;
    private int contentTop;
    private int contentHeight;
    private int contentViewportHeight;
    private int contentScrollOffset;
    private Integer draggedSlider;
    private int focusedControl = -1;

    public GuiDialogScreen(Dialog dialog, UUID sessionId, int revision) {
        this(dialog, sessionId, revision, DialogScreenContext.GAMEPLAY);
    }

    GuiDialogScreen(Dialog dialog, UUID sessionId, int revision, DialogScreenContext context) {
        this.dialog = dialog;
        this.sessionId = sessionId;
        this.revision = revision;
        this.context = context;
        int controlCount = controlCount();
        remainingUses = new int[controlCount + (dialog.getExitAction() == null ? 0 : 1)
            + DialogBodyActions.collect(dialog)
                .size()];
        for (int i = 0; i < remainingUses.length; i++) {
            Dialog.Action action = actionForControl(i);
            remainingUses[i] = action == null ? 1 : action.getMaxUses();
        }
        for (Dialog.Input input : dialog.getInputs()) inputValues.add(initialValue(input));
    }

    @Override
    public void initGui() {
        int previousFocus = focusedControl;
        Map<Integer, int[]> textFieldSelections = new LinkedHashMap<>();
        for (Map.Entry<Integer, GuiTextField> entry : textFields.entrySet()) {
            GuiTextField field = entry.getValue();
            inputValues.set(entry.getKey(), field.getText());
            textFieldSelections.put(entry.getKey(), new int[] { field.getCursorPosition(), field.getSelectionEnd() });
        }
        for (Map.Entry<Integer, DialogTextArea> entry : textAreas.entrySet()) {
            inputValues.set(
                entry.getKey(),
                entry.getValue()
                    .getBuffer()
                    .getText());
        }
        buttonList.clear();
        textFields.clear();
        sliders.clear();
        inputButtons.clear();
        focusOrder.clear();
        focusedControl = -1;
        int columns = dialog.getType() == Dialog.Type.NOTICE ? 1
            : dialog.getType() == Dialog.Type.CONFIRMATION ? 2 : dialog.getColumns();
        int rows = (controlCount() + columns - 1) / columns;
        boolean simpleActions = dialog.getType() == Dialog.Type.NOTICE || dialog.getType() == Dialog.Type.CONFIRMATION;
        int buttonGap = simpleActions ? 8 : 2;
        int footerRows = (simpleActions ? 1 : 0) + (dialog.getExitAction() == null ? 0 : 1);
        panelWidth = DialogLayout.panelWidth(width, requiredContentWidth());
        panelLeft = (width - panelWidth) / 2;
        int bodyHeight = bodyHeight();
        int inputHeight = inputHeight();
        boolean hasContentActions = !simpleActions && controlCount() > 0;
        int contentBlocks = (dialog.getBody()
            .isEmpty() ? 0 : 1)
            + (dialog.getInputs()
                .isEmpty() ? 0 : 1)
            + (hasContentActions ? 1 : 0);
        contentHeight = bodyHeight + inputHeight
            + (hasContentActions ? rows * 24 : 0)
            + Math.max(0, contentBlocks - 1) * 10;
        panelHeight = Math.min(Math.max(1, height - 32), 60 + contentHeight + footerRows * 26);
        contentViewportHeight = Math.max(1, panelHeight - 60 - footerRows * 26);
        contentScrollOffset = DialogListViewport
            .clampPixelOffset(contentScrollOffset, contentHeight, contentViewportHeight);
        panelTop = (height - panelHeight) / 2;
        contentTop = panelTop + 42;
        int inputsTop = contentTop + bodyHeight
            + (dialog.getBody()
                .isEmpty() ? 0 : 10)
            - contentScrollOffset;
        List<Integer> actionWidths = controlButtonWidths();
        int maxButtonWidth = Math.max(1, (panelWidth - 20 - buttonGap * Math.max(0, columns - 1)) / columns);
        int maxInputWidth = Math.max(1, panelWidth - 20);
        for (int i = 0; i < actionWidths.size(); i++) {
            actionWidths.set(i, Math.min(actionWidths.get(i), maxButtonWidth));
        }

        addButton(new GuiButton(WARNING_BUTTON_ID, panelLeft + panelWidth - 30, panelTop + 7, 20, 20, "!"));

        for (int i = 0; i < dialog.getInputs()
            .size(); i++) {
            Dialog.Input input = dialog.getInputs()
                .get(i);
            int rowY = inputsTop + inputTop(i);
            if (input instanceof Dialog.TextInput) {
                Dialog.TextInput text = (Dialog.TextInput) input;
                int labelWidth = text.isLabelVisible() ? fontRendererObj.getStringWidth(label(text.getLabel())) : 0;
                int labelGap = labelWidth == 0 ? 0 : 8;
                int fieldWidth = Math.min(text.getWidth(), Math.max(1, maxInputWidth - labelWidth - labelGap));
                int fieldX = DialogLayout.labeledControlX(panelLeft, panelWidth, labelWidth, labelGap, fieldWidth);
                int controlHeight = text.getMaxLines() > 1 ? textHeight(text) : 18;
                int fieldY = rowY + (inputRowHeight(text) - controlHeight) / 2;
                if (text.getMaxLines() > 1) {
                    DialogTextArea area = textAreas.get(i);
                    if (area == null) {
                        area = new DialogTextArea(
                            new DialogTextBuffer((String) inputValues.get(i), text.getMaxLength(), text.getMaxLines()));
                        textAreas.put(i, area);
                    }
                    area.setBounds(fieldX, fieldY, fieldWidth, controlHeight, fontRendererObj);
                    focusOrder.add(area);
                } else {
                    GuiTextField field = new GuiTextField(fontRendererObj, fieldX, fieldY, fieldWidth, 18);
                    field.setMaxStringLength(text.getMaxLength());
                    field.setText((String) inputValues.get(i));
                    int[] selection = textFieldSelections.get(i);
                    if (selection != null) {
                        int length = field.getText()
                            .length();
                        field.setCursorPosition(Math.min(selection[0], length));
                        field.setSelectionPos(Math.min(selection[1], length));
                    }
                    textFields.put(i, field);
                    focusOrder.add(field);
                }
            } else if (input instanceof Dialog.NumberRangeInput) {
                Dialog.NumberRangeInput range = (Dialog.NumberRangeInput) input;
                int sliderWidth = Math.min(range.getWidth(), maxInputWidth);
                sliders.put(i, new SliderBounds(panelLeft + (panelWidth - sliderWidth) / 2, rowY + 10, sliderWidth));
                int id = INPUT_BUTTON_BASE + i;
                inputButtons.put(id, input);
                addButton(
                    new GuiButton(
                        id,
                        panelLeft + (panelWidth - sliderWidth) / 2,
                        rowY,
                        sliderWidth,
                        20,
                        inputButtonLabel(i)));
            } else {
                int id = INPUT_BUTTON_BASE + i;
                inputButtons.put(id, input);
                int inputWidth = input instanceof Dialog.SingleOptionInput
                    ? Math.min(((Dialog.SingleOptionInput) input).getWidth(), maxInputWidth)
                    : input instanceof Dialog.BooleanInput
                        ? Math.min(fontRendererObj.getStringWidth(label(input.getLabel())) + 20, maxInputWidth)
                        : maxInputWidth;
                addButton(
                    input instanceof Dialog.BooleanInput
                        ? new DialogCheckboxButton(
                            id,
                            panelLeft + (panelWidth - inputWidth) / 2,
                            rowY,
                            inputWidth,
                            i,
                            inputButtonLabel(i))
                        : new GuiButton(
                            id,
                            panelLeft + (panelWidth - inputWidth) / 2,
                            rowY,
                            inputWidth,
                            20,
                            inputButtonLabel(i)));
            }
        }

        int y = contentTop + bodyHeight
            + (dialog.getBody()
                .isEmpty() ? 0 : 10)
            + inputHeight
            + (dialog.getInputs()
                .isEmpty() ? 0 : 10)
            - contentScrollOffset;
        if (dialog.getType() == Dialog.Type.NOTICE) {
            int buttonWidth = actionWidths.get(0);
            addButton(
                new GuiButton(
                    0,
                    DialogLayout.buttonX(panelLeft, panelWidth, actionWidths, 1, 0),
                    footerTop(),
                    buttonWidth,
                    20,
                    label(
                        dialog.getActions()
                            .get(0)
                            .getLabel())));
        } else if (dialog.getType() == Dialog.Type.CONFIRMATION) {
            addButton(
                new GuiButton(
                    0,
                    DialogLayout.buttonX(panelLeft, panelWidth, actionWidths, 2, 0, buttonGap),
                    footerTop(),
                    actionWidths.get(0),
                    20,
                    label(
                        dialog.getActions()
                            .get(0)
                            .getLabel())));
            addButton(
                new GuiButton(
                    1,
                    DialogLayout.buttonX(panelLeft, panelWidth, actionWidths, 2, 1, buttonGap),
                    footerTop(),
                    actionWidths.get(1),
                    20,
                    label(
                        dialog.getActions()
                            .get(1)
                            .getLabel())));
        } else if (dialog.getType() == Dialog.Type.DIALOG_LIST) {
            for (int i = 0; i < dialog.getEntries()
                .size(); i++) {
                int buttonWidth = actionWidths.get(i);
                addButton(
                    new GuiButton(
                        i,
                        DialogLayout.buttonX(panelLeft, panelWidth, actionWidths, columns, i, buttonGap),
                        y + i / columns * 24,
                        buttonWidth,
                        20,
                        label(
                            dialog.getEntries()
                                .get(i)
                                .getLabel())));
            }
        } else if (dialog.getType() == Dialog.Type.SERVER_LINKS) {
            List<String> labels = serverLinkLabels(dialog);
            for (int i = 0; i < dialog.getServerLinks()
                .size(); i++) {
                int buttonWidth = actionWidths.get(i);
                addButton(
                    new GuiButton(
                        i,
                        DialogLayout.buttonX(panelLeft, panelWidth, actionWidths, columns, i, buttonGap),
                        y + i / columns * 24,
                        buttonWidth,
                        20,
                        labels.get(i)));
            }
        } else {
            for (int i = 0; i < dialog.getActions()
                .size(); i++) {
                addButton(
                    new GuiButton(
                        i,
                        DialogLayout.buttonX(panelLeft, panelWidth, actionWidths, columns, i, buttonGap),
                        y + i / columns * 24,
                        actionWidths.get(i),
                        20,
                        label(
                            dialog.getActions()
                                .get(i)
                                .getLabel())));
            }
        }
        if (dialog.getExitAction() != null) {
            int exitWidth = Math.min(
                dialog.getExitAction()
                    .getWidth(),
                maxInputWidth);
            addButton(
                new GuiButton(
                    controlCount(),
                    panelLeft + (panelWidth - exitWidth) / 2,
                    footerTop() + (simpleActions ? 24 : 0),
                    exitWidth,
                    20,
                    label(
                        dialog.getExitAction()
                            .getLabel())));
        }
        for (GuiButton button : buttonList) {
            if (isContentButton(button)) {
                button.visible = DialogListViewport
                    .fullyVisible(button.yPosition, button.height, contentTop, contentViewportHeight);
            }
        }
        if (previousFocus >= 0 && previousFocus < focusOrder.size()) focusControl(previousFocus);
        context.opened(sessionId, revision);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == WARNING_BUTTON_ID) {
            context.showWarning(sessionId, revision);
            return;
        }
        Dialog.Input input = inputButtons.get(button.id);
        if (input != null) {
            int inputIndex = button.id - INPUT_BUTTON_BASE;
            if (input instanceof Dialog.BooleanInput) {
                inputValues.set(inputIndex, !(Boolean) inputValues.get(inputIndex));
            } else if (input instanceof Dialog.NumberRangeInput) {
                stepSlider(inputIndex, 1);
            } else {
                Dialog.SingleOptionInput options = (Dialog.SingleOptionInput) input;
                int selected = optionIndex(options, (String) inputValues.get(inputIndex));
                inputValues.set(
                    inputIndex,
                    options.getOptions()
                        .get(
                            (selected + 1) % options.getOptions()
                                .size())
                        .getId());
            }
            button.displayString = inputButtonLabel(inputIndex);
            return;
        }
        Map<String, Object> submittedValues = currentValues();
        if (button.id < controlCount()) {
            submitAction(button.id, submittedValues);
        } else if (dialog.getExitAction() != null && button.id == controlCount()) {
            submitAction(button.id, submittedValues);
        }
    }

    private void submitAction(int index, Map<String, Object> submittedValues) {
        Dialog.Action action = actionForControl(index);
        if (action != null && action.getType() == Dialog.ActionType.OPEN_URL) {
            confirmOpenUrl(
                action,
                () -> { if (sendAction(index, submittedValues, action, false)) openUrl(action.getValue()); });
            return;
        }
        if (action != null && action.getType() == Dialog.ActionType.RUN_COMMAND) {
            if (dialog.getCommandExecutionPolicy()
                .requiresConfirmation()) {
                confirmRunCommand(action, submittedValues, () -> sendAction(index, submittedValues, action, false));
            } else {
                sendAction(index, submittedValues, action, false);
            }
            return;
        }
        sendAction(index, submittedValues, action, true);
    }

    private boolean sendAction(int index, Map<String, Object> submittedValues, Dialog.Action action,
        boolean applyLocalAction) {
        if (!context.action(
            sessionId,
            revision,
            index,
            DialogFormValues.forAction(action, submittedValues),
            dialog.getAfterAction())) return false;
        consumeLocalUse(index);
        if (applyLocalAction) applyLocalAction(action);
        return true;
    }

    private void consumeLocalUse(int index) {
        if (dialog.getAfterAction() != Dialog.AfterAction.NONE || index < 0 || index >= remainingUses.length) return;
        if (--remainingUses[index] > 0) return;
        for (GuiButton button : buttonList) {
            if (button.id == index) {
                button.enabled = false;
                return;
            }
        }
    }

    private void confirmOpenUrl(Dialog.Action action, Runnable accepted) {
        Minecraft minecraft = Minecraft.getMinecraft();
        GuiYesNoCallback callback = (confirmed, id) -> {
            if (id != 0) return;
            if (!confirmed) {
                context.restoreDialog(sessionId, revision);
                return;
            }
            accepted.run();
        };
        context.showConfirmation(sessionId, revision, new GuiConfirmOpenLink(callback, action.getValue(), 0, false));
    }

    private void confirmRunCommand(Dialog.Action action, Map<String, Object> submittedValues, Runnable accepted) {
        String preview = action.resolveCommand(dialog, submittedValues);
        if (preview.length() > 120) preview = preview.substring(0, 117) + "...";
        GuiYesNoCallback callback = (confirmed, id) -> {
            if (id != 0) return;
            if (confirmed) accepted.run();
            else context.restoreDialog(sessionId, revision);
        };
        context.showConfirmation(sessionId, revision, new GuiYesNo(callback, "Run this command as you?", preview, 0));
    }

    private static void openUrl(String value) {
        Minecraft minecraft = Minecraft.getMinecraft();
        try {
            Desktop.getDesktop()
                .browse(URI.create(value));
        } catch (Exception exception) {
            if (minecraft.thePlayer != null) {
                minecraft.thePlayer.addChatMessage(new ChatComponentText("Could not open dialog link."));
            }
        }
    }

    private Dialog.Action actionForControl(int index) {
        int controls = controlCount();
        if (dialog.getType() == Dialog.Type.SERVER_LINKS && index >= 0 && index < controls) {
            return Dialog.Action.openUrl(
                dialog.getServerLinks()
                    .get(index)
                    .getUrl());
        }
        if (index >= 0 && index < dialog.getActions()
            .size()) {
            return dialog.getActions()
                .get(index)
                .getAction();
        }
        if (index == controls && dialog.getExitAction() != null) {
            return dialog.getExitAction()
                .getAction();
        }
        int bodyIndex = index - controls - (dialog.getExitAction() == null ? 0 : 1);
        List<Dialog.Action> bodyActions = DialogBodyActions.collect(dialog);
        return bodyIndex < 0 || bodyIndex >= bodyActions.size() ? null : bodyActions.get(bodyIndex);
    }

    private static void applyLocalAction(Dialog.Action action) {
        if (action == null) return;
        if (action.getType() == Dialog.ActionType.COPY_TO_CLIPBOARD) {
            GuiScreen.setClipboardString(action.getValue());
        } else if (action.getType() == Dialog.ActionType.SUGGEST_COMMAND) {
            Minecraft.getMinecraft()
                .displayGuiScreen(new GuiChat(action.getValue()));
        }
    }

    @Override
    protected void keyTyped(char character, int keyCode) {
        if (keyCode == 1) {
            if (dialog.canCloseWithEscape()) {
                Dialog.Action cancelAction = dialog.getCancelAction() == null ? null
                    : dialog.getCancelAction()
                        .getAction();
                Map<String, Object> values = cancelAction != null && cancelAction.isDynamic() ? currentValues()
                    : java.util.Collections.emptyMap();
                if (cancelAction != null && cancelAction.getType() == Dialog.ActionType.OPEN_URL) {
                    confirmOpenUrl(
                        cancelAction,
                        () -> { if (context.cancel(sessionId, revision, values)) openUrl(cancelAction.getValue()); });
                } else if (cancelAction != null && cancelAction.getType() == Dialog.ActionType.RUN_COMMAND
                    && dialog.getCommandExecutionPolicy()
                        .requiresConfirmation()) {
                            confirmRunCommand(cancelAction, values, () -> context.cancel(sessionId, revision, values));
                        } else
                    if (context.cancel(sessionId, revision, values)) {
                        applyLocalAction(cancelAction);
                    }
            }
            return;
        }
        if (keyCode == Keyboard.KEY_TAB && !focusOrder.isEmpty()) {
            cycleFocus(Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT));
            return;
        }
        if (focusedControl >= 0 && focusedControl < focusOrder.size()) {
            Object focused = focusOrder.get(focusedControl);
            if (focused instanceof GuiTextField && ((GuiTextField) focused).textboxKeyTyped(character, keyCode)) return;
            if (focused instanceof DialogTextArea
                && ((DialogTextArea) focused).keyTyped(character, keyCode, fontRendererObj)) return;
            if (focused instanceof GuiButton) {
                GuiButton button = (GuiButton) focused;
                Dialog.Input input = inputButtons.get(button.id);
                int inputIndex = button.id - INPUT_BUTTON_BASE;
                if (input instanceof Dialog.NumberRangeInput) {
                    if (keyCode == Keyboard.KEY_LEFT) {
                        stepSlider(inputIndex, -1);
                        return;
                    }
                    if (keyCode == Keyboard.KEY_RIGHT) {
                        stepSlider(inputIndex, 1);
                        return;
                    }
                }
                if (button.enabled && (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_SPACE)) {
                    actionPerformed(button);
                    return;
                }
            }
        }
        super.keyTyped(character, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton == 0 && insideContentViewport(mouseY)) {
            for (BodyClickTarget target : bodyClickTargets) {
                if (target.contains(mouseX, mouseY)) {
                    Dialog.Action action = actionForControl(target.actionIndex);
                    submitAction(
                        target.actionIndex,
                        action != null && action.isDynamic() ? currentValues() : java.util.Collections.emptyMap());
                    return;
                }
            }
            for (Map.Entry<Integer, SliderBounds> entry : sliders.entrySet()) {
                SliderBounds bounds = entry.getValue();
                if (bounds.contains(mouseX, mouseY)) {
                    draggedSlider = entry.getKey();
                    for (GuiTextField field : textFields.values()) field.setFocused(false);
                    for (DialogTextArea area : textAreas.values()) area.setFocused(false);
                    focusControl(findFocusIndex(INPUT_BUTTON_BASE + entry.getKey()));
                    updateSlider(draggedSlider, mouseX);
                    return;
                }
            }
        }
        for (Map.Entry<Integer, DialogTextArea> entry : textAreas.entrySet()) {
            DialogTextArea area = entry.getValue();
            if (insideContentViewport(mouseY) && area.mouseClicked(mouseX, mouseY, mouseButton, fontRendererObj)) {
                for (GuiTextField field : textFields.values()) field.setFocused(false);
                for (DialogTextArea other : textAreas.values()) if (other != area) other.setFocused(false);
                focusControl(findFocusIndex(area));
                return;
            }
        }
        if (insideContentViewport(mouseY)) {
            for (GuiTextField field : textFields.values()) field.mouseClicked(mouseX, mouseY, mouseButton);
        }
        focusedControl = -1;
        for (int i = 0; i < focusOrder.size(); i++) {
            Object control = focusOrder.get(i);
            if (control instanceof GuiTextField && ((GuiTextField) control).isFocused()) focusedControl = i;
            if (control instanceof DialogTextArea && ((DialogTextArea) control).isFocused()) focusedControl = i;
            if (control instanceof GuiButton) {
                GuiButton button = (GuiButton) control;
                if (button.visible && mouseX >= button.xPosition
                    && mouseX <= button.xPosition + button.width
                    && mouseY >= button.yPosition
                    && mouseY <= button.yPosition + button.height) focusedControl = i;
            }
        }
        if (focusedControl >= 0) focusControl(focusedControl);
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (draggedSlider != null && clickedMouseButton == 0) updateSlider(draggedSlider, mouseX);
        for (DialogTextArea area : textAreas.values()) area.mouseDragged(mouseX, mouseY, fontRendererObj);
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
    }

    @Override
    protected void mouseMovedOrUp(int mouseX, int mouseY, int state) {
        super.mouseMovedOrUp(mouseX, mouseY, state);
        if (state == 0) draggedSlider = null;
        if (state == 0) for (DialogTextArea area : textAreas.values()) area.mouseReleased();
    }

    @Override
    public void updateScreen() {
        context.tick();
        for (GuiTextField field : textFields.values()) field.updateCursorCounter();
        for (DialogTextArea area : textAreas.values()) area.updateCursorCounter();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        bodyClickTargets.clear();
        context.drawBackground(this, partialTicks);
        drawRect(panelLeft, panelTop, panelLeft + panelWidth, panelTop + panelHeight, 0xE0101010);
        drawCenteredString(fontRendererObj, label(dialog.getTitle()), width / 2, panelTop + 14, 0xFFFFFF);
        Minecraft minecraft = Minecraft.getMinecraft();
        int scale = new ScaledResolution(minecraft, minecraft.displayWidth, minecraft.displayHeight).getScaleFactor();
        IntBuffer previousScissor = BufferUtils.createIntBuffer(16);
        GL11.glGetInteger(GL11.GL_SCISSOR_BOX, previousScissor);
        boolean scissorWasEnabled = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(
            panelLeft * scale,
            minecraft.displayHeight - (contentTop + contentViewportHeight) * scale,
            panelWidth * scale,
            contentViewportHeight * scale);
        try {
            int y = contentTop - contentScrollOffset;
            int bodyActionIndex = controlCount() + (dialog.getExitAction() == null ? 0 : 1);
            for (int bodyIndex = 0; bodyIndex < dialog.getBody()
                .size(); bodyIndex++) {
                Dialog.Body body = dialog.getBody()
                    .get(bodyIndex);
                int bodyWidth = bodyWidth(body);
                int bodyX = panelLeft + (panelWidth - bodyWidth) / 2;
                if (body instanceof Dialog.TextBody) {
                    IChatComponent component = ((Dialog.TextBody) body).getText();
                    List<String> lines = fontRendererObj
                        .listFormattedStringToWidth(component.getFormattedText(), bodyWidth);
                    for (int line = 0; line < lines.size(); line++) {
                        String text = lines.get(line);
                        int textX = bodyX + (bodyWidth - fontRendererObj.getStringWidth(text)) / 2;
                        fontRendererObj.drawString(text, textX, y + line * fontRendererObj.FONT_HEIGHT, 0xFFFFFF);
                    }
                    List<DialogBodyActions.Span> spans = DialogBodyActions
                        .spans(component, ((Dialog.TextBody) body).getActionSpans());
                    addBodyClickTargets(component, spans, bodyActionIndex, bodyX, y, bodyWidth, true);
                    bodyActionIndex += spans.size();
                } else {
                    Dialog.ItemBody itemBody = (Dialog.ItemBody) body;
                    ItemStack item = itemBody.getItem();
                    int iconX = bodyX;
                    IChatComponent description = itemBody.getDescription();
                    int textHeight = description == null ? 0
                        : fontRendererObj
                            .splitStringWidth(label(description), Math.max(1, bodyWidth - itemBody.getWidth() - 6));
                    int contentHeight = Math.max(itemBody.getHeight(), textHeight);
                    int iconDrawX = iconX + (itemBody.getWidth() - 16) / 2;
                    int iconY = y + 2 + (contentHeight - 16) / 2;
                    if (itemBody.showDecorations()) {
                        RenderItem.getInstance()
                            .renderItemAndEffectIntoGUI(
                                fontRendererObj,
                                minecraft.renderEngine,
                                item,
                                iconDrawX,
                                iconY);
                        RenderItem.getInstance()
                            .renderItemOverlayIntoGUI(fontRendererObj, minecraft.renderEngine, item, iconDrawX, iconY);
                    } else {
                        RenderItem.getInstance()
                            .renderItemIntoGUI(fontRendererObj, minecraft.renderEngine, item, iconDrawX, iconY);
                    }
                    if (description != null) {
                        int descriptionX = iconX + itemBody.getWidth() + 2;
                        int descriptionY = y + 2 + (contentHeight - textHeight) / 2;
                        int descriptionWidth = Math.max(1, bodyWidth - itemBody.getWidth() - 2);
                        fontRendererObj.drawSplitString(
                            label(description),
                            descriptionX,
                            descriptionY,
                            descriptionWidth,
                            0xFFFFFF);
                        List<DialogBodyActions.Span> spans = DialogBodyActions
                            .spans(description, itemBody.getDescriptionActionSpans());
                        addBodyClickTargets(
                            description,
                            spans,
                            bodyActionIndex,
                            descriptionX,
                            descriptionY,
                            descriptionWidth,
                            false);
                        bodyActionIndex += spans.size();
                    }
                }
                y += bodyElementHeight(body, bodyWidth) + (bodyIndex + 1 < dialog.getBody()
                    .size() ? 10 : 0);
            }
            int inputsTop = contentTop + bodyHeight()
                + (dialog.getBody()
                    .isEmpty() ? 0 : 10)
                - contentScrollOffset;
            FontRenderer font = fontRendererObj;
            for (int i = 0; i < dialog.getInputs()
                .size(); i++) {
                Dialog.Input input = dialog.getInputs()
                    .get(i);
                int rowY = inputsTop + inputTop(i);
                if (input instanceof Dialog.TextInput && ((Dialog.TextInput) input).isLabelVisible()) {
                    Dialog.TextInput text = (Dialog.TextInput) input;
                    int labelWidth = font.getStringWidth(label(text.getLabel()));
                    int labelGap = labelWidth == 0 ? 0 : 8;
                    int fieldWidth = Math.min(text.getWidth(), Math.max(1, panelWidth - 20 - labelWidth - labelGap));
                    int fieldX = DialogLayout.labeledControlX(panelLeft, panelWidth, labelWidth, labelGap, fieldWidth);
                    int labelY = rowY + (inputRowHeight(text) - font.FONT_HEIGHT) / 2;
                    font.drawString(label(text.getLabel()), fieldX - labelGap - labelWidth, labelY, 0xFFFFFF);
                }
                GuiTextField field = textFields.get(i);
                if (field != null) field.drawTextBox();
                DialogTextArea area = textAreas.get(i);
                if (area != null) drawTextArea(area);
            }
        } finally {
            GL11.glScissor(
                previousScissor.get(0),
                previousScissor.get(1),
                previousScissor.get(2),
                previousScissor.get(3));
            if (!scissorWasEnabled) GL11.glDisable(GL11.GL_SCISSOR_TEST);
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
        drawSliders();
        for (GuiButton button : buttonList) {
            if (button.visible && button.id == WARNING_BUTTON_ID
                && button.mousePressed(Minecraft.getMinecraft(), mouseX, mouseY)) {
                drawHoveringText(
                    Arrays.asList(I18n.format("gui.necrotempus.dialogs.warning.tooltip")),
                    mouseX,
                    mouseY,
                    fontRendererObj);
                break;
            }
            IChatComponent tooltip = actionTooltip(button.id);
            if (tooltip != null && button.visible && button.mousePressed(Minecraft.getMinecraft(), mouseX, mouseY)) {
                drawHoveringText(Arrays.asList(label(tooltip)), mouseX, mouseY, fontRendererObj);
                break;
            }
        }
        if (focusedControl >= 0 && focusedControl < focusOrder.size()
            && focusOrder.get(focusedControl) instanceof GuiButton
            && ((GuiButton) focusOrder.get(focusedControl)).visible) {
            GuiButton button = (GuiButton) focusOrder.get(focusedControl);
            drawRect(
                button.xPosition - 2,
                button.yPosition - 2,
                button.xPosition + button.width + 2,
                button.yPosition,
                0xFFFFFF00);
            drawRect(
                button.xPosition - 2,
                button.yPosition + button.height,
                button.xPosition + button.width + 2,
                button.yPosition + button.height + 2,
                0xFFFFFF00);
            drawRect(
                button.xPosition - 2,
                button.yPosition,
                button.xPosition,
                button.yPosition + button.height,
                0xFFFFFF00);
            drawRect(
                button.xPosition + button.width,
                button.yPosition,
                button.xPosition + button.width + 2,
                button.yPosition + button.height,
                0xFFFFFF00);
        }
        ItemStack hoveredItem = hoveredBodyItem(mouseX, mouseY);
        if (hoveredItem != null) renderToolTip(hoveredItem, mouseX, mouseY);
    }

    @Override
    public void onGuiClosed() {
        context.screenClosed(sessionId, revision);
    }

    @Override
    public void handleMouseInput() {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        int mouseX = Mouse.getEventX() * width / minecraft.displayWidth;
        int mouseY = height - Mouse.getEventY() * height / minecraft.displayHeight - 1;
        if (mouseX < panelLeft || mouseX > panelLeft + panelWidth || !insideContentViewport(mouseY)) return;
        int nextOffset = DialogListViewport
            .clampPixelOffset(contentScrollOffset + (wheel > 0 ? -24 : 24), contentHeight, contentViewportHeight);
        if (nextOffset == contentScrollOffset) return;
        contentScrollOffset = nextOffset;
        initGui();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return DialogScreenContext.shouldPauseGame(context, dialog.pausesGame());
    }

    private Map<String, Object> currentValues() {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < dialog.getInputs()
            .size(); i++) {
            Dialog.Input input = dialog.getInputs()
                .get(i);
            GuiTextField field = textFields.get(i);
            DialogTextArea area = textAreas.get(i);
            Object value = field != null ? field.getText()
                : area != null ? area.getBuffer()
                    .getText() : inputValues.get(i);
            result.put(input.getKey(), value);
        }
        return result;
    }

    private int controlCount() {
        if (dialog.getType() == Dialog.Type.DIALOG_LIST) return dialog.getEntries()
            .size();
        if (dialog.getType() == Dialog.Type.SERVER_LINKS) return dialog.getServerLinks()
            .size();
        return dialog.getActions()
            .size();
    }

    static List<String> serverLinkLabels(Dialog dialog) {
        List<String> labels = new ArrayList<>(
            dialog.getServerLinks()
                .size());
        for (Dialog.ServerLink link : dialog.getServerLinks()) labels.add(label(link.getLabel()));
        return labels;
    }

    private void updateSlider(int inputIndex, int mouseX) {
        Dialog.NumberRangeInput input = (Dialog.NumberRangeInput) dialog.getInputs()
            .get(inputIndex);
        SliderBounds bounds = sliders.get(inputIndex);
        float t = (float) (mouseX - bounds.x - 4) / Math.max(1, bounds.width - 8);
        inputValues.set(inputIndex, DialogSlider.valueAt(input, t));
        updateSliderLabel(inputIndex);
    }

    private void stepSlider(int inputIndex, int direction) {
        Dialog.NumberRangeInput input = (Dialog.NumberRangeInput) dialog.getInputs()
            .get(inputIndex);
        float value = (Float) inputValues.get(inputIndex);
        double next;
        if (input.getStep() != null) {
            double unit = (input.getEnd() >= input.getStart() ? 1 : -1) * input.getStep();
            double current = Math.round(((double) value - input.getInitial()) / unit);
            next = input.getInitial() + unit * (current + direction);
            if (next < Math.min(input.getStart(), input.getEnd()) || next > Math.max(input.getStart(), input.getEnd()))
                next = value;
        } else {
            double increment = Math.max(Math.abs((double) input.getEnd() - input.getStart()) / 100, Math.ulp(value));
            if (increment == 0) return;
            next = value + (input.getEnd() >= input.getStart() ? increment : -increment) * direction;
            next = Math.max(
                Math.min(input.getStart(), input.getEnd()),
                Math.min(Math.max(input.getStart(), input.getEnd()), next));
        }
        inputValues.set(inputIndex, (float) next);
        updateSliderLabel(inputIndex);
    }

    private void updateSliderLabel(int inputIndex) {
        int id = INPUT_BUTTON_BASE + inputIndex;
        String text = inputButtonLabel(inputIndex);
        for (GuiButton button : buttonList) {
            if (button.id == id) {
                button.displayString = text;
                return;
            }
        }
    }

    private int findFocusIndex(int buttonId) {
        for (int i = 0; i < focusOrder.size(); i++) {
            Object control = focusOrder.get(i);
            if (control instanceof GuiButton && ((GuiButton) control).id == buttonId) return i;
        }
        throw new IllegalArgumentException("Unknown focusable dialog button: " + buttonId);
    }

    private int findFocusIndex(DialogTextArea area) {
        for (int i = 0; i < focusOrder.size(); i++) if (focusOrder.get(i) == area) return i;
        throw new IllegalArgumentException("Unknown focusable dialog text area");
    }

    private void cycleFocus(boolean backwards) {
        for (GuiTextField field : textFields.values()) field.setFocused(false);
        for (DialogTextArea area : textAreas.values()) area.setFocused(false);
        int count = focusOrder.size();
        focusedControl = focusedControl < 0 ? (backwards ? count - 1 : 0)
            : (focusedControl + (backwards ? count - 1 : 1)) % count;
        focusControl(focusedControl);
    }

    private void focusControl(int index) {
        for (GuiTextField field : textFields.values()) field.setFocused(false);
        for (DialogTextArea area : textAreas.values()) area.setFocused(false);
        focusedControl = index;
        Object control = focusOrder.get(index);
        if (control instanceof GuiTextField) ((GuiTextField) control).setFocused(true);
        if (control instanceof DialogTextArea) ((DialogTextArea) control).setFocused(true);
    }

    private void drawTextArea(DialogTextArea area) {
        int x = area.getX();
        int y = area.getY();
        int width = area.getWidth();
        int height = area.getHeight();
        drawRect(x, y, x + width, y + height, area.isFocused() ? 0xFFFFFFFF : 0xFF888888);
        drawRect(x + 1, y + 1, x + width - 1, y + height - 1, 0xFF080808);

        Minecraft minecraft = Minecraft.getMinecraft();
        int scale = new ScaledResolution(minecraft, minecraft.displayWidth, minecraft.displayHeight).getScaleFactor();
        IntBuffer previousScissor = BufferUtils.createIntBuffer(16);
        GL11.glGetInteger(GL11.GL_SCISSOR_BOX, previousScissor);
        boolean scissorWasEnabled = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(
            (x + 2) * scale,
            minecraft.displayHeight - (y + height - 2) * scale,
            (width - 4) * scale,
            (height - 4) * scale);
        try {
            String text = area.getBuffer()
                .getText();
            int selectionStart = area.getBuffer()
                .getSelectionStart();
            int selectionEnd = area.getBuffer()
                .getSelectionEnd();
            int cursor = area.getBuffer()
                .getCursorPosition();
            int cursorLine = area.cursorLine();
            for (int row = 0; row < area.getVisibleLineCount(); row++) {
                int lineNumber = area.getFirstVisibleLine() + row;
                if (lineNumber >= area.getLineCount()) break;
                int lineStart = area.lineStart(lineNumber);
                int lineEnd = area.lineEnd(lineNumber);
                int lineY = y + 3 + row * 9;
                int selectedStart = Math.max(selectionStart, lineStart);
                int selectedEnd = Math.min(selectionEnd, lineEnd);
                if (selectedStart < selectedEnd) {
                    String before = text.substring(lineStart, selectedStart);
                    String selected = text.substring(selectedStart, selectedEnd);
                    String after = text.substring(selectedEnd, lineEnd);
                    int beforeWidth = fontRendererObj.getStringWidth(before);
                    int selectedWidth = fontRendererObj.getStringWidth(selected);
                    int selectedX = x + 4 + beforeWidth - area.getHorizontalScroll();
                    fontRendererObj.drawString(before, x + 4 - area.getHorizontalScroll(), lineY, 0xFFFFFF);
                    drawRect(selectedX, lineY - 1, selectedX + selectedWidth, lineY + 9, 0xFF0000FF);
                    fontRendererObj.drawString(selected, selectedX, lineY, 0xFFFFFF);
                    fontRendererObj.drawString(after, selectedX + selectedWidth, lineY, 0xFFFFFF);
                } else {
                    fontRendererObj.drawString(
                        text.substring(lineStart, lineEnd),
                        x + 4 - area.getHorizontalScroll(),
                        lineY,
                        0xFFFFFF);
                }
                if (area.shouldDrawCursor() && lineNumber == cursorLine && cursor >= lineStart && cursor <= lineEnd) {
                    int caretX = x + 4
                        + fontRendererObj.getStringWidth(text.substring(lineStart, cursor))
                        - area.getHorizontalScroll();
                    drawRect(caretX, lineY - 1, caretX + 1, lineY + 9, 0xFFFFFF00);
                }
            }
        } finally {
            GL11.glScissor(
                previousScissor.get(0),
                previousScissor.get(1),
                previousScissor.get(2),
                previousScissor.get(3));
            if (!scissorWasEnabled) GL11.glDisable(GL11.GL_SCISSOR_TEST);
        }
    }

    private void addButton(GuiButton button) {
        buttonList.add(button);
        focusOrder.add(button);
    }

    private int footerTop() {
        return contentTop + contentViewportHeight;
    }

    private boolean insideContentViewport(int mouseY) {
        return DialogListViewport.contains(mouseY, contentTop, contentViewportHeight);
    }

    private boolean isContentButton(GuiButton button) {
        if (button.id == WARNING_BUTTON_ID) return false;
        if (inputButtons.containsKey(button.id)) return true;
        if (dialog.getType() == Dialog.Type.NOTICE || dialog.getType() == Dialog.Type.CONFIRMATION) return false;
        return button.id >= 0 && button.id < controlCount();
    }

    private void drawSlider(Dialog.NumberRangeInput input, SliderBounds bounds, int inputIndex) {
        float value = (Float) inputValues.get(inputIndex);
        drawRect(bounds.x + 4, bounds.y, bounds.x + bounds.width - 4, bounds.y + 2, 0xFF777777);
        float origin = input.getStart();
        float end = input.getEnd();
        double range = (double) end - origin;
        float t = range == 0 ? 0 : (float) (((double) value - origin) / range);
        int handleX = bounds.x + 4 + Math.round(Math.max(0, Math.min(1, t)) * Math.max(0, bounds.width - 8));
        drawRect(handleX - 2, bounds.y - 4, handleX + 3, bounds.y + 6, 0xFFFFFFFF);
    }

    private void drawSliders() {
        for (Map.Entry<Integer, SliderBounds> entry : sliders.entrySet()) {
            int index = entry.getKey();
            GuiButton button = null;
            for (GuiButton candidate : buttonList) {
                if (candidate.id == INPUT_BUTTON_BASE + index) {
                    button = candidate;
                    break;
                }
            }
            if (button == null || !button.visible) continue;
            drawSlider(
                (Dialog.NumberRangeInput) dialog.getInputs()
                    .get(index),
                entry.getValue(),
                index);
            drawCenteredString(
                fontRendererObj,
                button.displayString,
                button.xPosition + button.width / 2,
                button.yPosition + 6,
                button.enabled ? 0xFFFFFF : 0xA0A0A0);
        }
    }

    private String inputButtonLabel(int inputIndex) {
        Dialog.Input input = dialog.getInputs()
            .get(inputIndex);
        if (input instanceof Dialog.BooleanInput) {
            return label(input.getLabel());
        }
        if (input instanceof Dialog.NumberRangeInput) {
            return I18n.format(
                ((Dialog.NumberRangeInput) input).getLabelFormat(),
                label(input.getLabel()),
                DialogSlider.valueToString((Float) inputValues.get(inputIndex)));
        }
        Dialog.SingleOptionInput options = (Dialog.SingleOptionInput) input;
        String selected = (String) inputValues.get(inputIndex);
        for (Dialog.Option option : options.getOptions()) {
            if (option.getId()
                .equals(selected)) {
                String value = label(option.getLabel());
                return options.isLabelVisible() ? I18n.format("options.generic_value", label(input.getLabel()), value)
                    : value;
            }
        }
        return options.isLabelVisible() ? I18n.format("options.generic_value", label(input.getLabel()), selected)
            : selected;
    }

    private static Object initialValue(Dialog.Input input) {
        if (input instanceof Dialog.TextInput) return ((Dialog.TextInput) input).getInitial();
        if (input instanceof Dialog.BooleanInput) return ((Dialog.BooleanInput) input).getInitial();
        if (input instanceof Dialog.SingleOptionInput) return ((Dialog.SingleOptionInput) input).getInitial();
        if (input instanceof Dialog.NumberRangeInput) return ((Dialog.NumberRangeInput) input).getInitial();
        throw new IllegalArgumentException("Unsupported dialog input");
    }

    private static int optionIndex(Dialog.SingleOptionInput input, String id) {
        for (int i = 0; i < input.getOptions()
            .size(); i++) {
            if (input.getOptions()
                .get(i)
                .getId()
                .equals(id)) return i;
        }
        return -1;
    }

    private static String label(IChatComponent component) {
        return component.getFormattedText();
    }

    private int requiredContentWidth() {
        int required = 200;
        for (Dialog.Body body : dialog.getBody()) {
            if (body instanceof Dialog.TextBody) {
                required = Math.max(required, ((Dialog.TextBody) body).getWidth());
            } else {
                Dialog.ItemBody item = (Dialog.ItemBody) body;
                required = Math.max(
                    required,
                    item.getWidth() + (item.getDescription() == null ? 0 : 2 + item.getDescriptionWidth()));
            }
        }
        for (Dialog.Input input : dialog.getInputs()) {
            if (input instanceof Dialog.TextInput) {
                Dialog.TextInput text = (Dialog.TextInput) input;
                required = Math.max(
                    required,
                    text.getWidth()
                        + (text.isLabelVisible() ? 10 + fontRendererObj.getStringWidth(label(text.getLabel())) : 0));
            } else if (input instanceof Dialog.BooleanInput) {
                required = Math.max(required, fontRendererObj.getStringWidth(label(input.getLabel())) + 20);
            } else if (input instanceof Dialog.SingleOptionInput) {
                required = Math.max(required, ((Dialog.SingleOptionInput) input).getWidth());
            } else if (input instanceof Dialog.NumberRangeInput) {
                required = Math.max(required, ((Dialog.NumberRangeInput) input).getWidth());
            }
        }
        List<Integer> widths = controlButtonWidths();
        boolean simpleActions = dialog.getType() == Dialog.Type.NOTICE || dialog.getType() == Dialog.Type.CONFIRMATION;
        int columns = dialog.getType() == Dialog.Type.NOTICE ? 1
            : dialog.getType() == Dialog.Type.CONFIRMATION ? 2 : dialog.getColumns();
        required = Math.max(required, DialogLayout.gridWidth(widths, columns, simpleActions ? 8 : 2));
        if (dialog.getExitAction() != null) required = Math.max(
            required,
            dialog.getExitAction()
                .getWidth());
        return required;
    }

    private List<Integer> controlButtonWidths() {
        List<Integer> result = new ArrayList<>();
        if (dialog.getType() == Dialog.Type.DIALOG_LIST || dialog.getType() == Dialog.Type.SERVER_LINKS) {
            int count = controlCount();
            for (int i = 0; i < count; i++) result.add(dialog.getButtonWidth());
        } else {
            for (Dialog.ActionButton action : dialog.getActions()) result.add(action.getWidth());
        }
        return result;
    }

    private int bodyWidth(Dialog.Body body) {
        int width;
        if (body instanceof Dialog.TextBody) width = ((Dialog.TextBody) body).getWidth();
        else {
            Dialog.ItemBody item = (Dialog.ItemBody) body;
            width = item.getWidth() + (item.getDescription() == null ? 0 : 2 + item.getDescriptionWidth());
        }
        return Math.min(width, Math.max(1, panelWidth - 24));
    }

    private int bodyHeight() {
        int result = 0;
        for (int i = 0; i < dialog.getBody()
            .size(); i++) {
            Dialog.Body body = dialog.getBody()
                .get(i);
            result += bodyElementHeight(body, bodyWidth(body));
            if (i + 1 < dialog.getBody()
                .size()) result += 10;
        }
        return result;
    }

    private int inputHeight() {
        int result = 0;
        for (int i = 0; i < dialog.getInputs()
            .size(); i++) {
            result += inputRowHeight(
                dialog.getInputs()
                    .get(i));
            if (i > 0) result += 10;
        }
        return result;
    }

    private int inputTop(int index) {
        int result = 0;
        for (int i = 0; i < index; i++) result += inputRowHeight(
            dialog.getInputs()
                .get(i))
            + 10;
        return result;
    }

    private int inputRowHeight(Dialog.Input input) {
        if (!(input instanceof Dialog.TextInput)) return 20;
        Dialog.TextInput text = (Dialog.TextInput) input;
        int controlHeight = text.getMaxLines() > 1 ? textHeight(text) : 18;
        return Math.max(INPUT_ROW_HEIGHT, controlHeight + 12);
    }

    private int textHeight(Dialog.TextInput input) {
        return input.getHeight() == null
            ? Math.min(512, Math.max(1, input.getMaxLines()) * fontRendererObj.FONT_HEIGHT + 8)
            : input.getHeight();
    }

    private int bodyElementHeight(Dialog.Body body, int wrapWidth) {
        if (body instanceof Dialog.TextBody) {
            return fontRendererObj.splitStringWidth(
                ((Dialog.TextBody) body).getText()
                    .getFormattedText(),
                wrapWidth);
        }
        Dialog.ItemBody itemBody = (Dialog.ItemBody) body;
        int itemHeight = itemBody.getHeight();
        if (itemBody.getDescription() == null) return itemHeight + 4;
        int textHeight = fontRendererObj
            .splitStringWidth(label(itemBody.getDescription()), Math.max(1, wrapWidth - itemBody.getWidth() - 2));
        return Math.max(itemHeight, textHeight) + 4;
    }

    private void addBodyClickTargets(IChatComponent component, List<DialogBodyActions.Span> spans, int actionBase,
        int x, int y, int wrapWidth, boolean centered) {
        if (spans.isEmpty()) return;
        List<String> lines = fontRendererObj.listFormattedStringToWidth(component.getFormattedText(), wrapWidth);
        String plainText = component.getUnformattedText();
        int searchFrom = 0;
        for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
            String line = lines.get(lineIndex);
            int lineX = centered ? x + (wrapWidth - fontRendererObj.getStringWidth(line)) / 2 : x;
            String visibleLine = stripFormatting(line);
            if (visibleLine.isEmpty()) continue;
            int lineStart = plainText.indexOf(visibleLine, searchFrom);
            while (lineStart < 0 && searchFrom < plainText.length()
                && Character.isWhitespace(plainText.charAt(searchFrom))) {
                searchFrom++;
                lineStart = plainText.indexOf(visibleLine, searchFrom);
            }
            if (lineStart < 0) continue;
            int lineEnd = lineStart + visibleLine.length();
            for (DialogBodyActions.Span span : spans) {
                int start = Math.max(span.getStart(), lineStart);
                int end = Math.min(span.getEnd(), lineEnd);
                if (start >= end) continue;
                int formattedStart = formattedOffset(line, start - lineStart);
                int formattedEnd = formattedOffset(line, end - lineStart);
                int left = lineX + fontRendererObj.getStringWidth(line.substring(0, formattedStart));
                int right = lineX + fontRendererObj.getStringWidth(line.substring(0, formattedEnd));
                if (right > left) {
                    bodyClickTargets.add(
                        new BodyClickTarget(
                            actionBase + span.getActionOrdinal(),
                            left,
                            y + lineIndex * fontRendererObj.FONT_HEIGHT,
                            right - left,
                            fontRendererObj.FONT_HEIGHT));
                }
            }
            searchFrom = lineEnd;
        }
    }

    private static String stripFormatting(String text) {
        StringBuilder result = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);
            if (character == '\u00a7' && i + 1 < text.length()) i++;
            else result.append(character);
        }
        return result.toString();
    }

    private static int formattedOffset(String text, int visibleCharacters) {
        int visible = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\u00a7' && i + 1 < text.length()) i++;
            else if (visible++ == visibleCharacters) return i;
        }
        return text.length();
    }

    private ItemStack hoveredBodyItem(int mouseX, int mouseY) {
        if (!insideContentViewport(mouseY)) return null;
        int y = contentTop - contentScrollOffset;
        for (int i = 0; i < dialog.getBody()
            .size(); i++) {
            Dialog.Body body = dialog.getBody()
                .get(i);
            int bodyWidth = bodyWidth(body);
            int bodyX = panelLeft + (panelWidth - bodyWidth) / 2;
            if (body instanceof Dialog.ItemBody) {
                Dialog.ItemBody itemBody = (Dialog.ItemBody) body;
                IChatComponent description = itemBody.getDescription();
                int descriptionHeight = description == null ? 0
                    : fontRendererObj
                        .splitStringWidth(label(description), Math.max(1, bodyWidth - itemBody.getWidth() - 2));
                int itemY = y + 2 + (Math.max(itemBody.getHeight(), descriptionHeight) - itemBody.getHeight()) / 2;
                if (itemBody.showTooltip() && mouseX >= bodyX
                    && mouseX < bodyX + itemBody.getWidth()
                    && mouseY >= itemY
                    && mouseY < itemY + itemBody.getHeight()) return itemBody.getItem();
            }
            y += bodyElementHeight(body, bodyWidth) + (i + 1 < dialog.getBody()
                .size() ? 10 : 0);
        }
        return null;
    }

    private IChatComponent actionTooltip(int index) {
        if (dialog.getType() == Dialog.Type.DIALOG_LIST || dialog.getType() == Dialog.Type.SERVER_LINKS) return null;
        if (dialog.getExitAction() != null && index == controlCount()) return dialog.getExitAction()
            .getTooltip();
        return index >= 0 && index < dialog.getActions()
            .size() ? dialog.getActions()
                .get(index)
                .getTooltip() : null;
    }

    private static final class BodyClickTarget {

        private final int actionIndex;
        private final int x;
        private final int y;
        private final int width;
        private final int height;

        private BodyClickTarget(int actionIndex, int x, int y, int width, int height) {
            this.actionIndex = actionIndex;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        private boolean contains(int mouseX, int mouseY) {
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        }
    }

    private static final class SliderBounds {

        private final int x;
        private final int y;
        private final int width;

        private SliderBounds(int x, int y, int width) {
            this.x = x;
            this.y = y;
            this.width = width;
        }

        private boolean contains(int mouseX, int mouseY) {
            return mouseX >= x && mouseX <= x + width && mouseY >= y - 10 && mouseY <= y + 10;
        }
    }

    private final class DialogCheckboxButton extends GuiButton {

        private final int inputIndex;

        private DialogCheckboxButton(int id, int x, int y, int width, int inputIndex, String label) {
            super(id, x, y, width, 20, label);
            this.inputIndex = inputIndex;
        }

        @Override
        public void drawButton(Minecraft minecraft, int mouseX, int mouseY) {
            if (!visible) return;
            boolean hovered = mouseX >= xPosition && mouseX < xPosition + width
                && mouseY >= yPosition
                && mouseY < yPosition + height;
            int boxY = yPosition + 5;
            GuiDialogScreen.this
                .drawRect(xPosition, boxY, xPosition + 10, boxY + 10, hovered ? 0xFFFFFFFF : 0xFFAAAAAA);
            GuiDialogScreen.this.drawRect(xPosition + 1, boxY + 1, xPosition + 9, boxY + 9, 0xFF202020);
            if ((Boolean) inputValues.get(inputIndex)) {
                GuiDialogScreen.this.fontRendererObj.drawString("x", xPosition + 2, boxY + 1, 0xFFFFFFFF);
            }
            GuiDialogScreen.this.fontRendererObj
                .drawString(displayString, xPosition + 14, yPosition + 6, enabled ? 0xE0E0E0 : 0xA0A0A0);
        }
    }
}
