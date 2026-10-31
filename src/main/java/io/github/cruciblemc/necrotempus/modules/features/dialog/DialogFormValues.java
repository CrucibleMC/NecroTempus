package io.github.cruciblemc.necrotempus.modules.features.dialog;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import io.github.cruciblemc.necrotempus.api.dialog.Dialog;
import io.netty.buffer.ByteBuf;

public final class DialogFormValues {

    public static final int MAX_RESPONSE_BYTES = 16384;
    static final int MAX_INPUTS = 64;
    private static final int MAX_KEY_BYTES = 64;
    private static final int MAX_TEXT_BYTES = 4096;
    private static final Pattern INPUT_KEY = Pattern.compile("[A-Za-z_][A-Za-z0-9_.-]{0,63}");

    private DialogFormValues() {}

    public static Map<String, Object> forAction(Dialog.Action action, Map<String, Object> values) {
        return action != null && action.isDynamic() ? values : Collections.emptyMap();
    }

    public static Map<String, Object> validate(Dialog dialog, Map<String, Object> values) {
        if (dialog == null || values == null) {
            throw new IllegalArgumentException("Dialog input values do not match the definition");
        }
        Map<String, Dialog.Input> inputs = new LinkedHashMap<>();
        for (Dialog.Input input : dialog.getInputs()) inputs.put(input.getKey(), input);
        if (values.size() != inputs.size()) {
            throw new IllegalArgumentException("Dialog input values do not match the definition");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, Dialog.Input> entry : inputs.entrySet()) {
            String key = entry.getKey();
            Dialog.Input input = entry.getValue();
            if (!values.containsKey(key)) throw new IllegalArgumentException("Missing dialog input: " + key);
            Object value = values.get(key);
            if (input instanceof Dialog.TextInput) {
                if (!(value instanceof String)) throw invalidType(key);
                String text = (String) value;
                Dialog.TextInput textInput = (Dialog.TextInput) input;
                if (text.indexOf('\r') >= 0 || text.length() > textInput.getMaxLength()
                    || text.split("\n", -1).length > textInput.getMaxLines()) {
                    throw new IllegalArgumentException("Dialog text input exceeds its limits: " + key);
                }
            } else if (input instanceof Dialog.BooleanInput) {
                if (!(value instanceof Boolean)) throw invalidType(key);
            } else if (input instanceof Dialog.SingleOptionInput) {
                if (!(value instanceof String)) throw invalidType(key);
                boolean found = false;
                for (Dialog.Option option : ((Dialog.SingleOptionInput) input).getOptions()) {
                    if (option.getId()
                        .equals(value)) {
                        found = true;
                        break;
                    }
                }
                if (!found) throw new IllegalArgumentException("Unknown option for dialog input: " + key);
            } else if (input instanceof Dialog.NumberRangeInput) {
                if (!(value instanceof Float)) throw invalidType(key);
                validateNumber(key, (Dialog.NumberRangeInput) input, (Float) value);
            } else {
                throw new IllegalArgumentException("Unsupported dialog input: " + key);
            }
            result.put(key, value);
        }
        return Collections.unmodifiableMap(result);
    }

    public static void write(ByteBuf buffer, Map<String, Object> values) {
        int start = buffer.writerIndex();
        try {
            if (values == null || values.size() > MAX_INPUTS)
                throw new IllegalArgumentException("Too many dialog values");
            buffer.writeByte(values.size());
            for (Map.Entry<String, Object> entry : values.entrySet()) {
                writeString(buffer, entry.getKey(), MAX_KEY_BYTES);
                Object value = entry.getValue();
                if (value instanceof String) {
                    buffer.writeByte(0);
                    writeString(buffer, (String) value, MAX_TEXT_BYTES);
                } else if (value instanceof Boolean) {
                    buffer.writeByte(1);
                    buffer.writeByte((Boolean) value ? 1 : 0);
                } else if (value instanceof Float && Float.isFinite((Float) value)) {
                    buffer.writeByte(2);
                    buffer.writeFloat((Float) value);
                } else {
                    throw new IllegalArgumentException("Unsupported dialog value type");
                }
            }
            if (buffer.writerIndex() - start > MAX_RESPONSE_BYTES)
                throw new IllegalArgumentException("Dialog response is too large");
        } catch (RuntimeException exception) {
            buffer.writerIndex(start);
            throw exception;
        }
    }

    public static Map<String, Object> read(ByteBuf buffer) {
        int start = buffer.readerIndex();
        try {
            if (buffer.readableBytes() > MAX_RESPONSE_BYTES)
                throw new IllegalArgumentException("Dialog response is too large");
            int count = buffer.readUnsignedByte();
            if (count > MAX_INPUTS) throw new IllegalArgumentException("Too many dialog values");
            Map<String, Object> values = new LinkedHashMap<>();
            for (int i = 0; i < count; i++) {
                String key = readString(buffer, MAX_KEY_BYTES);
                if (!INPUT_KEY.matcher(key)
                    .matches() || values.containsKey(key)) {
                    throw new IllegalArgumentException("Invalid or duplicate dialog input key");
                }
                int type = buffer.readUnsignedByte();
                Object value;
                if (type == 0) {
                    value = readString(buffer, MAX_TEXT_BYTES);
                } else if (type == 1) {
                    int bool = buffer.readUnsignedByte();
                    if (bool > 1) throw new IllegalArgumentException("Invalid boolean dialog value");
                    value = bool == 1;
                } else if (type == 2) {
                    float number = buffer.readFloat();
                    if (!Float.isFinite(number)) throw new IllegalArgumentException("Invalid numeric dialog value");
                    value = number;
                } else {
                    throw new IllegalArgumentException("Unknown dialog value type");
                }
                values.put(key, value);
            }
            if (buffer.isReadable()) throw new IllegalArgumentException("Trailing dialog value data");
            return Collections.unmodifiableMap(values);
        } catch (IndexOutOfBoundsException exception) {
            buffer.readerIndex(start);
            throw new IllegalArgumentException("Truncated dialog values", exception);
        } catch (RuntimeException exception) {
            buffer.readerIndex(start);
            throw exception;
        }
    }

    private static void validateNumber(String key, Dialog.NumberRangeInput input, float value) {
        float start = input.getStart();
        float end = input.getEnd();
        if (!Float.isFinite(value) || value < Math.min(start, end) || value > Math.max(start, end)) {
            throw new IllegalArgumentException("Dialog number is outside its range: " + key);
        }
        Float step = input.getStep();
        if (step == null) return;
        double direction = end >= start ? 1 : -1;
        double offset = (value - input.getInitial()) / (direction * step);
        if (!Double.isFinite(offset)) throw new IllegalArgumentException("Dialog number is not on its step: " + key);
        double nearestStep = Math.rint(offset);
        double maxStep = Math.floor(Math.abs((end - (double) input.getInitial()) / step));
        if (nearestStep < 0 || nearestStep > maxStep) {
            throw new IllegalArgumentException("Dialog number is not on its step: " + key);
        }
        float canonical = (float) (input.getInitial() + direction * nearestStep * step);
        if (Math.abs(value - canonical) > 4 * Math.ulp(canonical)) {
            throw new IllegalArgumentException("Dialog number is not on its step: " + key);
        }
    }

    private static IllegalArgumentException invalidType(String key) {
        return new IllegalArgumentException("Invalid value type for dialog input: " + key);
    }

    private static void writeString(ByteBuf buffer, String value, int maxBytes) {
        if (value == null) throw new IllegalArgumentException("Dialog input string is missing");
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > maxBytes) throw new IllegalArgumentException("Dialog input string is too large");
        buffer.writeInt(bytes.length);
        buffer.writeBytes(bytes);
    }

    private static String readString(ByteBuf buffer, int maxBytes) {
        int length = buffer.readInt();
        if (length < 0 || length > maxBytes || length > buffer.readableBytes()) {
            throw new IllegalArgumentException("Invalid dialog input string length");
        }
        byte[] bytes = new byte[length];
        buffer.readBytes(bytes);
        try {
            return StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString();
        } catch (CharacterCodingException exception) {
            throw new IllegalArgumentException("Invalid UTF-8 dialog input", exception);
        }
    }
}
