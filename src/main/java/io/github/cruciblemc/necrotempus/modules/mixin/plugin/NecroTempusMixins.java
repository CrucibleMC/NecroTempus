package io.github.cruciblemc.necrotempus.modules.mixin.plugin;

import com.gtnewhorizon.gtnhmixins.builders.IMixins;
import com.gtnewhorizon.gtnhmixins.builders.MixinBuilder;

@Deprecated
public enum NecroTempusMixins implements IMixins {

    EMPTY((new MixinBuilder()).addClientMixins());

    private final MixinBuilder builder;

    NecroTempusMixins(MixinBuilder builder) {
        this.builder = builder;
    }

    @Override
    public MixinBuilder getBuilder() {
        return this.builder;
    }
}
