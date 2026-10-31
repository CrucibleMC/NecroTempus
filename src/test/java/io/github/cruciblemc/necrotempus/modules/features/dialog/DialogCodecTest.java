package io.github.cruciblemc.necrotempus.modules.features.dialog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.util.ChatComponentText;

import org.junit.jupiter.api.Test;

import io.github.cruciblemc.necrotempus.api.dialog.Dialog;
import io.github.cruciblemc.necrotempus.api.dialog.DialogResponse;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

class DialogCodecTest {

    @Test
    void dynamicCustomActionRetainsFormValuesWithoutSendingItsPrivateData() {
        net.minecraft.nbt.NBTTagCompound privatePayload = new net.minecraft.nbt.NBTTagCompound();
        privatePayload.setString("private_payload", "secret");
        Dialog source = Dialog.notice(new ChatComponentText("Form"))
            .input(new Dialog.TextInput("name", new ChatComponentText("Name"), "", 32, 1))
            .action(
                new Dialog.ActionButton(
                    new ChatComponentText("Submit"),
                    Dialog.Action.dynamicCustom("private:submit", privatePayload)))
            .build();
        ByteBuf buffer = Unpooled.buffer();
        try {
            DialogCodec.write(buffer, source);
            assertFalse(
                io.netty.buffer.ByteBufUtil.hexDump(buffer)
                    .contains("707269766174653a7375626d6974"));
            assertFalse(
                io.netty.buffer.ByteBufUtil.hexDump(buffer)
                    .contains("736563726574"));
            Dialog decoded = DialogCodec.read(buffer);
            Dialog.Action action = decoded.getActions()
                .get(0)
                .getAction();
            Map<String, Object> values = new LinkedHashMap<>();
            values.put("name", "Ada");
            assertEquals(true, action.isDynamic());
            Map<String, Object> submitted = DialogFormValues.forAction(action, values);
            DialogResponse response = new DialogResponse(0, false, submitted, action);
            ByteBuf responseBuffer = Unpooled.buffer();
            try {
                DialogFormValues.write(responseBuffer, response.getInputValues());
                Map<String, Object> received = DialogFormValues.read(responseBuffer);
                assertEquals(Collections.singletonMap("name", "Ada"), DialogFormValues.validate(source, received));
                assertThrows(
                    IllegalArgumentException.class,
                    () -> DialogFormValues
                        .validate(source, Collections.singletonMap("name", "012345678901234567890123456789012")));
            } finally {
                responseBuffer.release();
            }

            Dialog staticDialog = Dialog.notice(new ChatComponentText("Static"))
                .input(new Dialog.TextInput("name", new ChatComponentText("Name"), "", 32, 1))
                .action(
                    new Dialog.ActionButton(
                        new ChatComponentText("Submit"),
                        Dialog.Action.custom("private:static", new net.minecraft.nbt.NBTTagCompound())))
                .build();
            ByteBuf staticBuffer = Unpooled.buffer();
            try {
                DialogCodec.write(staticBuffer, staticDialog);
                Dialog staticDecoded = DialogCodec.read(staticBuffer);
                assertEquals(
                    Collections.emptyMap(),
                    DialogFormValues.forAction(
                        staticDecoded.getActions()
                            .get(0)
                            .getAction(),
                        values));
            } finally {
                staticBuffer.release();
            }
        } finally {
            buffer.release();
        }
    }

    @Test
    void rejectsOversizedNbtArraysBeforeNbtAllocation() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream nbt = new DataOutputStream(bytes);
        nbt.writeByte(10);
        nbt.writeUTF("");
        nbt.writeByte(7);
        nbt.writeUTF("payload");
        nbt.writeInt(Integer.MAX_VALUE);
        nbt.writeByte(0);

        assertThrows(IOException.class, () -> DialogCodec.validateNbt(bytes.toByteArray()));
    }

    @Test
    void roundTripsNoticePresentationWithoutSendingServerAction() {
        Dialog source = Dialog.notice(new ChatComponentText("Test title"))
            .externalTitle(new ChatComponentText("Menu label"))
            .body(new Dialog.TextBody(new ChatComponentText("Test body")))
            .action(new Dialog.ActionButton(new ChatComponentText("Continue"), Dialog.Action.none()))
            .build();
        ByteBuf buffer = Unpooled.buffer();
        try {
            DialogCodec.write(buffer, source);
            Dialog decoded = DialogCodec.read(buffer);
            assertEquals(
                "Test title",
                decoded.getTitle()
                    .getUnformattedText());
            assertEquals(
                "Menu label",
                decoded.getExternalTitle()
                    .getUnformattedText());
            assertEquals(
                "Test body",
                ((Dialog.TextBody) decoded.getBody()
                    .get(0)).getText()
                        .getUnformattedText());
            assertEquals(
                "Continue",
                decoded.getActions()
                    .get(0)
                    .getLabel()
                    .getUnformattedText());
            assertEquals(
                Dialog.ActionType.NONE,
                decoded.getActions()
                    .get(0)
                    .getAction()
                    .getType());
        } finally {
            buffer.release();
        }
    }

    @Test
    void roundTripsVanillaDimensionsLabelsAndTooltips() {
        Dialog source = Dialog.multiAction(new ChatComponentText("Layout"))
            .columns(3)
            .body(new Dialog.TextBody(new ChatComponentText("Body"), 360))
            .input(new Dialog.TextInput("name", new ChatComponentText("Name"), "Ada", 32, 3, 280, false, 96))
            .action(
                new Dialog.ActionButton(
                    new ChatComponentText("Submit"),
                    Dialog.Action.none(),
                    new ChatComponentText("Submit this form"),
                    220))
            .build();
        ByteBuf buffer = Unpooled.buffer();
        try {
            DialogCodec.write(buffer, source);
            Dialog decoded = DialogCodec.read(buffer);
            Dialog.TextInput input = (Dialog.TextInput) decoded.getInputs()
                .get(0);

            assertEquals(3, decoded.getColumns());
            assertEquals(
                360,
                ((Dialog.TextBody) decoded.getBody()
                    .get(0)).getWidth());
            assertEquals(280, input.getWidth());
            assertFalse(input.isLabelVisible());
            assertEquals(96, input.getHeight());
            assertEquals(
                220,
                decoded.getActions()
                    .get(0)
                    .getWidth());
            assertEquals(
                "Submit this form",
                decoded.getActions()
                    .get(0)
                    .getTooltip()
                    .getUnformattedText());
        } finally {
            buffer.release();
        }
    }

    @Test
    void dialogListWireDataContainsLabelsButKeepsTargetsServerSide() {
        Dialog target = Dialog.notice(new ChatComponentText("Private target"))
            .action(new Dialog.ActionButton(new ChatComponentText("Close"), Dialog.Action.none()))
            .build();
        Dialog source = Dialog.dialogList(new ChatComponentText("Menu"))
            .externalTitle(new ChatComponentText("Options"))
            .body(new Dialog.TextBody(new ChatComponentText("Choose a dialog.")))
            .entry(new Dialog.Entry(new ChatComponentText("First"), target))
            .build();
        ByteBuf buffer = Unpooled.buffer();
        ByteBuf clientCopy = Unpooled.buffer();
        try {
            DialogCodec.write(buffer, source);
            Dialog decoded = DialogCodec.read(buffer);
            assertEquals(Dialog.Type.DIALOG_LIST, decoded.getType());
            assertEquals(
                "Options",
                decoded.getExternalTitle()
                    .getUnformattedText());
            assertEquals(
                "First",
                decoded.getEntries()
                    .get(0)
                    .getLabel()
                    .getUnformattedText());
            assertNull(
                decoded.getEntries()
                    .get(0)
                    .getDialogId());
            assertFalse(
                decoded.getEntries()
                    .get(0)
                    .hasTarget());
            assertThrows(IllegalArgumentException.class, () -> DialogCodec.write(clientCopy, decoded));
        } finally {
            buffer.release();
            clientCopy.release();
        }
    }

    @Test
    void roundTripsLongDialogListsPastOldLayoutLimit() {
        Dialog.Builder builder = Dialog.dialogList(new ChatComponentText("Many dialogs"));
        for (int i = 0; i < 33; i++) {
            builder.entry(new Dialog.Entry(new ChatComponentText("Dialog " + i), "test:dialog_" + i));
        }
        ByteBuf buffer = Unpooled.buffer();
        try {
            DialogCodec.write(buffer, builder.build());
            assertEquals(
                33,
                DialogCodec.read(buffer)
                    .getEntries()
                    .size());
        } finally {
            buffer.release();
        }
    }

    @Test
    void rejectsListInputsUntilValuesCanSurviveNavigation() {
        Dialog list = Dialog.dialogList(new ChatComponentText("Menu"))
            .input(new Dialog.TextInput("name", new ChatComponentText("Name"), "", 16, 1))
            .entry(new Dialog.Entry(new ChatComponentText("Open"), "test:target"))
            .build();
        ByteBuf buffer = Unpooled.buffer();
        try {
            assertThrows(IllegalArgumentException.class, () -> DialogCodec.write(buffer, list));
        } finally {
            buffer.release();
        }
    }

    @Test
    void roundTripsSupportedTypedInputs() {
        Dialog form = Dialog.multiAction(new ChatComponentText("Form"))
            .input(new Dialog.TextInput("name", new ChatComponentText("Name"), "Ada", 32, 1))
            .input(
                new Dialog.BooleanInput(
                    "enabled",
                    new ChatComponentText("Enabled"),
                    true,
                    new ChatComponentText("Yes"),
                    new ChatComponentText("No")))
            .input(
                new Dialog.SingleOptionInput(
                    "role",
                    new ChatComponentText("Role"),
                    java.util.Arrays.asList(
                        new Dialog.Option("guest", new ChatComponentText("Guest")),
                        new Dialog.Option("admin", new ChatComponentText("Admin"))),
                    "admin"))
            .input(new Dialog.NumberRangeInput("amount", new ChatComponentText("Amount"), 0, 10, 2.0f, 4))
            .action(
                new Dialog.ActionButton(
                    new ChatComponentText("Send"),
                    Dialog.Action.dynamicCustom("test:submit", new net.minecraft.nbt.NBTTagCompound())))
            .build();
        ByteBuf buffer = Unpooled.buffer();
        try {
            DialogCodec.write(buffer, form);
            Dialog decoded = DialogCodec.read(buffer);
            assertEquals(
                4,
                decoded.getInputs()
                    .size());
            assertEquals(
                "Ada",
                ((Dialog.TextInput) decoded.getInputs()
                    .get(0)).getInitial());
            assertEquals(
                true,
                ((Dialog.BooleanInput) decoded.getInputs()
                    .get(1)).getInitial());
            assertEquals(
                "admin",
                ((Dialog.SingleOptionInput) decoded.getInputs()
                    .get(2)).getInitial());
            assertEquals(
                2.0f,
                ((Dialog.NumberRangeInput) decoded.getInputs()
                    .get(3)).getStep());
        } finally {
            buffer.release();
        }
    }

    @Test
    void roundTripsMultilineTextLimitsAndInitialValue() {
        Dialog form = Dialog.notice(new ChatComponentText("Multiline"))
            .input(new Dialog.TextInput("message", new ChatComponentText("Message"), "first\nsecond", 128, 3))
            .action(
                new Dialog.ActionButton(
                    new ChatComponentText("Send"),
                    Dialog.Action.dynamicCustom("test:submit", new net.minecraft.nbt.NBTTagCompound())))
            .build();
        ByteBuf buffer = Unpooled.buffer();
        try {
            DialogCodec.write(buffer, form);
            Dialog.TextInput decoded = (Dialog.TextInput) DialogCodec.read(buffer)
                .getInputs()
                .get(0);
            assertEquals(3, decoded.getMaxLines());
            assertEquals("first\nsecond", decoded.getInitial());
        } finally {
            buffer.release();
        }
    }

    @Test
    void rejectsCarriageReturnsInTextInputInitialValues() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new Dialog.TextInput("message", new ChatComponentText("Message"), "first\r\nsecond", 128, 3));
    }

    @Test
    void letsTheClientConfirmACommandThatOnlyTheServerExecutes() {
        Dialog command = Dialog.notice(new ChatComponentText("Command"))
            .action(new Dialog.ActionButton(new ChatComponentText("Run"), Dialog.Action.runCommand("/help")))
            .build();
        ByteBuf buffer = Unpooled.buffer();
        try {
            DialogCodec.write(buffer, command);
            Dialog.Action decoded = DialogCodec.read(buffer)
                .getActions()
                .get(0)
                .getAction();
            assertEquals(Dialog.ActionType.RUN_COMMAND, decoded.getType());
            assertEquals("/help", decoded.getValue());
        } finally {
            buffer.release();
        }
    }

    @Test
    void commandExecutionPolicyRoundTripsWithTheDialog() {
        Dialog source = Dialog.notice(new ChatComponentText("Command"))
            .commandExecutionPolicy(Dialog.CommandExecutionPolicy.NO_CONFIRMATION)
            .action(new Dialog.ActionButton(new ChatComponentText("Run"), Dialog.Action.runCommand("help")))
            .build();
        ByteBuf buffer = Unpooled.buffer();
        try {
            DialogCodec.write(buffer, source);
            assertEquals(
                Dialog.CommandExecutionPolicy.NO_CONFIRMATION,
                DialogCodec.read(buffer)
                    .getCommandExecutionPolicy());
        } finally {
            buffer.release();
        }
    }

    @Test
    void rejectsUnknownCommandExecutionPolicies() {
        ByteBuf buffer = Unpooled.buffer();
        try {
            DialogCodec.write(
                buffer,
                Dialog.notice(new ChatComponentText("Command"))
                    .build());
            buffer.setByte(3, 127);
            assertThrows(IllegalArgumentException.class, () -> DialogCodec.read(buffer));
        } finally {
            buffer.release();
        }
    }

    @Test
    void customActionPayloadStaysServerSideWhileItsButtonRoundTrips() {
        Dialog custom = Dialog.notice(new ChatComponentText("Custom"))
            .action(
                new Dialog.ActionButton(
                    new ChatComponentText("Run"),
                    Dialog.Action.custom("test:run", new net.minecraft.nbt.NBTTagString("private"))))
            .build();
        Dialog plain = Dialog.notice(new ChatComponentText("Custom"))
            .action(new Dialog.ActionButton(new ChatComponentText("Run"), Dialog.Action.none()))
            .build();
        ByteBuf customBuffer = Unpooled.buffer();
        ByteBuf plainBuffer = Unpooled.buffer();
        try {
            DialogCodec.write(customBuffer, custom);
            DialogCodec.write(plainBuffer, plain);
            org.junit.jupiter.api.Assertions.assertEquals(
                io.netty.buffer.ByteBufUtil.hexDump(plainBuffer),
                io.netty.buffer.ByteBufUtil.hexDump(customBuffer));
            assertEquals(
                "Run",
                DialogCodec.read(customBuffer)
                    .getActions()
                    .get(0)
                    .getLabel()
                    .getUnformattedText());
        } finally {
            customBuffer.release();
            plainBuffer.release();
        }
    }

    @Test
    void roundTripsSupportedLocalActionsWithoutSendingCustomMetadata() {
        Dialog source = Dialog.multiAction(new ChatComponentText("Local actions"))
            .action(
                new Dialog.ActionButton(new ChatComponentText("Copy"), Dialog.Action.copyToClipboard("copied text")))
            .action(new Dialog.ActionButton(new ChatComponentText("Suggest"), Dialog.Action.suggestCommand("/help")))
            .action(
                new Dialog.ActionButton(new ChatComponentText("Open"), Dialog.Action.openUrl("https://example.com")))
            .build();
        ByteBuf buffer = Unpooled.buffer();
        try {
            DialogCodec.write(buffer, source);
            Dialog decoded = DialogCodec.read(buffer);
            assertEquals(
                Dialog.ActionType.COPY_TO_CLIPBOARD,
                decoded.getActions()
                    .get(0)
                    .getAction()
                    .getType());
            assertEquals(
                "copied text",
                decoded.getActions()
                    .get(0)
                    .getAction()
                    .getValue());
            assertEquals(
                "/help",
                decoded.getActions()
                    .get(1)
                    .getAction()
                    .getValue());
            assertEquals(
                Dialog.ActionType.OPEN_URL,
                decoded.getActions()
                    .get(2)
                    .getAction()
                    .getType());
            assertEquals(
                "https://example.com",
                decoded.getActions()
                    .get(2)
                    .getAction()
                    .getValue());
        } finally {
            buffer.release();
        }
    }

    @Test
    void roundTripsNonClosingActionsAndFiniteUseLimits() {
        Dialog source = Dialog.multiAction(new ChatComponentText("Repeatable"))
            .pause(false)
            .afterAction(Dialog.AfterAction.NONE)
            .action(
                new Dialog.ActionButton(
                    new ChatComponentText("Twice"),
                    Dialog.Action.none()
                        .withMaxUses(2)))
            .build();
        ByteBuf buffer = Unpooled.buffer();
        try {
            DialogCodec.write(buffer, source);
            Dialog decoded = DialogCodec.read(buffer);
            assertEquals(Dialog.AfterAction.NONE, decoded.getAfterAction());
            assertEquals(
                2,
                decoded.getActions()
                    .get(0)
                    .getAction()
                    .getMaxUses());
        } finally {
            buffer.release();
        }
    }

    @Test
    void roundTripsWaitForResponseAfterAction() {
        Dialog source = Dialog.notice(new ChatComponentText("Wait"))
            .afterAction(Dialog.AfterAction.WAIT_FOR_RESPONSE)
            .action(new Dialog.ActionButton(new ChatComponentText("Submit"), Dialog.Action.none()))
            .build();
        ByteBuf buffer = Unpooled.buffer();
        try {
            DialogCodec.write(buffer, source);
            assertEquals(
                Dialog.AfterAction.WAIT_FOR_RESPONSE,
                DialogCodec.read(buffer)
                    .getAfterAction());
        } finally {
            buffer.release();
        }
    }

    @Test
    void roundTripsDynamicCommandTemplateAndUseLimit() {
        Dialog source = Dialog.notice(new ChatComponentText("Run command"))
            .input(new Dialog.TextInput("name", new ChatComponentText("Name"), "Alex", 32, 1))
            .action(
                new Dialog.ActionButton(
                    new ChatComponentText("Run"),
                    Dialog.Action.dynamicCommand("say $(name)")
                        .withMaxUses(3)))
            .build();
        ByteBuf buffer = Unpooled.buffer();
        try {
            DialogCodec.write(buffer, source);
            Dialog decoded = DialogCodec.read(buffer);
            Dialog.Action action = decoded.getActions()
                .get(0)
                .getAction();
            assertEquals(Dialog.ActionType.RUN_COMMAND, action.getType());
            assertEquals("say $(name)", action.getValue());
            assertEquals(3, action.getMaxUses());
            assertEquals(true, action.isDynamic());
        } finally {
            buffer.release();
        }
    }

    @Test
    void showDialogActionKeepsItsTypeWithoutSendingItsRegisteredTargetToTheClient() {
        Dialog navigation = Dialog.notice(new ChatComponentText("Navigate"))
            .action(
                new Dialog.ActionButton(
                    new ChatComponentText("Continue"),
                    Dialog.Action.showDialog("test:registered_target")))
            .build();
        Dialog generic = Dialog.notice(new ChatComponentText("Navigate"))
            .action(new Dialog.ActionButton(new ChatComponentText("Continue"), Dialog.Action.none()))
            .build();
        ByteBuf navigationBuffer = Unpooled.buffer();
        ByteBuf genericBuffer = Unpooled.buffer();
        try {
            DialogCodec.write(navigationBuffer, navigation);
            DialogCodec.write(genericBuffer, generic);
            assertFalse(
                io.netty.buffer.ByteBufUtil.hexDump(navigationBuffer)
                    .contains("746573743a726567697374657265645f746172676574"));
            assertEquals(
                Dialog.ActionType.SHOW_DIALOG,
                DialogCodec.read(navigationBuffer)
                    .getActions()
                    .get(0)
                    .getAction()
                    .getType());
        } finally {
            navigationBuffer.release();
            genericBuffer.release();
        }
    }

    @Test
    void serverLinksRetainTheirTypeAndUrlsForTheClient() {
        Dialog links = Dialog.serverLinks(new ChatComponentText("Server links"))
            .serverLink(new Dialog.ServerLink(new ChatComponentText("Website"), "https://example.com/"))
            .serverLink(new Dialog.ServerLink(new ChatComponentText("Community"), "https://example.org/"))
            .build();
        ByteBuf buffer = Unpooled.buffer();
        try {
            DialogCodec.write(buffer, links);
            Dialog client = DialogCodec.read(buffer);
            assertEquals(Dialog.Type.SERVER_LINKS, client.getType());
            assertEquals(
                2,
                client.getServerLinks()
                    .size());
            assertEquals(
                "https://example.com/",
                client.getServerLinks()
                    .get(0)
                    .getUrl());
            assertEquals(
                "https://example.org/",
                client.getServerLinks()
                    .get(1)
                    .getUrl());
        } finally {
            buffer.release();
        }
    }

    @Test
    void serverLinksRejectNonHttpSchemes() {
        Dialog links = Dialog.serverLinks(new ChatComponentText("Unsafe links"))
            .serverLink(new Dialog.ServerLink(new ChatComponentText("Run"), "javascript:alert(1)"))
            .build();
        ByteBuf buffer = Unpooled.buffer();
        try {
            assertThrows(IllegalArgumentException.class, () -> DialogCodec.write(buffer, links));
        } finally {
            buffer.release();
        }
    }

    @Test
    void serverLinksAndTheirExitActionRoundTrip() {
        Dialog source = Dialog.serverLinks(new ChatComponentText("Links"))
            .columns(3)
            .buttonWidth(230)
            .serverLink(new Dialog.ServerLink(new ChatComponentText("Home"), "https://example.com/"))
            .exitAction(new Dialog.ActionButton(new ChatComponentText("Exit"), Dialog.Action.none()))
            .build();
        ByteBuf buffer = Unpooled.buffer();
        try {
            DialogCodec.write(buffer, source);
            Dialog decoded = DialogCodec.read(buffer);

            assertEquals(Dialog.Type.SERVER_LINKS, decoded.getType());
            assertEquals(3, decoded.getColumns());
            assertEquals(230, decoded.getButtonWidth());
            assertEquals(
                "https://example.com/",
                decoded.getServerLinks()
                    .get(0)
                    .getUrl());
            assertEquals(
                "Exit",
                decoded.getExitAction()
                    .getLabel()
                    .getUnformattedText());
        } finally {
            buffer.release();
        }
    }

    @Test
    void rejectsSuggestedCommandsThatCanInjectAnotherLine() {
        Dialog dialog = Dialog.notice(new ChatComponentText("Suggest"))
            .action(
                new Dialog.ActionButton(
                    new ChatComponentText("Suggest"),
                    Dialog.Action.suggestCommand("/help\n/op Developer")))
            .build();
        ByteBuf buffer = Unpooled.buffer();
        try {
            assertThrows(IllegalArgumentException.class, () -> DialogCodec.write(buffer, dialog));
        } finally {
            buffer.release();
        }
    }

    @Test
    void rejectsUrlsOutsideHttpAndHttps() {
        for (String url : new String[] { "javascript:alert(1)", "file:///etc/passwd", "https:///missing-host" }) {
            Dialog dialog = Dialog.notice(new ChatComponentText("Open"))
                .action(new Dialog.ActionButton(new ChatComponentText("Open"), Dialog.Action.openUrl(url)))
                .build();
            ByteBuf buffer = Unpooled.buffer();
            try {
                assertThrows(IllegalArgumentException.class, () -> DialogCodec.write(buffer, dialog), url);
            } finally {
                buffer.release();
            }
        }
    }

    @Test
    void rejectsFormsWhoseMaximumResponseWouldOverflowItsPacket() {
        Dialog.Builder builder = Dialog.notice(new ChatComponentText("Large form"));
        for (int i = 0; i < 4; i++) {
            builder.input(new Dialog.TextInput("text" + i, new ChatComponentText("Text"), "", 1024, 1));
        }
        Dialog form = builder
            .action(
                new Dialog.ActionButton(
                    new ChatComponentText("Submit"),
                    Dialog.Action.dynamicCustom("test:submit", new net.minecraft.nbt.NBTTagCompound())))
            .build();
        ByteBuf buffer = Unpooled.buffer();
        try {
            assertThrows(IllegalArgumentException.class, () -> DialogCodec.write(buffer, form));
        } finally {
            buffer.release();
        }
    }

    @Test
    void acceptsFormsAndBodiesPastOldLayoutCapsWhenTheyFitThePacket() {
        Dialog.Builder builder = Dialog.multiAction(new ChatComponentText("Large form"));
        String bodyText = new String(new char[257]).replace('\0', 'x');
        for (int i = 0; i < 7; i++) builder.body(new Dialog.TextBody(new ChatComponentText(bodyText)));
        for (int i = 0; i < 5; i++)
            builder.input(new Dialog.TextInput("input" + i, new ChatComponentText("Input"), "", 1, 1));
        for (int i = 0; i < 9; i++) {
            builder.action(
                new Dialog.ActionButton(
                    new ChatComponentText("Action"),
                    Dialog.Action.dynamicCustom("test:submit", new net.minecraft.nbt.NBTTagCompound())));
        }

        ByteBuf buffer = Unpooled.buffer();
        try {
            DialogCodec.write(buffer, builder.build());
            Dialog decoded = DialogCodec.read(buffer);
            assertEquals(
                7,
                decoded.getBody()
                    .size());
            assertEquals(
                5,
                decoded.getInputs()
                    .size());
            assertEquals(
                9,
                decoded.getActions()
                    .size());
        } finally {
            buffer.release();
        }
    }

    @Test
    void allowsStaticCancelAlongsideDynamicFormSubmit() {
        Dialog form = Dialog.multiAction(new ChatComponentText("Form"))
            .input(new Dialog.TextInput("name", new ChatComponentText("Name"), "", 16, 1))
            .action(new Dialog.ActionButton(new ChatComponentText("Cancel"), Dialog.Action.none()))
            .action(
                new Dialog.ActionButton(
                    new ChatComponentText("Submit"),
                    Dialog.Action.dynamicCustom("test:submit", new net.minecraft.nbt.NBTTagCompound())))
            .build();
        ByteBuf buffer = Unpooled.buffer();
        try {
            DialogCodec.write(buffer, form);
            assertEquals(
                form.getActions()
                    .size(),
                DialogCodec.read(buffer)
                    .getActions()
                    .size());
        } finally {
            buffer.release();
        }
    }

    @Test
    void rejectsTruncatedPresentation() {
        ByteBuf buffer = Unpooled.buffer();
        try {
            buffer.writeByte(0);
            assertThrows(IllegalArgumentException.class, () -> DialogCodec.read(buffer));
        } finally {
            buffer.release();
        }
    }

    @Test
    void rejectsOversizedComponentBeforeAllocatingIt() {
        ByteBuf buffer = Unpooled.buffer();
        try {
            buffer.writeByte(1);
            buffer.writeByte(Dialog.Type.NOTICE.ordinal());
            buffer.writeByte(3);
            buffer.writeByte(Dialog.AfterAction.CLOSE.ordinal());
            buffer.writeInt(Integer.MAX_VALUE);
            assertThrows(IllegalArgumentException.class, () -> DialogCodec.read(buffer));
        } finally {
            buffer.release();
        }
    }
}
