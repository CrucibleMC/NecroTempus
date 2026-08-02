package io.github.cruciblemc.necrotempus.modules.mixin.mixins.minecraft;

import net.minecraft.client.renderer.entity.RenderHorse;
import net.minecraft.entity.passive.EntityHorse;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import io.github.cruciblemc.necrotempus.modules.features.glow.render.GlowRenderCore;
import io.github.cruciblemc.necrotempus.modules.features.glow.render.GlowRenderPolicy;

/** Applies the silhouette-pass invisibility policy to RenderHorse's override. */
@Mixin(RenderHorse.class)
public abstract class RenderHorseMixin {

    @Redirect(
        method = "renderModel(Lnet/minecraft/entity/passive/EntityHorse;FFFFFF)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/passive/EntityHorse;isInvisible()Z"))
    private boolean nt$renderInvisibleHorseModel(EntityHorse entity) {
        return !GlowRenderPolicy.shouldRenderBaseModel(entity.isInvisible(), GlowRenderCore.silhouettePassActive);
    }
}
