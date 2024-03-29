package io.github.cruciblemc.necrotempus.modules.features.title.component;

import io.github.cruciblemc.necrotempus.api.title.TitleComponent;
import io.github.cruciblemc.necrotempus.api.title.TitleElement;
import io.github.cruciblemc.necrotempus.api.title.TitleType;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.nbt.NBTTagCompound;

public class TimedTitle extends TitleComponent {

    @Getter
    @Setter
    private long startTime;

    public static TimedTitle fromCompound(NBTTagCompound tagCompound) {

        TimedTitle component = new TimedTitle();

        component.setFadeIn(tagCompound.getInteger("fadeIn"));
        component.setStay(tagCompound.getInteger("stay"));
        component.setFadeOut(tagCompound.getInteger("fadeOut"));
        component.setStartTime(System.currentTimeMillis());

        for (TitleType type : TitleType.values()) {
            String key = type.name().toLowerCase();
            if (tagCompound.hasKey(key)) {
                component.addElement(TitleElement.fromCompound(tagCompound.getCompoundTag(key)));
            }
        }

        return component;
    }

}
