package io.github.cruciblemc.necrotempus.modules.mixin.plugin;

import com.gtnewhorizon.gtnhmixins.builders.ITargetMod;
import com.gtnewhorizon.gtnhmixins.builders.TargetModBuilder;

public enum TargetedMod implements ITargetMod {

    BOTANIA(new TargetModBuilder().setTargetClass("vazkii.botania.common.core.BotaniaCreativeTab")
        .setModId("botania")),
    CUSTOM_NPCS(new TargetModBuilder().setTargetClass("noppes.npcs.CustomNpcs")
        .setModId("customnpcs")),
    ANGELICA(new TargetModBuilder().setTargetClass("com.gtnewhorizons.angelica.AngelicaMod")
        .setModId("angelica"));

    private final TargetModBuilder builder;

    TargetedMod(TargetModBuilder builder) {
        this.builder = builder;
    }

    @Override
    public TargetModBuilder getBuilder() {
        return this.builder;
    }
}
