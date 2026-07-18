package io.github.cruciblemc.necrotempus.modules.mixin;

import com.gtnewhorizon.gtnhmixins.builders.ITargetMod;
import com.gtnewhorizon.gtnhmixins.builders.TargetModBuilder;
import org.jetbrains.annotations.NotNull;

public enum MixinTargetedMod implements ITargetMod {

    BOTANIA(new TargetModBuilder().setTargetClass("vazkii.botania.common.core.BotaniaCreativeTab")
        .setModId("botania")),

    CUSTOM_NPCS(new TargetModBuilder().setTargetClass("noppes.npcs.CustomNpcs")
        .setModId("customnpcs")),

    ANGELICA(new TargetModBuilder().setTargetClass("com.gtnewhorizons.angelica.AngelicaMod")
        .setModId("angelica"));

    private final TargetModBuilder builder;

    MixinTargetedMod(TargetModBuilder builder) {
        this.builder = builder;
    }

    @NotNull
    @Override
    public TargetModBuilder getBuilder() {
        return this.builder;
    }
}
