package io.github.crucible.necrotempus.modules.playertab.component;

import lombok.Data;
import net.minecraft.util.IChatComponent;

import java.util.List;

@Data
public abstract class PlayerTab {

    private List<TabCell> cellList;
    private boolean drawPlayerHeads;

    private IChatComponent header;
    private IChatComponent footer;

}
