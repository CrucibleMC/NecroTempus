package io.github.cruciblemc.necrotempus.modules.mixin.plugin;

import com.gtnewhorizon.gtnhmixins.builders.IMixins;
import com.gtnewhorizon.gtnhmixins.builders.MixinBuilder;

import io.github.cruciblemc.necrotempus.NecroTempusConfig;

public enum VanillaMixins implements IMixins {

    FML_CLIENT_HANDLER((new MixinBuilder()).addClientMixins(new String[] { "fml.FMLClientHandler" })),

    FONT_RENDERER((new MixinBuilder()).setApplyIf(() -> NecroTempusConfig.HexColorsEnabled)
        .addClientMixins(new String[] { "minecraft.FontRendererMixin" })),

    FONT_RENDERER_2((new MixinBuilder()).addClientMixins(new String[] { "minecraft.FontRenderer2Mixin" })),

    GUI_CONTAINER((new MixinBuilder()).addClientMixins(new String[] { "minecraft.GuiContainerMixin" })),

    GUI_INGAME((new MixinBuilder()).setApplyIf(() -> NecroTempusConfig.ScoreBoardEnabled)
        .addClientMixins(new String[] { "minecraft.GuiIngameMixin" })),

    GUI_CHEST((new MixinBuilder()).addClientMixins(new String[] { "minecraft.inv.GuiChestMixin" })),

    GUI_BREWING_STAND((new MixinBuilder()).addClientMixins(new String[] { "minecraft.inv.GuiBrewingStandMixin" })),

    GUI_DISPENSER((new MixinBuilder()).addClientMixins(new String[] { "minecraft.inv.GuiDispenserMixin" })),

    GUI_FURNACE((new MixinBuilder()).addClientMixins(new String[] { "minecraft.inv.GuiFurnaceMixin" })),

    GUI_HORSE_INVENTORY(
        (new MixinBuilder()).addClientMixins(new String[] { "minecraft.inv.GuiScreenHorseInventoryMixin" })),

    GUI_ENCHANTMENT((new MixinBuilder()).addClientMixins(new String[] { "minecraft.inv.GuiEnchantmentMixin" })),

    GUI_HOPPER((new MixinBuilder()).addClientMixins(new String[] { "minecraft.inv.GuiHopperMixin" })),

    GUI_MERCHANT((new MixinBuilder()).addClientMixins(new String[] { "minecraft.inv.GuiMerchantMixin" })),

    BOTANIA_BOSS_BAR((new MixinBuilder()).addClientMixins(new String[] { "botania.BossBarHandler" })),

    CUSTOM_NPCS_PERMISSIONS(
        (new MixinBuilder()).addClientMixins(new String[] { "customnpcs.CustomNpcsPermissionsMixin" })),

    CUSTOM_NPCS_RENDER_NPC((new MixinBuilder()).addClientMixins(new String[] { "customnpcs.MixinRenderNPCInterface" })),

    FONT_STRATEGIST((new MixinBuilder()).addClientMixins(new String[] { "angelica.FontStrategistMixin" })),

    BATCHING_FONT_RENDERER((new MixinBuilder()).addClientMixins(new String[] { "angelica.BatchingFontRendererMixin" })),

    CRAFT_BOSS_BAR((new MixinBuilder()).addServerMixins(new String[] { "bukkit.boss.CraftBossBar" })),

    CRAFT_PLAYER((new MixinBuilder()).addServerMixins(new String[] { "bukkit.player.CraftPlayer" })),

    CRAFT_PLAYER_SPIGOT((new MixinBuilder()).addServerMixins(new String[] { "bukkit.playerspigot.CraftPlayerSpigot" })),

    CRAFT_SERVER((new MixinBuilder()).addServerMixins(new String[] { "bukkit.server.CraftServer" }));

    private final MixinBuilder builder;

    VanillaMixins(MixinBuilder builder) {
        this.builder = builder;
    }

    @Override
    public MixinBuilder getBuilder() {
        return this.builder;
    }
}
