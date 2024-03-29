package io.github.cruciblemc.necrotempus.modules.features.glyphs;

import io.github.cruciblemc.necrotempus.utils.MathUtils;
import lombok.Data;
import net.minecraft.util.ResourceLocation;

import static io.github.cruciblemc.necrotempus.modules.features.glyphs.CustomGlyphs.FitMode.*;

@Data
public class CustomGlyphs {

    private final char target;

    private final ResourceLocation resource;

    private final int horizontalPadding;
    private final int verticalPadding;

    private final int width;
    private final int height;

    private int charWidth = -1;

    private FitMode fitMode = NONE;

    public int getFinalCharacterWidth() {

        if (charWidth >= 0) {
            return charWidth;
        }
        if (fitMode == CONTAINS)
            return 10;
        else if (fitMode == VERTICALLY) {
            return (int) Math.ceil(MathUtils.calculateWidth(width, height, 9)) + 1;
        }
        return width;
    }

    public enum FitMode {

        NONE, VERTICALLY, CONTAINS;

        public static FitMode parse(String string) {

            if (string == null)
                return NONE;

            for (FitMode el : values()) {
                if (el.name().equalsIgnoreCase(string))
                    return el;
            }

            return NONE;
        }

    }

}
