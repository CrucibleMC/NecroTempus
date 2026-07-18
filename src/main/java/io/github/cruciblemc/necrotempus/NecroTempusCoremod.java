package io.github.cruciblemc.necrotempus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.gtnewhorizon.gtnhmixins.IEarlyMixinLoader;

import com.gtnewhorizon.gtnhmixins.builders.IMixins;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin;
import io.github.cruciblemc.necrotempus.modules.mixin.NecroTempusMixins;

@IFMLLoadingPlugin.MCVersion("1.7.10")
public class NecroTempusCoremod implements IFMLLoadingPlugin, IEarlyMixinLoader {

    @Override
    public String[] getASMTransformerClass() {
        return null;
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {

    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }

    @Override
    public String getMixinConfig() {
        return "mixins.necrotempus.early.json";
    }

    @Override
    public List<String> getMixins(Set<String> loadedCoreMods) {
        return IMixins.getEarlyMixins(NecroTempusMixins.Early.class, loadedCoreMods);
    }

}
