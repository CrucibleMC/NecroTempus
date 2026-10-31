package io.github.cruciblemc.necrotempus.modules.features.dialog.server;

import java.util.Arrays;
import java.util.Collections;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;

import io.github.cruciblemc.necrotempus.api.dialog.Dialog;
import io.github.cruciblemc.necrotempus.api.dialog.DialogManager;
import io.github.cruciblemc.necrotempus.api.dialog.DialogResponse;

public final class DialogTestCommand extends CommandBase {

    @Override
    public String getCommandName() {
        return "ntdialogs";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/ntdialogs test [notice|confirmation|multi|form|custom|menu] | show <id>";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] arguments) throws CommandException {
        if (!(sender instanceof EntityPlayerMP)) {
            sender.addChatMessage(new ChatComponentText("Run this command as a player."));
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) sender;
        if (arguments.length == 2 && "show".equalsIgnoreCase(arguments[0])) {
            DialogManager.ShowResult result = DialogManager.show(
                player,
                arguments[1],
                (target, response) -> target
                    .addChatMessage(new ChatComponentText(formatResponse(arguments[1], response))));
            if (result != DialogManager.ShowResult.SHOWN) {
                sender.addChatMessage(new ChatComponentText("Dialog could not open: " + result.name()));
            }
            return;
        }
        if ((arguments.length == 1 || arguments.length == 2) && "test".equalsIgnoreCase(arguments[0])) {
            String type = arguments.length == 1 ? "confirmation" : arguments[1];
            if ("menu".equalsIgnoreCase(type)) {
                String menuId = "necrotempus:integration_menu";
                String itemId = "necrotempus:integration_item";
                if (DialogManager.getRegistered(menuId) == null || DialogManager.getRegistered(itemId) == null) {
                    sender.addChatMessage(new ChatComponentText("Load examples/crafttweaker/ntdialogs.zs first."));
                    return;
                }
                try {
                    DialogManager.setMenuTags(
                        player,
                        Collections.singletonList("minecraft:pause_screen_additions"),
                        Collections.singletonList("minecraft:quick_actions"));
                } catch (IllegalArgumentException exception) {
                    sender.addChatMessage(new ChatComponentText("Load examples/crafttweaker/ntdialogs.zs first."));
                    return;
                }
                DialogManager.setServerLinks(
                    player,
                    Arrays.asList(
                        new Dialog.ServerLink(new ChatComponentText("Example link"), "https://example.com/one"),
                        new Dialog.ServerLink(new ChatComponentText("Example link"), "https://example.com/two")));
                sender
                    .addChatMessage(new ChatComponentText("Dialog pause menu, G shortcut, and server links enabled."));
                return;
            }
            Dialog dialog;
            try {
                dialog = createTestDialog(type);
            } catch (IllegalArgumentException exception) {
                throw new CommandException(getCommandUsage(sender));
            }
            show(player, dialog, "Test " + type);
            return;
        }
        throw new CommandException(getCommandUsage(sender));
    }

    static Dialog createTestDialog(String type) {
        if ("notice".equalsIgnoreCase(type)) {
            return Dialog.notice(new ChatComponentText("Dialog notice test"))
                .body(new Dialog.TextBody(new ChatComponentText("This notice uses the Forge dialog channel.")))
                .action(new Dialog.ActionButton(new ChatComponentText("Close"), Dialog.Action.none()))
                .build();
        }
        if ("confirmation".equalsIgnoreCase(type)) {
            return Dialog.confirmation(new ChatComponentText("NecroTempus dialogs"))
                .body(new Dialog.TextBody(new ChatComponentText("Choose a response to test the callback.")))
                .action(new Dialog.ActionButton(new ChatComponentText("Confirm"), Dialog.Action.none()))
                .action(new Dialog.ActionButton(new ChatComponentText("Decline"), Dialog.Action.none()))
                .build();
        }
        if ("multi".equalsIgnoreCase(type)) {
            Dialog.Builder builder = Dialog.multiAction(new ChatComponentText("Dialog layout test"))
                .body(new Dialog.TextBody(new ChatComponentText("Five actions exercise the two-column layout.")));
            for (int i = 1; i <= 5; i++) {
                builder.action(new Dialog.ActionButton(new ChatComponentText("Choice " + i), Dialog.Action.none()));
            }
            return builder.build();
        }
        if ("form".equalsIgnoreCase(type)) {
            return Dialog.notice(new ChatComponentText("Dialog input test"))
                .body(new Dialog.TextBody(new ChatComponentText("Submit each control to return validated values.")))
                .input(new Dialog.TextInput("name", new ChatComponentText("Name"), "Alex", 40, 1))
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
                            new Dialog.Option("builder", new ChatComponentText("Builder")),
                            new Dialog.Option("tester", new ChatComponentText("Tester"))),
                        "builder"))
                .input(new Dialog.NumberRangeInput("amount", new ChatComponentText("Amount"), 0, 10, 2.0f, 2))
                .action(
                    new Dialog.ActionButton(
                        new ChatComponentText("Submit"),
                        Dialog.Action.dynamicCustom("necrotempus:test_form_submit", new NBTTagCompound())))
                .build();
        }
        if ("custom".equalsIgnoreCase(type)) {
            return Dialog.notice(new ChatComponentText("Custom action test"))
                .body(new Dialog.TextBody(new ChatComponentText("The server receives the selected action ID.")))
                .action(
                    new Dialog.ActionButton(
                        new ChatComponentText("Run"),
                        Dialog.Action.custom("necrotempus:test_custom", null)))
                .build();
        }
        throw new IllegalArgumentException("Unknown dialog test: " + type);
    }

    static String formatResponse(String id, DialogResponse response) {
        String result = "Dialog " + id
            + " returned action "
            + response.getActionIndex()
            + (response.isCancelled() ? " (cancelled)" : "");
        if (response.getActionId() != null) {
            result += " custom " + response.getActionId();
            if (response.getActionPayload() != null) result += " payload " + response.getActionPayload();
        }
        if (!response.getInputValues()
            .isEmpty()) result += " with inputs " + response.getInputValues();
        return result + ".";
    }

    private static void show(EntityPlayerMP player, Dialog dialog, String label) {
        DialogManager.ShowResult result = DialogManager.show(
            player,
            dialog,
            (target, response) -> target.addChatMessage(
                new ChatComponentText(
                    response.isCancelled() ? label + " cancelled."
                        : label + " selected action " + response.getActionIndex() + ".")));
        if (result != DialogManager.ShowResult.SHOWN) {
            player.addChatMessage(new ChatComponentText("Dialog could not open: " + result.name()));
        }
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }
}
