package io.github.cruciblemc.necrotempus.modules.mixin.mixins.customnpcs;

import noppes.npcs.CustomNpcsPermissions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = CustomNpcsPermissions.class, remap = false)
public abstract class CustomNpcsPermissionsMixin {

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Ljava/lang/Class;forName(Ljava/lang/String;)Ljava/lang/Class;", remap = false), remap = false)
    private Class<?> init(String className) throws ClassNotFoundException {
        throw new ClassNotFoundException();
    }

}
