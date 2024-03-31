package io.github.cruciblemc.necrotempus.modules.features.glyphs.compat.crafttweaker;

import cpw.mods.fml.common.FMLCommonHandler;
import io.github.cruciblemc.necrotempus.Tags;
import io.github.cruciblemc.necrotempus.modules.features.glyphs.CustomGlyphs;
import io.github.cruciblemc.necrotempus.modules.features.glyphs.GlyphsRegistry;
import minetweaker.IUndoableAction;
import minetweaker.MineTweakerAPI;
import minetweaker.annotations.ModOnly;
import net.minecraft.util.ResourceLocation;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

import static io.github.cruciblemc.necrotempus.modules.features.glyphs.CustomGlyphs.FitMode.NONE;

@ZenClass(value = "necrotempus.crafttweaker.Glyph")
@ModOnly(Tags.MODID)
public class Glyphs {

    @ZenMethod
    public static void register(String target, String resource, int horizontalPadding, int verticalPadding, int width, int height) {
        MineTweakerAPI.apply(new GlyphCustomizeAddAction(target.charAt(0), new ResourceLocation(resource), horizontalPadding, verticalPadding, width, height));
    }

    @ZenMethod
    public static void register(String target, String resource, int horizontalPadding, int verticalPadding, int width, int height, String fitMode) {
        MineTweakerAPI.apply(new GlyphCustomizeAddAction(target.charAt(0), new ResourceLocation(resource), horizontalPadding, verticalPadding, width, height, fitMode, -1));
    }

    @ZenMethod
    public static void register(String target, String resource, int horizontalPadding, int verticalPadding, int width, int height, String fitMode, int charWidth) {
        MineTweakerAPI.apply(new GlyphCustomizeAddAction(target.charAt(0), new ResourceLocation(resource), horizontalPadding, verticalPadding, width, height, fitMode, charWidth));
    }

    @ZenMethod
    public static void unregister(String target) {
        MineTweakerAPI.apply(new GlyphCustomizeRemoveAction(target.charAt(0)));
    }

    public static class GlyphCustomizeRemoveAction implements IUndoableAction {

        private final Character target;
        private CustomGlyphs customGlyphs;

        public GlyphCustomizeRemoveAction(Character target) {
            this.target = target;
        }

        @Override
        public void apply() {
            if (FMLCommonHandler.instance().getSide().isClient()) {

                customGlyphs = GlyphsRegistry.getCandidate(target);

                if (customGlyphs != null)
                    GlyphsRegistry.unregister(customGlyphs);
            }
        }

        @Override
        public boolean canUndo() {
            return FMLCommonHandler.instance().getSide().isClient() && customGlyphs != null;
        }

        @Override
        public void undo() {
            if (FMLCommonHandler.instance().getSide().isClient()) {
                GlyphsRegistry.register(customGlyphs);
            }
        }

        @Override
        public String describe() {
            return String.format("Removing Custom Glyph for character %s.", target);
        }

        @Override
        public String describeUndo() {
            return String.format(
                    "Registering Custom Glyph for character %s. (Width: %d, Height: %d, HPadding: %d, VPadding: %d, FitMode: %s, CharWidth: %d)",
                    customGlyphs.getTarget(),
                    customGlyphs.getWidth(),
                    customGlyphs.getHeight(),
                    customGlyphs.getHorizontalPadding(),
                    customGlyphs.getVerticalPadding(),
                    customGlyphs.getFitMode(),
                    customGlyphs.getCharWidth()
            );
        }

        @Override
        public Object getOverrideKey() {
            return null;
        }

    }

    public static class GlyphCustomizeAddAction implements IUndoableAction {

        private final char target;
        private final ResourceLocation resource;
        private final int horizontalPadding;
        private final int verticalPadding;
        private final int width;
        private final int height;
        private CustomGlyphs.FitMode fitMode = NONE;
        private int charWidth = -1;


        public GlyphCustomizeAddAction(char target, ResourceLocation resource, int horizontalPadding, int verticalPadding, int width, int height) {
            this.target = target;
            this.resource = resource;
            this.horizontalPadding = horizontalPadding;
            this.verticalPadding = verticalPadding;
            this.width = width;
            this.height = height;
        }

        public GlyphCustomizeAddAction(char target, ResourceLocation resource, int horizontalPadding, int verticalPadding, int width, int height, String fitMode, int charWidth) {
            this.target = target;
            this.resource = resource;
            this.horizontalPadding = horizontalPadding;
            this.verticalPadding = verticalPadding;
            this.width = width;
            this.height = height;
            this.charWidth = charWidth;
            this.fitMode = CustomGlyphs.FitMode.parse(fitMode);
        }

        @Override
        public boolean canUndo() {
            return true;
        }

        @Override
        public void apply() {
            if (FMLCommonHandler.instance().getSide().isClient()) {

                CustomGlyphs customGlyphs = new CustomGlyphs(target, resource, horizontalPadding, verticalPadding, width, height);

                customGlyphs.setFitMode(fitMode);
                customGlyphs.setCharWidth(charWidth);

                GlyphsRegistry.register(customGlyphs);

            }
        }

        @Override
        public void undo() {
            if (FMLCommonHandler.instance().getSide().isClient()) {
                GlyphsRegistry.unregister(target);
            }
        }

        @Override
        public String describe() {
            return String.format("Registering Custom Glyph for character %s. (Width: %d, Height: %d, HPadding: %d, VPadding: %d, FitMode: %s, CharWidth: %d)", target, width, height, horizontalPadding, verticalPadding, fitMode, charWidth);
        }

        @Override
        public String describeUndo() {
            return String.format("Removing Custom Glyph for character %s. (Width: %d, Height: %d, HPadding: %d, VPadding: %d, FitMode: %s, CharWidth: %d)", target, width, height, horizontalPadding, verticalPadding, fitMode, charWidth);
        }

        @Override
        public Object getOverrideKey() {
            return null;
        }

    }

}
