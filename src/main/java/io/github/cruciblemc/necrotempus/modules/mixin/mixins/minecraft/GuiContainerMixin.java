package io.github.cruciblemc.necrotempus.modules.mixin.mixins.minecraft;

import net.minecraft.client.gui.inventory.GuiContainer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiContainer.class)
public class GuiContainerMixin {

    @Shadow
    protected int guiLeft;

    @Shadow
    protected int guiTop;

    @Inject(method = "Lnet/minecraft/client/gui/inventory/GuiContainer;drawScreen(IIF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/inventory/GuiContainer;drawGuiContainerBackgroundLayer(FII)V", shift = At.Shift.AFTER))
    public void drawScreen(CallbackInfo callbackInfo) {

        GL11.glPushMatrix();

        GL11.glTranslatef((float) guiLeft, (float) guiTop, 0.0F);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);

        GL11.glDisable(GL11.GL_LIGHTING);
        crucibleTimeMachine$titleRenderHook();
        GL11.glEnable(GL11.GL_LIGHTING);

        GL11.glPopMatrix();

    }

    @Unique
    public void crucibleTimeMachine$titleRenderHook() {
    }

}
