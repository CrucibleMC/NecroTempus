package io.github.cruciblemc.necrotempus.api.actionbar;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;

@AllArgsConstructor
@Getter
@Setter
public class ActionBar {

    @Getter
    @Setter
    private static ActionBarManager actionBarManager = new ActionBarManager();

    private int time;
    private IChatComponent text;

    public NBTTagCompound toNbt() {
        NBTTagCompound tagCompound = new NBTTagCompound();
        tagCompound.setInteger("time", time);
        tagCompound.setString("text", text.getUnformattedText());
        return tagCompound;
    }

    public static ActionBar fromCompound(NBTTagCompound tagCompound) {
        return new ActionBar(
                tagCompound.getInteger("time"),
                new ChatComponentText(tagCompound.getString("text"))
        );
    }
}
