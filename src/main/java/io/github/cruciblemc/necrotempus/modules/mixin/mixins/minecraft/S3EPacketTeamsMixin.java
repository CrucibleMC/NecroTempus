package io.github.cruciblemc.necrotempus.modules.mixin.mixins.minecraft;

import java.io.IOException;

import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.S3EPacketTeams;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Raises the vanilla 16-character scoreboard team prefix/suffix limit to 32.
 * <p>
 * {@code readPacketData} reads the team name, prefix and suffix via {@code readStringFromBuffer(16)},
 * which throws if the received string is longer than 16. We redirect every such read to a minimum
 * limit of 32, matching the relaxed server-side cap in the Bukkit {@code CraftTeam} mixin. Reads that
 * already allow more (display name 32, member names 40) are left untouched by the {@code Math.max}.
 */
@Mixin(S3EPacketTeams.class)
public class S3EPacketTeamsMixin {

    @Redirect(
        method = "Lnet/minecraft/network/play/server/S3EPacketTeams;readPacketData(Lnet/minecraft/network/PacketBuffer;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/PacketBuffer;readStringFromBuffer(I)Ljava/lang/String;"))
    private String necro$extendTeamStringLimit(PacketBuffer buffer, int maxLength) throws IOException {
        return buffer.readStringFromBuffer(Math.max(maxLength, 32));
    }
}
