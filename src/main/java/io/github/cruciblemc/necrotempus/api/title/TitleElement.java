package io.github.cruciblemc.necrotempus.api.title;

import lombok.Getter;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;

@Getter
public class TitleElement {

    private final IChatComponent text;
    private final TitleType type;

    private TitleElement(IChatComponent text, TitleType type) {
        this.text = text;
        this.type = type;
    }

    public static TitleElement titleOf(IChatComponent text) {
        return new TitleElement(text, TitleType.TITLE);
    }

    public static TitleElement subtitleOf(IChatComponent text) {
        return new TitleElement(text, TitleType.SUBTITLE);
    }

    public NBTTagCompound toNbt() {
        NBTTagCompound nbtTagCompound = new NBTTagCompound();
        nbtTagCompound.setString("text", text.getUnformattedText());
        nbtTagCompound.setString("type", type.name());
        return nbtTagCompound;
    }

    public static TitleElement fromCompound(NBTTagCompound tagCompound) {
        return new TitleElement(
                new ChatComponentText(tagCompound.getString("text")),
                TitleType.of(tagCompound.getString("type"))
        );
    }

}
