package io.github.cruciblemc.necrotempus.modules.mixin.mixins.minecraft.inv;

import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiChest.class)
public abstract class GuiChestMixin extends GuiContainer {

    @Shadow
    private IInventory upperChestInventory;

    @Shadow
    private IInventory lowerChestInventory;

    public GuiChestMixin(Container p_i1072_1_) {
        super(p_i1072_1_);
    }

    @Inject(method = "Lnet/minecraft/client/gui/inventory/GuiChest;drawGuiContainerForegroundLayer(II)V", at = @At(value = "HEAD"), cancellable = true)
    public void drawGuiContainerForegroundLayer(int mouseX, int mouseY, CallbackInfo callbackInfo) {
        callbackInfo.cancel();
    }

    @Intrinsic
    public void crucibleTimeMachine$titleRenderHook() {
        this.fontRendererObj.drawString(this.lowerChestInventory.hasCustomInventoryName() ? this.lowerChestInventory.getInventoryName() : I18n.format(this.lowerChestInventory.getInventoryName()), 8, 6, 4210752);
        this.fontRendererObj.drawString(this.upperChestInventory.hasCustomInventoryName() ? this.upperChestInventory.getInventoryName() : I18n.format(this.upperChestInventory.getInventoryName()), 8, this.ySize - 96 + 2, 4210752);

    }

}
