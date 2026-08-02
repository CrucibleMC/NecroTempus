package io.github.cruciblemc.necrotempus.modules.mixin;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.gtnhmixins.builders.IMixins;
import com.gtnewhorizon.gtnhmixins.builders.MixinBuilder;

import io.github.cruciblemc.necrotempus.NecroTempusConfig;
import io.github.cruciblemc.necrotempus.modules.features.font.FontFeatureToggles;

public enum NecroTempusMixins implements IMixins {

    FML_CLIENT_HANDLER((new MixinBuilder()).addClientMixins("fml.FMLClientHandler")),

    FONT_RENDERER((new MixinBuilder()).setApplyIf(() -> FontFeatureToggles.isVanillaFontMixinEnabled())
        .addClientMixins("minecraft.FontRendererMixin")),

    GUI_CONTAINER((new MixinBuilder()).addClientMixins("minecraft.GuiContainerMixin")),
    GUI_CHAT((new MixinBuilder()).addClientMixins("minecraft.GuiChatMixin")),
    GUI_INGAME((new MixinBuilder()).setApplyIf(() -> NecroTempusConfig.ScoreBoardEnabled)
        .addClientMixins("minecraft.GuiIngameMixin")),
    GUI_NEW_CHAT((new MixinBuilder()).addClientMixins("minecraft.GuiNewChatMixin")),
    ABSTRACT_CLIENT_PLAYER((new MixinBuilder()).addExcludedMod(MixinTargetedMod.HODGEPODGE)
        .addClientMixins("minecraft.AbstractClientPlayerMixin")),
    THREAD_DOWNLOAD_IMAGE_DATA((new MixinBuilder()).addExcludedMod(MixinTargetedMod.HODGEPODGE)
        .addClientMixins("minecraft.ThreadDownloadImageDataMixin")),
    PLAYER_LIST_ITEM_PACKET((new MixinBuilder()).addClientMixins("minecraft.S38PacketPlayerListItemMixin")),
    TEAMS_PACKET((new MixinBuilder()).addClientMixins("minecraft.S3EPacketTeamsMixin")),
    GUI_CHEST((new MixinBuilder()).addClientMixins("minecraft.inv.GuiChestMixin")),
    GUI_BREWING_STAND((new MixinBuilder()).addClientMixins("minecraft.inv.GuiBrewingStandMixin")),
    GUI_DISPENSER((new MixinBuilder()).addClientMixins("minecraft.inv.GuiDispenserMixin")),
    GUI_FURNACE((new MixinBuilder()).addClientMixins("minecraft.inv.GuiFurnaceMixin")),
    GUI_HORSE_INVENTORY((new MixinBuilder()).addClientMixins("minecraft.inv.GuiScreenHorseInventoryMixin")),
    GUI_ENCHANTMENT((new MixinBuilder()).addClientMixins("minecraft.inv.GuiEnchantmentMixin")),
    GUI_HOPPER((new MixinBuilder()).addClientMixins("minecraft.inv.GuiHopperMixin")),
    GUI_MERCHANT((new MixinBuilder()).addClientMixins("minecraft.inv.GuiMerchantMixin")),

    BOTANIA_BOSS_BAR((new MixinBuilder()).addRequiredMod(MixinTargetedMod.BOTANIA)
        .addClientMixins("botania.BossBarHandler")),

    CUSTOM_NPCS_PERMISSIONS((new MixinBuilder()).addRequiredMod(MixinTargetedMod.CUSTOM_NPCS)
        .addClientMixins("customnpcs.CustomNpcsPermissionsMixin")),
    CUSTOM_NPCS_RENDER_NPC((new MixinBuilder()).addRequiredMod(MixinTargetedMod.CUSTOM_NPCS)
        .addClientMixins("customnpcs.MixinRenderNPCInterface")),

    BATCHING_FONT_RENDERER((new MixinBuilder()).setApplyIf(() -> FontFeatureToggles.isAngelicaFontMixinEnabled())
        .addRequiredMod(MixinTargetedMod.ANGELICA)
        .addClientMixins("angelica.BatchingFontRendererMixin")),

    CRAFT_BOSS_BAR((new MixinBuilder()).addRequiredMod(MixinTargetedMod.CRUCIBLE)
        .addServerMixins("bukkit.boss.CraftBossBar")),
    CRAFT_PLAYER((new MixinBuilder()).addRequiredMod(MixinTargetedMod.CRUCIBLE)
        .addServerMixins("bukkit.player.CraftPlayer")),
    CRAFT_PLAYER_SPIGOT((new MixinBuilder()).addRequiredMod(MixinTargetedMod.CRUCIBLE)
        .addServerMixins("bukkit.playerspigot.CraftPlayerSpigot")),
    CRAFT_TEAM((new MixinBuilder()).addRequiredMod(MixinTargetedMod.CRUCIBLE)
        .addServerMixins("bukkit.scoreboard.CraftTeam")),
    CRAFT_SERVER((new MixinBuilder()).addRequiredMod(MixinTargetedMod.CRUCIBLE)
        .addServerMixins("bukkit.server.CraftServer"));

    private final MixinBuilder builder;

    NecroTempusMixins(MixinBuilder builder) {
        this.builder = builder;
    }

    @NotNull
    @Override
    public MixinBuilder getBuilder() {
        return this.builder;
    }

    public enum Early implements IMixins {

        GAME_RESET_DATA((new MixinBuilder()).setPhase(Phase.EARLY)
            .addClientMixins("reset.GameDataMixin"));

        private final MixinBuilder builder;

        Early(MixinBuilder builder) {
            this.builder = builder;
        }

        @NotNull
        @Override
        public MixinBuilder getBuilder() {
            return this.builder;
        }

    }

    public enum Late implements IMixins {

        ;

        private final MixinBuilder builder;

        Late(MixinBuilder builder) {
            this.builder = builder;
        }

        @NotNull
        @Override
        public MixinBuilder getBuilder() {
            return this.builder;
        }

    }

}
