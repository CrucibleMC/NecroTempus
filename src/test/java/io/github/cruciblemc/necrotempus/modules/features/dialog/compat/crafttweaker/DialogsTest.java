package io.github.cruciblemc.necrotempus.modules.features.dialog.compat.crafttweaker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.UUID;

import net.minecraft.util.ChatComponentText;

import org.junit.jupiter.api.Test;

import io.github.cruciblemc.necrotempus.api.dialog.Dialog;
import io.github.cruciblemc.necrotempus.api.dialog.DialogManager;

class DialogsTest {

    @Test
    void exposesTextInputRegistrationToZenScript() {
        assertTrue(hasZenMethod("registerItemNotice"));
        assertTrue(hasZenMethod("registerTextInputNotice"));
        assertTrue(hasZenMethod("registerMultilineTextInputNotice"));
        assertTrue(hasZenMethod("registerDialogList"));
        assertTrue(hasZenMethod("registerCustomNotice"));
        assertTrue(hasZenMethod("registerNavigateNotice"));
        assertTrue(hasZenMethod("registerSuggestNotice"));
        assertTrue(hasZenMethod("registerClipboardNotice"));
        assertTrue(hasZenMethod("registerUrlNotice"));
        assertTrue(hasZenMethod("registerWaitingNotice"));
        assertTrue(hasZenMethod("registerRepeatableNotice"));
        assertTrue(hasZenMethod("registerDynamicCommandNotice"));
        assertTrue(hasZenMethod("registerDynamicCommandNotice", 10));
        assertTrue(hasZenMethod("registerServerLinks"));
        assertTrue(hasZenMethod("registerLinkedNotice"));
        assertTrue(hasZenMethod("registerDialogTag"));
        assertTrue(hasZenMethod("registerTaggedDialogList"));
    }

    private static boolean hasZenMethod(String name) {
        for (Method method : Dialogs.class.getMethods()) {
            if (!name.equals(method.getName())) continue;
            for (Annotation annotation : method.getAnnotations()) {
                if ("stanhebben.zenscript.annotations.ZenMethod".equals(
                    annotation.annotationType()
                        .getName())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasZenMethod(String name, int parameterCount) {
        for (Method method : Dialogs.class.getMethods()) {
            if (!name.equals(method.getName()) || method.getParameterCount() != parameterCount) continue;
            for (Annotation annotation : method.getAnnotations()) {
                if ("stanhebben.zenscript.annotations.ZenMethod".equals(
                    annotation.annotationType()
                        .getName())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Test
    void scriptClassIsZenRegisteredAndItsActionRestoresPreviousDefinitions() throws Exception {
        Annotation zenClass = null;
        for (Annotation annotation : Dialogs.class.getAnnotations()) {
            if ("stanhebben.zenscript.annotations.ZenClass".equals(
                annotation.annotationType()
                    .getName())) {
                zenClass = annotation;
                break;
            }
        }
        assertEquals(
            "necrotempus.crafttweaker.Dialogs",
            zenClass.annotationType()
                .getMethod("value")
                .invoke(zenClass));
        String id = "test:" + UUID.randomUUID()
            .toString()
            .replace("-", "");
        Dialog previous = notice("Before");
        Dialog replacement = notice("After");
        Class<?> actionClass = Class.forName(Dialogs.class.getName() + "$RegisterAction");
        Constructor<?> constructor = actionClass.getConstructor(String.class, Dialog.class);
        Object registerPrevious = constructor.newInstance(id, previous);
        Object registerReplacement = constructor.newInstance(id, replacement);
        Method apply = actionClass.getMethod("apply");
        Method undo = actionClass.getMethod("undo");

        apply.invoke(registerPrevious);
        apply.invoke(registerReplacement);
        assertSame(replacement, DialogManager.getRegistered(id));
        undo.invoke(registerReplacement);
        assertSame(previous, DialogManager.getRegistered(id));
        undo.invoke(registerPrevious);
        assertNull(DialogManager.getRegistered(id));
    }

    @Test
    void registrationActionAppliesAndRestoresCommandExecutionPolicy() throws Exception {
        String id = "test:" + UUID.randomUUID()
            .toString()
            .replace("-", "");
        Class<?> actionClass = Class.forName(Dialogs.class.getName() + "$RegisterAction");
        Constructor<?> constructor = actionClass
            .getConstructor(String.class, Dialog.class, Dialog.CommandExecutionPolicy.class);
        Object action = constructor.newInstance(id, notice("Command"), Dialog.CommandExecutionPolicy.NO_CONFIRMATION);

        actionClass.getMethod("apply")
            .invoke(action);
        assertEquals(
            Dialog.CommandExecutionPolicy.NO_CONFIRMATION,
            DialogManager.getRegistered(id)
                .getCommandExecutionPolicy());
        actionClass.getMethod("undo")
            .invoke(action);
        assertNull(DialogManager.getRegistered(id));
    }

    private static Dialog notice(String text) {
        return Dialog.notice(new ChatComponentText(text))
            .action(new Dialog.ActionButton(new ChatComponentText("Close"), Dialog.Action.none()))
            .build();
    }
}
