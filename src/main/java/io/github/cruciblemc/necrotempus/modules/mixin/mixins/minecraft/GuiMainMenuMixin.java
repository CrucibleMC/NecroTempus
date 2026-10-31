package io.github.cruciblemc.necrotempus.modules.mixin.mixins.minecraft;

import net.minecraft.client.gui.GuiMainMenu;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.cruciblemc.necrotempus.modules.features.dialog.client.GuiMainMenuAccess;
import io.github.cruciblemc.necrotempus.modules.features.dialog.client.PreJoinPanorama;

@Mixin(GuiMainMenu.class)
public abstract class GuiMainMenuMixin implements GuiMainMenuAccess {

    @Invoker("renderSkybox")
    public abstract void necrotempus$renderSkybox(int mouseX, int mouseY, float partialTicks);

    @Inject(method = "initGui", at = @At("TAIL"))
    private void necrotempus$setActiveMenu(CallbackInfo ci) {
        PreJoinPanorama.setActiveMenu((GuiMainMenu) (Object) this);
    }
}
