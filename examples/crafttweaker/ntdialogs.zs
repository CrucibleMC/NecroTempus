import necrotempus.crafttweaker.Dialogs;

Dialogs.registerNotice(
    "necrotempus:integration_notice",
    "Dialog transport test",
    "This dialog was registered by a CraftTweaker script.",
    "Close");

Dialogs.registerItemNotice(
    "necrotempus:integration_item",
    "Item body test",
    "minecraft:apple",
    0,
    3,
    "The icon, count, tooltip, and description come from an ItemBody.",
    "Close");

Dialogs.registerLinkedNotice(
    "necrotempus:integration_body_link",
    "Body click event test",
    "Click ",
    "Open example",
    "https://example.com/",
    " to test a body click event.",
    "Close");

Dialogs.registerCustomNotice(
    "necrotempus:integration_custom",
    "Custom action test",
    "The server callback receives this action ID.",
    "necrotempus:ct_custom",
    "Send action");

Dialogs.registerSuggestNotice(
    "necrotempus:integration_suggest",
    "Suggest command test",
    "The command is placed in chat but is not executed.",
    "/help",
    "Fill chat");

Dialogs.registerClipboardNotice(
    "necrotempus:integration_clipboard",
    "Clipboard test",
    "Copy a fixed value from this server-defined action.",
    "Copied from a NecroTempus dialog",
    "Copy text");

Dialogs.registerUrlNotice(
    "necrotempus:integration_url",
    "Open URL test",
    "The legacy client asks before opening this HTTP link.",
    "https://example.com/",
    "Review link");

Dialogs.registerWaitingNotice(
    "necrotempus:integration_wait",
    "Wait for response test",
    "The return button unlocks after five seconds; Escape unlocks after five seconds.",
    "Submit and wait");

Dialogs.registerRepeatableNotice(
    "necrotempus:integration_repeatable",
    "Finite action test",
    "This action can be used twice without closing the dialog.",
    "Use once",
    2);

Dialogs.registerDynamicCommandNotice(
    "necrotempus:integration_command",
    "Dynamic command test",
    "Compatibility policy confirms the command; the server runs it as the player.",
    "message",
    "Message",
    "Hello from dialogs",
    80,
    "say $(message)",
    // Use NO_CONFIRMATION only when the dialog's command is safe to run directly.
    "COMPATIBILITY_CONFIRM",
    "Confirm and run");

Dialogs.registerNavigateNotice(
    "necrotempus:integration_navigate",
    "Dialog navigation test",
    "This action opens the registered wait dialog on the server.",
    "necrotempus:integration_wait",
    "Open wait dialog");

Dialogs.registerServerLinks(
    "necrotempus:integration_links",
    "Server links test",
    "Each link asks for confirmation before opening the browser.",
    ["Website", "Community"] as string[],
    ["https://example.com/", "https://example.org/"] as string[]);

Dialogs.registerConfirmation(
    "necrotempus:integration_confirmation",
    "Dialog callback test",
    "Choose an answer; the server reports the selected action index.",
    "Confirm",
    "Decline");

Dialogs.registerMultiAction(
    "necrotempus:integration_multi",
    "Dialog layout test",
    "Five actions exercise the two-column layout.",
    ["One", "Two", "Three", "Four", "Five"] as string[]);

Dialogs.registerTextInputNotice(
    "necrotempus:integration_form",
    "Dialog input test",
    "Enter a value and submit it; the server validates and reports the response.",
    "name",
    "Name",
    "Alex",
    40,
    "Submit");

Dialogs.registerMultilineTextInputNotice(
    "necrotempus:integration_multiline",
    "Multiline input test",
    "Enter a short message on up to four lines.",
    "message",
    "Message",
    "First line",
    128,
    4,
    "Send message");

Dialogs.registerDialogList(
    "necrotempus:integration_menu",
    "Integration menu",
    "Choose a dialog",
    "Targets are resolved and opened by the server.",
    ["Notice", "Form", "Multiline", "Wait", "Repeatable", "Command", "Navigate", "Links"] as string[],
    [
        "necrotempus:integration_notice",
        "necrotempus:integration_form",
        "necrotempus:integration_multiline",
        "necrotempus:integration_wait",
        "necrotempus:integration_repeatable",
        "necrotempus:integration_command",
        "necrotempus:integration_navigate",
        "necrotempus:integration_links"] as string[]);

Dialogs.registerDialogTag(
    "necrotempus:integration_dialog_targets",
    ["necrotempus:integration_notice", "necrotempus:integration_form"] as string[]);

Dialogs.registerTaggedDialogList(
    "necrotempus:integration_tagged_list",
    "Dialogs from a tag",
    "Tagged dialogs",
    "This list expands a server-registered tag.",
    ["necrotempus:integration_dialog_targets"] as string[]);

Dialogs.registerDialogTag(
    "minecraft:pause_screen_additions",
    ["necrotempus:integration_tagged_list"] as string[]);

Dialogs.registerDialogTag(
    "minecraft:quick_actions",
    ["necrotempus:integration_menu", "necrotempus:integration_item"] as string[]);
