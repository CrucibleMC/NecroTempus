package io.github.cruciblemc.necrotempus.api.dialog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import net.minecraft.event.ClickEvent;
import net.minecraft.util.IChatComponent;

/** Resolves legacy text click events into the server-authorized dialog action table. */
public final class DialogBodyActions {

    private DialogBodyActions() {}

    public static List<Dialog.Action> collect(Dialog dialog) {
        List<Dialog.Action> actions = new ArrayList<>();
        for (Dialog.Body body : dialog.getBody()) {
            if (body instanceof Dialog.TextBody) {
                Dialog.TextBody text = (Dialog.TextBody) body;
                appendActions(text.getText(), text.getActionSpans(), actions);
            } else if (body instanceof Dialog.ItemBody) {
                Dialog.ItemBody item = (Dialog.ItemBody) body;
                IChatComponent description = item.getDescription();
                if (description != null) appendActions(description, item.getDescriptionActionSpans(), actions);
            }
        }
        return Collections.unmodifiableList(actions);
    }

    public static List<Span> spans(IChatComponent component) {
        return spans(component, Collections.emptyList());
    }

    public static List<Span> spans(IChatComponent component, List<Dialog.ActionSpan> explicitSpans) {
        List<Span> spans = new ArrayList<>();
        int textOffset = 0;
        Iterator<?> components = component.iterator();
        while (components.hasNext()) {
            IChatComponent part = (IChatComponent) components.next();
            String text = part.getUnformattedTextForChat();
            if (text.isEmpty()) continue;
            ClickEvent event = part.getChatStyle()
                .getChatClickEvent();
            if (event != null) spans.add(new Span(textOffset, textOffset + text.length(), 0, toAction(event)));
            textOffset += text.length();
        }
        for (Dialog.ActionSpan span : explicitSpans) {
            if (span.getEnd() > textOffset) throw new IllegalArgumentException("Body action span exceeds its text");
            spans.add(new Span(span.getStart(), span.getEnd(), 0, span.getAction()));
        }
        Collections.sort(spans, Comparator.comparingInt(Span::getStart));
        int previousEnd = 0;
        for (int i = 0; i < spans.size(); i++) {
            Span span = spans.get(i);
            if (span.start < previousEnd) throw new IllegalArgumentException("Dialog body click actions overlap");
            spans.set(i, new Span(span.start, span.end, i, span.action));
            previousEnd = span.end;
        }
        return Collections.unmodifiableList(spans);
    }

    private static void appendActions(IChatComponent component, List<Dialog.ActionSpan> explicitSpans,
        List<Dialog.Action> actions) {
        for (Span span : spans(component, explicitSpans)) actions.add(span.action);
    }

    private static Dialog.Action toAction(ClickEvent event) {
        if (event.getAction() == ClickEvent.Action.OPEN_URL) return Dialog.Action.openUrl(event.getValue());
        if (event.getAction() == ClickEvent.Action.RUN_COMMAND) return Dialog.Action.runCommand(event.getValue());
        if (event.getAction() == ClickEvent.Action.SUGGEST_COMMAND) {
            return Dialog.Action.suggestCommand(event.getValue());
        }
        throw new IllegalArgumentException("Unsupported dialog body click action");
    }

    public static final class Span {

        private final int start;
        private final int end;
        private final int actionOrdinal;
        private final Dialog.Action action;

        private Span(int start, int end, int actionOrdinal, Dialog.Action action) {
            this.start = start;
            this.end = end;
            this.actionOrdinal = actionOrdinal;
            this.action = action;
        }

        public int getStart() {
            return start;
        }

        public int getEnd() {
            return end;
        }

        public int getActionOrdinal() {
            return actionOrdinal;
        }

        public Dialog.Action getAction() {
            return action;
        }
    }
}
