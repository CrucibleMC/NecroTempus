package io.github.cruciblemc.necrotempus.modules.mixin.plugin;

import com.gtnewhorizon.gtnhmixins.builders.IMixins;
import com.gtnewhorizon.gtnhmixins.builders.MixinBuilder;

import io.github.cruciblemc.necrotempus.NecroTempusConfig;

public enum VanillaMixins implements IMixins {

    FML_CLIENT_HANDLER((new MixinBuilder()).addClientMixins("fml.FMLClientHandler")),

    FONT_RENDERER((new MixinBuilder()).setApplyIf(() -> NecroTempusConfig.HexColorsEnabled)
        .addClientMixins("minecraft.FontRendererMixin")),

    FONT_RENDERER_2((new MixinBuilder()).addClientMixins("minecraft.FontRenderer2Mixin")),

    GUI_CONTAINER((new MixinBuilder()).addClientMixins("minecraft.GuiContainerMixin")),

    GUI_INGAME((new MixinBuilder()).setApplyIf(() -> NecroTempusConfig.ScoreBoardEnabled)
        .addClientMixins("minecraft.GuiIngameMixin")),

    GUI_CHEST((new MixinBuilder()).addClientMixins("minecraft.inv.GuiChestMixin")),

    GUI_BREWING_STAND((new MixinBuilder()).addClientMixins("minecraft.inv.GuiBrewingStandMixin")),

    GUI_DISPENSER((new MixinBuilder()).addClientMixins("minecraft.inv.GuiDispenserMixin")),

    GUI_FURNACE((new MixinBuilder()).addClientMixins("minecraft.inv.GuiFurnaceMixin")),

    GUI_HORSE_INVENTORY(
        (new MixinBuilder()).addClientMixins("minecraft.inv.GuiScreenHorseInventoryMixin")),

    GUI_ENCHANTMENT((new MixinBuilder()).addClientMixins("minecraft.inv.GuiEnchantmentMixin")),

    GUI_HOPPER((new MixinBuilder()).addClientMixins("minecraft.inv.GuiHopperMixin")),

    GUI_MERCHANT((new MixinBuilder()).addClientMixins("minecraft.inv.GuiMerchantMixin")),

    BOTANIA_BOSS_BAR((new MixinBuilder()).addClientMixins("botania.BossBarHandler")),

    CUSTOM_NPCS_PERMISSIONS(
        (new MixinBuilder()).addClientMixins("customnpcs.CustomNpcsPermissionsMixin")),

    CUSTOM_NPCS_RENDER_NPC((new MixinBuilder()).addClientMixins("customnpcs.MixinRenderNPCInterface")),

    FONT_STRATEGIST((new MixinBuilder()).addClientMixins("angelica.FontStrategistMixin")),

    BATCHING_FONT_RENDERER((new MixinBuilder()).addClientMixins("angelica.BatchingFontRendererMixin")),

    CRAFT_BOSS_BAR((new MixinBuilder()).addServerMixins("bukkit.boss.CraftBossBar")),

    CRAFT_PLAYER((new MixinBuilder()).addServerMixins("bukkit.player.CraftPlayer")),

    CRAFT_PLAYER_SPIGOT((new MixinBuilder()).addServerMixins("bukkit.playerspigot.CraftPlayerSpigot")),

    CRAFT_SERVER((new MixinBuilder()).addServerMixins("bukkit.server.CraftServer"));

    private final MixinBuilder builder;

    VanillaMixins(MixinBuilder builder) {
        this.builder = builder;
    }

    @Override
    public MixinBuilder getBuilder() {
        return this.builder;
    }
}
