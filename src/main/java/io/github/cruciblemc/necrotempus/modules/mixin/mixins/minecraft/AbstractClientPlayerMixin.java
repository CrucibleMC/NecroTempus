package io.github.cruciblemc.necrotempus.modules.mixin.mixins.minecraft;

import net.minecraft.client.entity.AbstractClientPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(AbstractClientPlayer.class)
public class AbstractClientPlayerMixin {

    @ModifyConstant(
        method = "getDownloadImageSkin",
        constant = @Constant(stringValue = "http://skins.minecraft.net/MinecraftSkins/%s.png"),
        require = 0)
    private static String necrotempus$redirectSkinUrl(String url) {
        return "https://visage.surgeplay.com/skin/%s.png";
    }
}
