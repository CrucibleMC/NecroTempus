package io.github.cruciblemc.necrotempus.api.dialog;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

final class DialogCommandTemplate {

    private static final int MAX_COMMAND_BYTES = 4096;

    private DialogCommandTemplate() {}

    static void validate(String template, boolean dynamic) {
        if (template == null || template.isEmpty()
            || template.getBytes(StandardCharsets.UTF_8).length > MAX_COMMAND_BYTES
            || template.indexOf('\n') >= 0
            || template.indexOf('\r') >= 0) {
            throw new IllegalArgumentException("Invalid dialog command");
        }
        if (dynamic) parse(template, null, null);
    }

    static String expand(String template, Dialog dialog, Map<String, Object> values) {
        return parse(template, dialog.getInputs(), values);
    }

    private static String parse(String template, List<Dialog.Input> inputs, Map<String, Object> values) {
        StringBuilder result = new StringBuilder(template.length());
        int literalStart = 0;
        int variableCount = 0;
        for (int i = 0; i < template.length();) {
            if (template.charAt(i) != '$' || i + 1 >= template.length() || template.charAt(i + 1) != '(') {
                i++;
                continue;
            }
            int end = template.indexOf(')', i + 2);
            if (end < 0) throw new IllegalArgumentException("Unterminated dialog command variable");
            String key = template.substring(i + 2, end);
            if (!validKey(key)) throw new IllegalArgumentException("Invalid dialog command variable: " + key);
            result.append(template, literalStart, i);
            result.append(substitution(key, inputs, values));
            variableCount++;
            i = end + 1;
            literalStart = i;
        }
        if (variableCount == 0) throw new IllegalArgumentException("Dynamic dialog commands need a variable");
        return result.append(template, literalStart, template.length())
            .toString();
    }

    private static String substitution(String key, List<Dialog.Input> inputs, Map<String, Object> values) {
        if (inputs == null || values == null) return "";
        Object value = values.get(key);
        if (value == null) return "";
        Dialog.Input effectiveInput = null;
        for (Dialog.Input input : inputs) {
            if (input.getKey()
                .equals(key)) effectiveInput = input;
        }
        if (effectiveInput instanceof Dialog.TextInput && value instanceof String) {
            return escapeWithoutQuotes((String) value);
        }
        if (effectiveInput instanceof Dialog.BooleanInput && value instanceof Boolean) {
            Dialog.BooleanInput bool = (Dialog.BooleanInput) effectiveInput;
            net.minecraft.util.IChatComponent component = (Boolean) value ? bool.getOnTrue() : bool.getOnFalse();
            return component == null ? Boolean.toString((Boolean) value) : component.getUnformattedText();
        }
        if (effectiveInput instanceof Dialog.SingleOptionInput && value instanceof String) {
            return (String) value;
        }
        if (effectiveInput instanceof Dialog.NumberRangeInput && value instanceof Float) {
            String number = Float.toString((Float) value);
            return number.endsWith(".0") ? number.substring(0, number.length() - 2) : number;
        }
        throw new IllegalArgumentException("Invalid value for dialog command variable: " + key);
    }

    private static String escapeWithoutQuotes(String value) {
        StringBuilder escaped = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            if (character == '"' || character == '\'' || character == '\\') escaped.append('\\');
            switch (character) {
                case '\b':
                    escaped.append("\\b");
                    break;
                case '\t':
                    escaped.append("\\t");
                    break;
                case '\n':
                    escaped.append("\\n");
                    break;
                case '\f':
                    escaped.append("\\f");
                    break;
                case '\r':
                    escaped.append("\\r");
                    break;
                default:
                    escaped.append(character);
            }
        }
        return escaped.toString();
    }

    static boolean validKey(String value) {
        if (value.isEmpty() || value.length() > 64) return false;
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            if (!Character.isLetterOrDigit(character) && character != '_') return false;
        }
        return true;
    }
}
