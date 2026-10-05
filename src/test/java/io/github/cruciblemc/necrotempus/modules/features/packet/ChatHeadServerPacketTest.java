package io.github.cruciblemc.necrotempus.modules.features.packet;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.network.play.server.S02PacketChat;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

class ChatHeadServerPacketTest {

    @Test
    void serverChatHookDoesNotCallClientOnlyPacketMethods() throws IOException {
        Set<String> clientOnlyMethods = new HashSet<>();
        for (Method method : S02PacketChat.class.getDeclaredMethods()) {
            SideOnly side = method.getAnnotation(SideOnly.class);
            if (side != null && side.value() == Side.CLIENT)
                clientOnlyMethods.add(method.getName() + Type.getMethodDescriptor(method));
        }
        assertFalse(clientOnlyMethods.isEmpty());

        ClassNode hook = new ClassNode();
        new ClassReader("io.github.cruciblemc.necrotempus.modules.mixin.mixins.crucible.NetHandlerPlayServerMixin")
            .accept(hook, 0);
        for (MethodNode method : hook.methods) {
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (!(instruction instanceof MethodInsnNode)) continue;
                MethodInsnNode call = (MethodInsnNode) instruction;
                if (call.owner.equals(Type.getInternalName(S02PacketChat.class)))
                    assertFalse(clientOnlyMethods.contains(call.name + call.desc), call.name + " is client-only");
            }
        }
    }
}
