package io.github.cruciblemc.necrotempus.modules.features.modernfonts;

import lombok.AllArgsConstructor;
import lombok.Builder;
import net.minecraft.util.ResourceLocation;

@AllArgsConstructor
@Builder
public class ModernFontEntry {

    public int atlasX;
    public int atlasY;

    public int width;
    public int height;

    public int ascent;

    public int frameWidth;
    public int frameHeight;

    public int totalWidth;
    public int totalHeight;

    public ResourceLocation location;

    @Override
    public String toString() {
        return "ModernFontEntry{" +
                "atlasX=" + atlasX +
                ", atlasY=" + atlasY +
                ", width=" + width +
                ", height=" + height +
                ", ascent=" + ascent +
                ", frameWidth=" + frameWidth +
                ", frameHeight=" + frameHeight +
                ", totalWidth=" + totalWidth +
                ", totalHeight=" + totalHeight +
                ", location=" + location +
                '}';
    }
}
