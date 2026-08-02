package io.github.cruciblemc.necrotempus.modules.mixin.mixins.minecraft;

import net.minecraft.client.renderer.EntityRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.cruciblemc.necrotempus.NecroTempusConfig;
import io.github.cruciblemc.necrotempus.modules.features.glow.render.GlowRenderCore;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {

    // Injected at the unconditional "hand" profiler section (right after ForgeHooksClient.dispatchRenderLast),
    // where the world camera projection/modelview are still active — before the depth-buffer clear and the
    // CONDITIONALLY-called renderHand that would otherwise overwrite them with first-person-hand transforms.
    @Inject(
        method = "renderWorld(FJ)V",
        at = @At(
            value = "INVOKE_STRING",
            target = "Lnet/minecraft/profiler/Profiler;endStartSection(Ljava/lang/String;)V",
            args = "ldc=hand"))
    private void nt$renderEntityGlow(float partialTicks, long nanoTime, CallbackInfo ci) {
        if (!NecroTempusConfig.enableEntityGlow) return;
        GlowRenderCore.INSTANCE.renderAndComposite(partialTicks);
    }
}
