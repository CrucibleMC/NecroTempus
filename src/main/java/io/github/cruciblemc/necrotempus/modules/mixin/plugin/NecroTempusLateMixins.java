package io.github.cruciblemc.necrotempus.modules.mixin.plugin;

import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.gtnhmixins.ILateMixinLoader;
import com.gtnewhorizon.gtnhmixins.LateMixin;
import com.gtnewhorizon.gtnhmixins.builders.IMixins;

import io.github.cruciblemc.necrotempus.modules.mixin.NecroTempusMixins;
import org.jetbrains.annotations.NotNull;

@LateMixin
public class NecroTempusLateMixins implements ILateMixinLoader {

    @Override
    public String getMixinConfig() {
        return "mixins.necrotempus.late.json";
    }

    @NotNull
    @Override
    public List<String> getMixins(Set<String> loadedMods) {
        return IMixins.getLateMixins(NecroTempusMixins.Late.class, loadedMods);
    }

}
