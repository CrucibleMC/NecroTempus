package io.github.cruciblemc.necrotempus.modules.mixin.mixins.customnpcs;

import io.github.cruciblemc.necrotempus.api.bossbar.BossBar;
import io.github.cruciblemc.necrotempus.api.bossbar.BossBarColor;
import io.github.cruciblemc.necrotempus.api.bossbar.BossBarType;
import io.github.cruciblemc.necrotempus.modules.features.bossbar.client.ClientBossBarManager;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.boss.IBossDisplayData;
import net.minecraft.util.ChatComponentText;
import noppes.npcs.client.renderer.RenderNPCInterface;
import noppes.npcs.entity.EntityNPCInterface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RenderNPCInterface.class)
public abstract class MixinRenderNPCInterface {

    @Inject(
            method = "doRender(Lnet/minecraft/entity/EntityLiving;DDDFF)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/boss/BossStatus;setBossStatus(Lnet/minecraft/entity/boss/IBossDisplayData;Z)V", shift = At.Shift.BEFORE)
    )
    void doRenderInject(EntityLiving entityliving, double d, double d1, double d2, float f, float f1, CallbackInfo ci) {
        if (entityliving instanceof EntityNPCInterface npc) {

            if ((npc.display.showBossBar == 1 || npc.display.showBossBar == 2 && npc.isAttacking()) && !npc.isKilled() && npc.deathTime <= 20 && npc.canSee(Minecraft.getMinecraft().thePlayer)) {

                BossBar bossBar = BossBar.createBossBar(entityliving.getUniqueID());

                bossBar.setText((ChatComponentText) ((IBossDisplayData) npc).func_145748_c_());
                bossBar.setPercentage(((IBossDisplayData) npc).getHealth() / ((IBossDisplayData) npc).getMaxHealth());
                bossBar.setCreationTime(System.currentTimeMillis());
                bossBar.setType(BossBarType.FLAT);

                if (npc.getFaction() != null) {
                    bossBar.setLazyColor(npc.getFaction().getColor());
                } else {
                    bossBar.setColor(BossBarColor.WHITE);
                }

                ClientBossBarManager.add(bossBar);
            }

        }
    }

    @Redirect(
            method = "doRender(Lnet/minecraft/entity/EntityLiving;DDDFF)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/boss/BossStatus;setBossStatus(Lnet/minecraft/entity/boss/IBossDisplayData;Z)V", remap = false)
    )
    void doRender(IBossDisplayData iBossDisplayData, boolean b) {
    } // do nothing

}
