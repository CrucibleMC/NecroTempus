package io.github.cruciblemc.necrotempus.modules.mixin.mixins.botania;

import io.github.cruciblemc.necrotempus.modules.features.bossbar.client.ClientBossBarManager;
import net.minecraft.client.gui.ScaledResolution;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(value = vazkii.botania.client.core.handler.BossBarHandler.class, remap = false)
public class BossBarHandler {

    @ModifyConstant(method = "render(Lnet/minecraft/client/gui/ScaledResolution;)V", constant = @Constant(intValue = 20))
    private static int render(int constant, ScaledResolution scaledResolution) {
        return 20 + Math.min(ClientBossBarManager.size() * 20, scaledResolution.getScaledHeight() / 3);
    }

}
