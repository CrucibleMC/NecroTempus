package io.github.cruciblemc.necrotempus.modules.mixin.plugin;

import cpw.mods.fml.relauncher.FMLLaunchHandler;
import io.github.cruciblemc.necrotempus.NecroTempusConfig;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public enum Mixin {

    //
    // IMPORTANT: Do not make any references to any mod from this file. This file is loaded quite early on and if
    // you refer to other mods you load them as well. The consequence is: You can't inject any previously loaded classes!
    // Exception: Tags.java, as long as it is used for Strings only!
    //

    // Replace with your own mixins:
    FMLClientHandler("fml.FMLClientHandler", Side.CLIENT, TargetedMod.VANILLA),
    BotaniaBossBarHandler("botania.BossBarHandler", Side.CLIENT, TargetedMod.BOTANIA),
    CustomNpcsPermissions("customnpcs.CustomNpcsPermissionsMixin", Side.CLIENT, TargetedMod.CUSTOM_NPCS),
    CustomNpcsMixinRenderNPCInterface("customnpcs.MixinRenderNPCInterface", Side.CLIENT, TargetedMod.CUSTOM_NPCS),
    FontRenderMixin("minecraft.FontRendererMixin", Side.CLIENT, TargetedMod.VANILLA),
    FontRender2Mixin("minecraft.FontRenderer2Mixin", Side.CLIENT, TargetedMod.VANILLA),
    GuiContainerMixin("minecraft.GuiContainerMixin", Side.CLIENT, TargetedMod.VANILLA),
    GuiIngame("minecraft.GuiIngameMixin", Side.CLIENT, TargetedMod.VANILLA),
    GuiChestMixin("minecraft.inv.GuiChestMixin", Side.CLIENT, TargetedMod.VANILLA),
    GuiBrewingStandMixin("minecraft.inv.GuiBrewingStandMixin", Side.CLIENT, TargetedMod.VANILLA),
    GuiDispenserMixin("minecraft.inv.GuiDispenserMixin", Side.CLIENT, TargetedMod.VANILLA),
    GuiFurnaceMixin("minecraft.inv.GuiFurnaceMixin", Side.CLIENT, TargetedMod.VANILLA),
    GuiScreenHorseInventoryMixin("minecraft.inv.GuiScreenHorseInventoryMixin", Side.CLIENT, TargetedMod.VANILLA),
    GuiEnchantmentMixin("minecraft.inv.GuiEnchantmentMixin", Side.CLIENT, TargetedMod.VANILLA),
    GuiHopperMixin("minecraft.inv.GuiHopperMixin", Side.CLIENT, TargetedMod.VANILLA),
    GuiMerchantMixin("minecraft.inv.GuiMerchantMixin", Side.CLIENT, TargetedMod.VANILLA);


    public final String mixinClass;
    public final HashSet<TargetedMod> targetedMods;
    private final Side side;

    Mixin(String mixinClass, Side side, TargetedMod... targetedMods) {
        this.mixinClass = mixinClass;
        this.targetedMods = new HashSet<>(Arrays.asList(targetedMods));
        this.side = side;
    }


    public boolean shouldLoad(List<TargetedMod> loadedMods) {

        if (this == FontRenderMixin && !NecroTempusConfig.HexColorsEnabled)
            return false;

        if (this == GuiIngame && !NecroTempusConfig.ScoreBoardEnabled) {
            return false;
        }

        return (side == Side.BOTH
                || side == Side.SERVER && FMLLaunchHandler.side().isServer()
                || side == Side.CLIENT && FMLLaunchHandler.side().isClient())
                && new HashSet<>(loadedMods).containsAll(targetedMods);
    }
}

enum Side {
    BOTH,
    CLIENT,
    SERVER
}
