package io.github.cruciblemc.necrotempus.modules.mixin.mixins.minecraft;

import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.entity.EntityLivingBase;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.github.cruciblemc.necrotempus.modules.features.glow.render.GlowRenderCore;
import io.github.cruciblemc.necrotempus.modules.features.glow.render.GlowRenderPolicy;

@Mixin(RendererLivingEntity.class)
public abstract class RendererLivingEntityMixin {

    /**
     * Suppress nametag rendering while the glow silhouette pass is active, so
     * nameplate text does not get captured in the glow mask framebuffer.
     */
    @Inject(method = "func_110813_b(Lnet/minecraft/entity/EntityLivingBase;)Z", at = @At("HEAD"), cancellable = true)
    private void nt$suppressNameplate(EntityLivingBase entity, CallbackInfoReturnable<Boolean> ci) {
        if (GlowRenderCore.silhouettePassActive) {
            ci.setReturnValue(false);
        }
    }

    /**
     * Vanilla skips the model for fully invisible entities. During the glow pass the model itself
     * must be captured into the silhouette framebuffer, while the regular scene keeps vanilla's
     * invisible/translucent behavior.
     */
    @Redirect(
        method = "renderModel(Lnet/minecraft/entity/EntityLivingBase;FFFFFF)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityLivingBase;isInvisible()Z"))
    private boolean nt$renderInvisibleModel(EntityLivingBase entity) {
        return !GlowRenderPolicy.shouldRenderBaseModel(entity.isInvisible(), GlowRenderCore.silhouettePassActive);
    }
}
