package io.github.cruciblemc.necrotempus.modules.features.bossbar.compat;

import cpw.mods.fml.common.FMLCommonHandler;
import io.github.cruciblemc.necrotempus.api.bossbar.BossBarColor;
import io.github.cruciblemc.necrotempus.api.bossbar.BossBarType;
import io.github.cruciblemc.necrotempus.modules.features.bossbar.client.render.BossDisplayAdapterListener;
import io.github.cruciblemc.necrotempus.modules.features.bossbar.component.BossDisplayAdapter;
import minetweaker.IUndoableAction;
import minetweaker.MineTweakerAPI;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenClass(value = "mods.necrotempus.BossBar")
public class CraftTweakerBossBar {

    @ZenMethod
    public static void customize(String entity, int color, String type){
        MineTweakerAPI.apply(new BossBarCustomizeAction(entity, color, type));
    }

    public static class BossBarCustomizeAction implements IUndoableAction{

        private final String entity;
        private final BossBarColor color;
        private final BossBarType type;

        public BossBarCustomizeAction(String entity, int color, String type) {
            this.entity = entity;
            this.color = BossBarColor.valueOfString("$" + color);
            this.type = BossBarType.valueOfString(type);
        }

        @Override
        public boolean canUndo() {
            return true;
        }

        @Override
        public void apply() {
            if(FMLCommonHandler.instance().getEffectiveSide().isClient()){
                BossDisplayAdapter bossDisplayAdapter = new BossDisplayAdapter(entity, color, type);
                BossDisplayAdapterListener.getCustomAdapters().add(bossDisplayAdapter);
            }
        }

        @Override
        public void undo() {
            if(FMLCommonHandler.instance().getEffectiveSide().isClient()){
                BossDisplayAdapterListener.getCustomAdapters().removeIf( el -> el.getTargetClass().equalsIgnoreCase(entity) );
            }
        }

        @Override
        public String describe() {
            return String.format("Registering CustomBossBarAdapter for entity %s. (Color: RGB(%s), Type: %s)", entity, color.intValue(), type.getType());
        }

        @Override
        public String describeUndo() {
            return String.format("Removing CustomBossBarAdapter for entity %s. (Color: RGB(%s), Type: %s)", entity, color.intValue(), type.getType());
        }

        @Override
        public Object getOverrideKey() {
            return null;
        }

    }

}
