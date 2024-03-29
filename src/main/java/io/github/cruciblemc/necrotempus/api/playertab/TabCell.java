package io.github.cruciblemc.necrotempus.api.playertab;

import com.mojang.authlib.GameProfile;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;

@Getter
@AllArgsConstructor
public class TabCell {

    @Setter
    private IChatComponent displayName;

    private String linkedUserName;

    private GameProfile skullProfile;

    private boolean displayScore;

    private int playerPing;

    public static TabCell fromNBT(NBTTagCompound tagCompound) {
        return new TabCell(
                new ChatComponentText(tagCompound.getString("displayName")),
                tagCompound.getString("linkedUserName"),
                NBTUtil.func_152459_a(tagCompound.getCompoundTag("skullProfile")),
                tagCompound.getBoolean("displayScore"),
                tagCompound.getInteger("playerPing")
        );
    }

    public NBTTagCompound toNbt() {

        NBTTagCompound tagCompound = new NBTTagCompound();

        tagCompound.setString("displayName", displayName.getUnformattedText());
        tagCompound.setString("linkedUserName", linkedUserName);

        NBTTagCompound skullProfileTag = new NBTTagCompound();
        NBTUtil.func_152460_a(skullProfileTag, skullProfile);
        tagCompound.setTag("skullProfile", skullProfileTag);

        tagCompound.setBoolean("displayScore", displayScore);
        tagCompound.setInteger("playerPing", playerPing);

        return tagCompound;
    }

}
