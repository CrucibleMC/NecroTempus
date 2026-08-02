package io.github.cruciblemc.necrotempus.modules.mixin.mixins.minecraft;

import java.net.HttpURLConnection;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import io.github.cruciblemc.necrotempus.Tags;

@Mixin(targets = "net/minecraft/client/renderer/ThreadDownloadImageData$1")
public class ThreadDownloadImageDataMixin {

    @SuppressWarnings("UnresolvedMixinReference")
    @Inject(
        method = "run()V",
        at = @At(value = "INVOKE", target = "Ljava/net/HttpURLConnection;setDoInput(Z)V"),
        locals = LocalCapture.CAPTURE_FAILSOFT,
        remap = false)
    private void necrotempus$setUserAgent(CallbackInfo ci, HttpURLConnection connection) {
        connection.setRequestProperty(
            "User-Agent",
            "Minecraft/1.7.10 NecroTempus/" + Tags.VERSION + " (+https://github.com/CrucibleMC/NecroTempus)");
    }
}
