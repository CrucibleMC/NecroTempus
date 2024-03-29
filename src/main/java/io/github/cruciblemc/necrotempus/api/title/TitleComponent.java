package io.github.cruciblemc.necrotempus.api.title;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.nbt.NBTTagCompound;

@Getter
@Setter
public class TitleComponent {

    @Getter
    @Setter
    private static TitleManager titleManager = new TitleManager();

    private int fadeIn = 500;
    private int stay = 3500;
    private int fadeOut = 1000;

    private final TitleElement[] elements = new TitleElement[2];

    public void addElement(TitleElement element) {
        elements[element.getType().ordinal()] = element;
    }

    public boolean hasElement(TitleType type) {
        return elements[type.ordinal()] != null;
    }

    public TitleElement getElement(TitleType type) {
        return elements[type.ordinal()];
    }

    public NBTTagCompound toNbt() {

        NBTTagCompound nbtTagCompound = new NBTTagCompound();

        nbtTagCompound.setInteger("fadeIn", fadeIn);
        nbtTagCompound.setInteger("stay", stay);
        nbtTagCompound.setInteger("fadeOut", fadeOut);

        for (TitleType type : TitleType.values()) {
            TitleElement titleElement = elements[type.ordinal()];
            if (titleElement != null)
                nbtTagCompound.setTag(type.name().toLowerCase(), titleElement.toNbt());
        }

        return nbtTagCompound;
    }

    public static TitleComponent fromCompound(NBTTagCompound tagCompound) {

        TitleComponent component = new TitleComponent();

        component.fadeIn = tagCompound.getInteger("fadeIn");
        component.stay = tagCompound.getInteger("stay");
        component.fadeOut = tagCompound.getInteger("fadeOut");

        for (TitleType type : TitleType.values()) {
            String key = type.name().toLowerCase();
            if (tagCompound.hasKey(key)) {
                component.addElement(TitleElement.fromCompound(tagCompound.getCompoundTag(key)));
            }
        }

        return component;
    }

}
