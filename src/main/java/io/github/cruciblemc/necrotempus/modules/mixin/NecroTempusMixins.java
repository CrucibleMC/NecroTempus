package io.github.cruciblemc.necrotempus.modules.mixin;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.gtnhmixins.builders.IMixins;
import com.gtnewhorizon.gtnhmixins.builders.MixinBuilder;

import io.github.cruciblemc.necrotempus.NecroTempusConfig;

public enum NecroTempusMixins implements IMixins {

    FML_CLIENT_HANDLER((new MixinBuilder()).addClientMixins("fml.FMLClientHandler")),

    FONT_RENDERER((new MixinBuilder()).addClientMixins("minecraft.FontRendererMixin")),

    GUI_CONTAINER((new MixinBuilder()).addClientMixins("minecraft.GuiContainerMixin")),
    GUI_INGAME((new MixinBuilder()).setApplyIf(() -> NecroTempusConfig.ScoreBoardEnabled)
        .addClientMixins("minecraft.GuiIngameMixin")),
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

    FONT_STRATEGIST((new MixinBuilder()).addRequiredMod(MixinTargetedMod.ANGELICA)
        .addClientMixins("angelica.FontStrategistMixin")),
    BATCHING_FONT_RENDERER((new MixinBuilder()).addRequiredMod(MixinTargetedMod.ANGELICA)
        .addClientMixins("angelica.BatchingFontRendererMixin")),

    CRAFT_BOSS_BAR((new MixinBuilder()).addServerMixins("bukkit.boss.CraftBossBar")),
    CRAFT_PLAYER((new MixinBuilder()).addServerMixins("bukkit.player.CraftPlayer")),
    CRAFT_PLAYER_SPIGOT((new MixinBuilder()).addServerMixins("bukkit.playerspigot.CraftPlayerSpigot")),
    CRAFT_SERVER((new MixinBuilder()).addServerMixins("bukkit.server.CraftServer"));

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
