package io.github.cruciblemc.necrotempus.modules.mixin.mixins.minecraft.inv;

import net.minecraft.client.gui.inventory.GuiBrewingStand;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.Container;
import net.minecraft.tileentity.TileEntityBrewingStand;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiBrewingStand.class)
public abstract class GuiBrewingStandMixin extends GuiContainer {

    @Shadow
    private TileEntityBrewingStand tileBrewingStand;

    public GuiBrewingStandMixin(Container p_i1072_1_) {
        super(p_i1072_1_);
    }

    @Inject(method = "Lnet/minecraft/client/gui/inventory/GuiBrewingStand;drawGuiContainerForegroundLayer(II)V", at = @At(value = "HEAD"), cancellable = true)
    public void drawGuiContainerForegroundLayer(int mouseX, int mouseY, CallbackInfo callbackInfo) {
        callbackInfo.cancel();
    }

    @Intrinsic
    public void crucibleTimeMachine$titleRenderHook() {
        String s = this.tileBrewingStand.hasCustomInventoryName() ? this.tileBrewingStand.getInventoryName() : I18n.format(this.tileBrewingStand.getInventoryName());
        this.fontRendererObj.drawString(s, this.xSize / 2 - this.fontRendererObj.getStringWidth(s) / 2, 6, 4210752);
        this.fontRendererObj.drawString(I18n.format("container.inventory"), 8, this.ySize - 96 + 2, 4210752);

    }

}
