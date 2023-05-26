package io.github.crucible.necrotempus.api.playertab;

import lombok.*;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlayerTab {

    @Getter @Setter
    private static PlayerTabManager playerTabManager = new PlayerTabManager();

    private List<TabCell> cellList;
    private boolean drawPlayerHeads;

    private IChatComponent header;
    private IChatComponent footer;

    public NBTTagCompound toNbt(){

        NBTTagCompound nbtTagCompound = new NBTTagCompound();

        NBTTagList cellListTag = new NBTTagList();
        for(TabCell cell : cellList)
            cellListTag.appendTag(cell.toNbt());
        nbtTagCompound.setTag("cellList", cellListTag);

        nbtTagCompound.setBoolean("drawPlayerHeads", drawPlayerHeads);

        if(header != null) nbtTagCompound.setString("header", header.getUnformattedText());
        if(footer != null) nbtTagCompound.setString("footer", footer.getUnformattedText());

        return nbtTagCompound;
    }

    public static PlayerTab fromCompound(NBTTagCompound compound){

        NBTTagList tagList = compound.getTagList("cellList", 10);
        ArrayList<TabCell> cells = new ArrayList<>();

        for(int i = 0; i < tagList.tagCount(); i++)
            cells.add(TabCell.fromNBT(tagList.getCompoundTagAt(i)));

        IChatComponent header = null;
        if(compound.hasKey("header"))
            header = new ChatComponentText(compound.getString("header"));

        IChatComponent footer = null;
        if(compound.hasKey("footer"))
            footer = new ChatComponentText(compound.getString("footer"));

        return new PlayerTab(
                cells,
                compound.getBoolean("drawPlayerHeads"),
                header,
                footer
                );
    }

}
