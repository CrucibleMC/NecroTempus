package io.github.cruciblemc.necrotempus.modules.mixin.mixins.bukkit.inventory;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(targets = "org.bukkit.craftbukkit.v1_7_R4.inventory.CraftInventoryCustom$MinecraftInventory", remap = false)
public abstract class CraftInventoryCustom_MixinMinecraftInventory {

//    @Redirect(
//        method = {
//            "<init>(Lorg/bukkit/inventory/InventoryHolder;I;Ljava/lang/String;)V",
//            "<init>(Lorg/bukkit/inventory/InventoryHolder;Lorg/bukkit/event/inventory/InventoryType;Ljava/lang/String;)V"
//        },
//        at = @At(
//            value = "INVOKE",
//            target = "Lorg/apache/commons/lang/Validate;isTrue(ZLjava/lang/String;)V"
//        )
//    )
//    private void redirectValidate(boolean expression, String message) {
//        // Do nothing - this prevents the original validation from executing
//    }

}
