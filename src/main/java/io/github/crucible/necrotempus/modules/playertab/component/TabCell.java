package io.github.crucible.necrotempus.modules.playertab.component;

import com.mojang.authlib.GameProfile;
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

    private GameProfile skullProfile;

    private boolean displayScore;

    private int playerPing;

}
