package io.github.cruciblemc.necrotempus.modules.mixin.plugin;

import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.lib.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import com.gtnewhorizon.gtnhmixins.builders.IMixins;

import io.github.cruciblemc.necrotempus.modules.mixin.NecroTempusMixins;

public class MixinPlugin implements IMixinConfigPlugin {

    private static final Logger LOG = LogManager.getLogger("necrotempus mixins");

    @Override
    public void onLoad(String mixinPackage) {}

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        try {
            Class.forName(targetClassName, false, MixinPlugin.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            LOG.warn("Skipping {} because target class {} was not found", mixinClassName, targetClassName);
            return false;
        }
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() {
        return IMixins.getMixins(NecroTempusMixins.class);
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
