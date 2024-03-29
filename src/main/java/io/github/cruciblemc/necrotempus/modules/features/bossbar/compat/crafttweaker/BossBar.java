package io.github.cruciblemc.necrotempus.modules.features.bossbar.compat.crafttweaker;

import cpw.mods.fml.common.FMLCommonHandler;
import io.github.cruciblemc.necrotempus.Tags;
import io.github.cruciblemc.necrotempus.api.bossbar.BossBarColor;
import io.github.cruciblemc.necrotempus.api.bossbar.BossBarType;
import io.github.cruciblemc.necrotempus.modules.features.bossbar.client.render.BossDisplayAdapterListener;
import io.github.cruciblemc.necrotempus.modules.features.bossbar.component.BossDisplayAdapter;
import minetweaker.IUndoableAction;
import minetweaker.MineTweakerAPI;
import minetweaker.annotations.ModOnly;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenClass(value = "necrotempus.crafttweaker.BossBar")
@ModOnly(Tags.MODID)
public class BossBar {

    @ZenMethod
    public static void customize(String entity, String type, int color) {
        MineTweakerAPI.apply(new BossBarCustomizeAction(entity, color, type));
    }

    public static class BossBarCustomizeAction implements IUndoableAction {

        private final String entity;
        private final BossBarColor color;
        private final BossBarType type;
        private final BossDisplayAdapter bossDisplayAdapter;

        public BossBarCustomizeAction(Object entity, int color, Object type) {
            this.entity = (String) entity;
            this.color = BossBarColor.valueOfString("$" + color);
            this.type = BossBarType.valueOfString((String) type);
            this.bossDisplayAdapter = new BossDisplayAdapter(this.entity, this.color, this.type);
        }

        @Override
        public boolean canUndo() {
            return true;
        }

        @Override
        public void apply() {
            if (FMLCommonHandler.instance().getSide().isClient()) {
                BossDisplayAdapterListener.add(bossDisplayAdapter);
            }
        }

        @Override
        public void undo() {
            if (FMLCommonHandler.instance().getSide().isClient()) {
                BossDisplayAdapterListener.remove(bossDisplayAdapter);
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
