package io.github.crucible.necrotempus.modules.playertab.component;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.IChatComponent;

@Getter
@AllArgsConstructor
public class TabCell {

    @Setter
    private IChatComponent displayName;

    private String linkedUserName;

    private String displaySkullName;

    private boolean displayScore;

    private int playerPing;

}
