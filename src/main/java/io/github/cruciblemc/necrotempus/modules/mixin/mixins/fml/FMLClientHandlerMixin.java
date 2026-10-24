package io.github.cruciblemc.necrotempus.modules.mixin.mixins.fml;

import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ServerListEntryNormal;
import net.minecraft.client.multiplayer.ServerData;

import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.realmsclient.gui.ChatFormatting;

import cpw.mods.fml.client.ExtendedServerListData;
import io.github.cruciblemc.necrotempus.modules.crucible.CrucibleServerListEntry;

@Mixin(value = cpw.mods.fml.client.FMLClientHandler.class, remap = false)
public class FMLClientHandlerMixin {

    @Shadow
    private Minecraft client;

    @Shadow
    private Map<ServerData, ExtendedServerListData> serverDataTag;

    @Unique
    private boolean nt$mc$isBlocked;

    @Unique
    private int nt$crucibleMode;

    @Unique
    private int nt$modCount;

    @Inject(method = "enhanceServerListEntry", at = @At("HEAD"), cancellable = true)
    private void nt$enhanceServerListEntry(ServerListEntryNormal serverListEntry, ServerData serverEntry, int x,
        int width, int y, int relativeMouseX, int relativeMouseY, CallbackInfoReturnable<String> cir) {
        this.nt$mc$isBlocked = false;
        this.nt$crucibleMode = 0;

        if (serverDataTag.containsKey(serverEntry)) {
            ExtendedServerListData extendedData = serverDataTag.get(serverEntry);
            this.nt$mc$isBlocked = extendedData.isBlocked;
            this.nt$modCount = extendedData.modData.size();

            this.nt$crucibleMode = extendedData.modData.containsKey("Crucible")
                ? (extendedData.modData.containsKey("necrotempus") ? 2 : 1)
                : 0;
        }
    }

    @Inject(method = "enhanceServerListEntry", at = @At("RETURN"), cancellable = true)
    private void nt$injectTooltip(ServerListEntryNormal serverListEntry, ServerData serverEntry, int x, int width,
        int y, int relativeMouseX, int relativeMouseY, CallbackInfoReturnable<String> cir) {
        if (this.nt$mc$isBlocked) return;
        if (this.nt$crucibleMode == 0) return;

        String tooltip = cir.getReturnValue();

        if (tooltip == null || tooltip.trim()
            .isEmpty()) return;

        tooltip = String.format(
            (ChatFormatting.DARK_GRAY + "Compatible %s "
                + ChatFormatting.DARK_GRAY
                + "modded server\n"
                + ChatFormatting.DARK_GRAY
                + ChatFormatting.BOLD
                + "%d"
                + ChatFormatting.DARK_GRAY
                + " Mods are present"),
            (String.valueOf(this.nt$crucibleMode == 1 ? ChatFormatting.GREEN : ChatFormatting.RED))
                + ChatFormatting.BOLD
                + "Crucible"
                + ChatFormatting.RESET,
            this.nt$modCount);

        cir.setReturnValue(tooltip);
    }

    @WrapOperation(
        method = "enhanceServerListEntry",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;func_146110_a(IIFFIIFF)V"))
    private void nt$wrapIcon(int x, int y, float u, float v, int width, int height, float textureWidth,
        float textureHeight, Operation<Void> original) {
        if (this.nt$mc$isBlocked || this.nt$crucibleMode == 0) {
            original.call(x, y, u, v, width, height, textureWidth, textureHeight);
            return;
        }
        this.client.getTextureManager()
            .bindTexture(CrucibleServerListEntry.CRUCIBLE_ICONS);

        Gui.func_146110_a(x, y, 0.0F, (this.nt$crucibleMode - 1) * 16, width, height, 256.0F, 256.0F);
    }

}
