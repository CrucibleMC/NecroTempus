package io.github.cruciblemc.necrotempus.modules.mixin.plugin;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.gtnewhorizon.gtnhmixins.ILateMixinLoader;
import com.gtnewhorizon.gtnhmixins.LateMixin;

@Deprecated
@LateMixin
@SuppressWarnings("unused")
public class NecroTempusLateMixins implements ILateMixinLoader {

    @Override
    public String getMixinConfig() {
        return "mixins.necrotempus.late.json";
    }

    @Override
    public List<String> getMixins(Set<String> loadedMods) {
        return Collections.emptyList();
    }
}
