package io.github.cruciblemc.necrotempus.modules.mixin.mixins.minecraft.inv;

import net.minecraft.client.gui.GuiEnchantment;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.Container;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiEnchantment.class)
public abstract class GuiEnchantmentMixin extends GuiContainer {

    @Shadow
    private String field_147079_H;

    public GuiEnchantmentMixin(Container p_i1072_1_) {
        super(p_i1072_1_);
    }

    @Inject(method = "Lnet/minecraft/client/gui/inventory/GuiEnchantment;drawGuiContainerForegroundLayer(II)V", at = @At(value = "HEAD"), cancellable = true)
    public void drawGuiContainerForegroundLayer(int mouseX, int mouseY, CallbackInfo callbackInfo) {
        callbackInfo.cancel();
    }

    @Intrinsic
    public void crucibleTimeMachine$titleRenderHook() {
        this.fontRendererObj.drawString(this.field_147079_H == null ? I18n.format("container.enchant") : this.field_147079_H, 12, 5, 4210752);
        this.fontRendererObj.drawString(I18n.format("container.inventory"), 8, this.ySize - 96 + 2, 4210752);
    }

}
