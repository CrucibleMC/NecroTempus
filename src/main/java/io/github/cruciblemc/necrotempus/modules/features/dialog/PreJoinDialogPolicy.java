package io.github.cruciblemc.necrotempus.modules.features.dialog;

import java.util.List;

import net.minecraft.util.IChatComponent;

import io.github.cruciblemc.necrotempus.api.dialog.Dialog;
import io.github.cruciblemc.necrotempus.api.dialog.DialogBodyActions;
import io.github.cruciblemc.necrotempus.modules.features.dialog.network.PreJoinPacket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/** Validation for the dialog subset that can be shown before world entry. */
public final class PreJoinDialogPolicy {

    private static final int SHOW_FRAME_HEADER_BYTES = 2 + 2 * Long.BYTES + Integer.BYTES;

    private PreJoinDialogPolicy() {}

    public static void validate(Dialog dialog) {
        if (dialog == null) throw new IllegalArgumentException("Dialog is required");
        if (dialog.getType() != Dialog.Type.NOTICE && dialog.getType() != Dialog.Type.CONFIRMATION
            && dialog.getType() != Dialog.Type.MULTI_ACTION) {
            throw new IllegalArgumentException("Unsupported pre-join dialog type");
        }
        if (dialog.getAfterAction() != Dialog.AfterAction.WAIT_FOR_RESPONSE) {
            throw new IllegalArgumentException("Pre-join dialogs must wait for a response");
        }
        validateText(dialog.getTitle());
        validateText(dialog.getExternalTitle());
        for (Dialog.Body body : dialog.getBody()) {
            if (!(body instanceof Dialog.TextBody)) throw new IllegalArgumentException("Unsupported pre-join body");
            Dialog.TextBody text = (Dialog.TextBody) body;
            validateText(text.getText());
            if (!text.getActionSpans()
                .isEmpty()) throw new IllegalArgumentException("Body actions are not allowed");
        }
        for (Dialog.Input input : dialog.getInputs()) {
            validateText(input.getLabel());
            if (input instanceof Dialog.TextInput) {
                Dialog.TextInput text = (Dialog.TextInput) input;
                if (text.getMaxLines() != 1 || hasLineBreak(text.getInitial())) {
                    throw new IllegalArgumentException("Pre-join text inputs must be single-line");
                }
            } else if (input instanceof Dialog.BooleanInput) {
                Dialog.BooleanInput bool = (Dialog.BooleanInput) input;
                validateText(bool.getOnTrue());
                validateText(bool.getOnFalse());
            } else if (input instanceof Dialog.SingleOptionInput) {
                for (Dialog.Option option : ((Dialog.SingleOptionInput) input).getOptions()) {
                    validateText(option.getLabel());
                }
            } else if (!(input instanceof Dialog.NumberRangeInput)) {
                throw new IllegalArgumentException("Unsupported pre-join input");
            }
        }
        validateButtons(dialog.getActions());
        if (dialog.getExitAction() != null) validateButton(dialog.getExitAction());
        if (hasDynamicAction(dialog) && dialog.getInputs()
            .size() > DialogFormValues.MAX_INPUTS) {
            throw new IllegalArgumentException("Pre-join form exceeds the response input limit");
        }
        validateFrameSize(dialog);
    }

    private static boolean hasDynamicAction(Dialog dialog) {
        for (Dialog.ActionButton button : dialog.getActions()) {
            if (button.getAction()
                .isDynamic()) return true;
        }
        return dialog.getExitAction() != null && dialog.getExitAction()
            .getAction()
            .isDynamic();
    }

    private static void validateButtons(List<Dialog.ActionButton> buttons) {
        for (Dialog.ActionButton button : buttons) validateButton(button);
    }

    private static void validateButton(Dialog.ActionButton button) {
        validateText(button.getLabel());
        validateText(button.getTooltip());
        Dialog.Action action = button.getAction();
        if (action.getType() != Dialog.ActionType.NONE && action.getType() != Dialog.ActionType.CUSTOM) {
            throw new IllegalArgumentException("Unsupported pre-join action");
        }
    }

    private static void validateText(IChatComponent component) {
        if (component == null) return;
        if (hasLineBreak(component.getUnformattedTextForChat())) {
            throw new IllegalArgumentException("Pre-join text must be single-line");
        }
        if (!DialogBodyActions.spans(component)
            .isEmpty()) {
            throw new IllegalArgumentException("Text click actions are not allowed in pre-join dialogs");
        }
    }

    private static boolean hasLineBreak(String value) {
        return value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0;
    }

    private static void validateFrameSize(Dialog dialog) {
        ByteBuf buffer = Unpooled.buffer();
        try {
            DialogCodec.write(buffer, dialog);
            if (buffer.readableBytes() > PreJoinPacket.MAX_FRAME_BYTES - SHOW_FRAME_HEADER_BYTES) {
                throw new IllegalArgumentException("Pre-join dialog exceeds the packet limit");
            }
        } finally {
            buffer.release();
        }
    }
}
