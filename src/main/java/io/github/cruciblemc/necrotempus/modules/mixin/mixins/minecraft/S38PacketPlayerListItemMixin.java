package io.github.cruciblemc.necrotempus.modules.mixin.mixins.minecraft;

import java.io.IOException;

import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.S38PacketPlayerListItem;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Raises the vanilla 16-character player-list (tab) name limit to 32.
 * <p>
 * Vanilla {@code readPacketData} reads the name via {@code readStringFromBuffer(16)}, which throws
 * if the received name is longer than 16 chars. We redirect that read to allow up to 32, matching
 * the relaxed server-side cap in the Bukkit {@code CraftPlayer} mixin.
 */
@Mixin(S38PacketPlayerListItem.class)
public class S38PacketPlayerListItemMixin {

    @Redirect(
        method = "Lnet/minecraft/network/play/server/S38PacketPlayerListItem;readPacketData(Lnet/minecraft/network/PacketBuffer;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/PacketBuffer;readStringFromBuffer(I)Ljava/lang/String;"))
    private String necro$extendNameLimit(PacketBuffer buffer, int maxLength) throws IOException {
        return buffer.readStringFromBuffer(32);
    }
}
