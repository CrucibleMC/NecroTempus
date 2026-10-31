package io.github.cruciblemc.necrotempus.modules.features.dialog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.util.ChatComponentText;

import org.junit.jupiter.api.Test;

import io.github.cruciblemc.necrotempus.api.dialog.Dialog;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

class DialogFormValuesTest {

    @Test
    void validatesInputKeysTypesRangesAndOptionIdsAgainstTheDialog() {
        Dialog dialog = form();
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("name", "Grace");
        values.put("enabled", true);
        values.put("color", "blue");
        values.put("amount", 9.0f);

        assertEquals(values, DialogFormValues.validate(dialog, values));
        values.put("amount", 10.0f);
        assertThrows(IllegalArgumentException.class, () -> DialogFormValues.validate(dialog, values));
        values.put("amount", 9.0f);
        values.put("color", "green");
        assertThrows(IllegalArgumentException.class, () -> DialogFormValues.validate(dialog, values));
    }

    @Test
    void roundTripsOnlyKnownPrimitiveValueTypes() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("name", "Ada");
        values.put("enabled", false);
        values.put("color", "red");
        values.put("amount", 5.0f);
        ByteBuf buffer = Unpooled.buffer();
        try {
            DialogFormValues.write(buffer, values);
            assertEquals(values, DialogFormValues.read(buffer));
        } finally {
            buffer.release();
        }
    }

    @Test
    void roundTripsTheMaximumNumberOfValues() {
        Map<String, Object> values = new LinkedHashMap<>();
        for (int i = 0; i < 64; i++) values.put("value" + i, i % 2 == 0);
        ByteBuf buffer = Unpooled.buffer();
        try {
            DialogFormValues.write(buffer, values);
            assertEquals(values, DialogFormValues.read(buffer));
        } finally {
            buffer.release();
        }
    }

    @Test
    void rejectsMoreThanTheMaximumNumberOfValues() {
        Map<String, Object> values = new LinkedHashMap<>();
        for (int i = 0; i < 65; i++) values.put("value" + i, i % 2 == 0);
        ByteBuf buffer = Unpooled.buffer();
        try {
            assertThrows(IllegalArgumentException.class, () -> DialogFormValues.write(buffer, values));
            assertEquals(0, buffer.readableBytes());

            buffer.writeByte(65);
            buffer.writeByte(42);
            assertThrows(IllegalArgumentException.class, () -> DialogFormValues.read(buffer));
            assertEquals(0, buffer.readerIndex());
            assertEquals(2, buffer.readableBytes());
        } finally {
            buffer.release();
        }
    }

    @Test
    void writesBooleanAsOneByte() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("enabled", true);
        ByteBuf buffer = Unpooled.buffer();
        try {
            DialogFormValues.write(buffer, values);
            assertEquals(1, buffer.readUnsignedByte());
            assertEquals(7, buffer.readInt());
            buffer.skipBytes(7);
            assertEquals(1, buffer.readUnsignedByte());
            assertEquals(1, buffer.readUnsignedByte());
            buffer.readerIndex(0);
            assertEquals(values, DialogFormValues.read(buffer));
        } finally {
            buffer.release();
        }
    }

    @Test
    void validatesMultilineTextAndRejectsLineEndingInjection() {
        Dialog dialog = Dialog.notice(new ChatComponentText("Message"))
            .input(new Dialog.TextInput("message", new ChatComponentText("Message"), "", 64, 2))
            .action(new Dialog.ActionButton(new ChatComponentText("Send"), Dialog.Action.none()))
            .build();
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("message", "first\nsecond");

        assertEquals(values, DialogFormValues.validate(dialog, values));
        values.put("message", "first\nsecond\nthird");
        assertThrows(IllegalArgumentException.class, () -> DialogFormValues.validate(dialog, values));
        values.put("message", "first\r\nsecond");
        assertThrows(IllegalArgumentException.class, () -> DialogFormValues.validate(dialog, values));
    }

    @Test
    void validatesDescendingAndConstantNumberRangesAndRejectsNonFiniteValues() {
        Dialog descending = range(10, 0, null, 5);
        Dialog constant = range(5, 5, 0.5f, 5);
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("amount", 7.5f);
        assertEquals(values, DialogFormValues.validate(descending, values));

        values.put("amount", 5.0f);
        assertEquals(values, DialogFormValues.validate(constant, values));
        values.put("amount", Float.NaN);
        assertThrows(IllegalArgumentException.class, () -> DialogFormValues.validate(descending, values));
        values.put("amount", Float.POSITIVE_INFINITY);
        assertThrows(IllegalArgumentException.class, () -> DialogFormValues.validate(descending, values));
    }

    @Test
    void acceptsFourUlpsAroundTheCanonicalStepAndRejectsTheFifth() {
        Dialog dialog = range(0, 1, 0.1f, 0.1f);
        float canonical = (float) (0.1f + 2.0 * 0.1f);
        for (int i = 0; i < 4; i++) canonical = Math.nextUp(canonical);
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("amount", canonical);
        assertEquals(values, DialogFormValues.validate(dialog, values));

        values.put("amount", Math.nextUp(canonical));
        assertThrows(IllegalArgumentException.class, () -> DialogFormValues.validate(dialog, values));
    }

    @Test
    void rejectsStringsAboveTheirDeclaredLength() {
        Dialog dialog = Dialog.notice(new ChatComponentText("Name"))
            .input(new Dialog.TextInput("name", new ChatComponentText("Name"), "", 4, 1))
            .action(new Dialog.ActionButton(new ChatComponentText("Submit"), Dialog.Action.none()))
            .build();
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("name", "12345");

        assertThrows(IllegalArgumentException.class, () -> DialogFormValues.validate(dialog, values));
    }

    @Test
    void duplicateInputKeysUseTheLastInputDefinition() {
        Dialog dialog = Dialog.notice(new ChatComponentText("Duplicate"))
            .input(new Dialog.TextInput("name", new ChatComponentText("First"), "", 1, 1))
            .input(new Dialog.TextInput("name", new ChatComponentText("Second"), "", 16, 1))
            .action(new Dialog.ActionButton(new ChatComponentText("Submit"), Dialog.Action.none()))
            .build();
        Map<String, Object> values = Collections.singletonMap("name", "Grace");

        assertEquals(values, DialogFormValues.validate(dialog, values));
    }

    private static Dialog form() {
        return Dialog.multiAction(new ChatComponentText("Form"))
            .input(new Dialog.TextInput("name", new ChatComponentText("Name"), "Ada", 32, 1))
            .input(new Dialog.BooleanInput("enabled", new ChatComponentText("Enabled"), false, null, null))
            .input(
                new Dialog.SingleOptionInput(
                    "color",
                    new ChatComponentText("Color"),
                    Arrays.asList(
                        new Dialog.Option("red", new ChatComponentText("Red")),
                        new Dialog.Option("blue", new ChatComponentText("Blue"))),
                    "red"))
            .input(new Dialog.NumberRangeInput("amount", new ChatComponentText("Amount"), 0, 10, 4.0f, 1))
            .action(new Dialog.ActionButton(new ChatComponentText("Save"), Dialog.Action.none()))
            .build();
    }

    private static Dialog range(float start, float end, Float step, float initial) {
        return Dialog.notice(new ChatComponentText("Range"))
            .input(new Dialog.NumberRangeInput("amount", new ChatComponentText("Amount"), start, end, step, initial))
            .action(new Dialog.ActionButton(new ChatComponentText("Submit"), Dialog.Action.none()))
            .build();
    }
}
