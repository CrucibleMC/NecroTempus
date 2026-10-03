package io.github.cruciblemc.necrotempus.utils;

import com.mojang.authlib.GameProfile;

public final class ChatHead {

    public final GameProfile profile;
    public final int offset;

    public ChatHead(GameProfile profile, int offset) {
        this.profile = profile;
        this.offset = offset;
    }
}
