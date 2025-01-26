package io.github.cruciblemc.necrotempus.modules.mixin.mixins.minecraft.inv;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiScreenHorseInventory;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiScreenHorseInventory.class)
public abstract class GuiScreenHorseInventoryMixin extends GuiContainer {

    @Shadow
    private IInventory field_147030_v;
    @Shadow
    private IInventory field_147029_w;

    public GuiScreenHorseInventoryMixin(Container p_i1072_1_) {
        super(p_i1072_1_);
    }

    @Inject(method = "Lnet/minecraft/client/gui/inventory/GuiScreenHorseInventory;drawGuiContainerForegroundLayer(II)V", at = @At(value = "HEAD"), cancellable = true)
    public void drawGuiContainerForegroundLayer(int mouseX, int mouseY, CallbackInfo callbackInfo) {
        callbackInfo.cancel();
    }

    @Intrinsic
    public void crucibleTimeMachine$titleRenderHook() {
        this.fontRendererObj.drawString(this.field_147029_w.hasCustomInventoryName() ? this.field_147029_w.getInventoryName() : I18n.format(this.field_147029_w.getInventoryName()), 8, 6, 4210752);
        this.fontRendererObj.drawString(this.field_147030_v.hasCustomInventoryName() ? this.field_147030_v.getInventoryName() : I18n.format(this.field_147030_v.getInventoryName()), 8, this.ySize - 96 + 2, 4210752);
    }

}
