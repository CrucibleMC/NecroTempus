package io.github.cruciblemc.necrotempus.utils;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;

public class SideUtils {

    public static void enforceSide(Side expectedSide, String reason) {
        if (FMLCommonHandler.instance().getEffectiveSide() != expectedSide) {
            throw new IllegalStateException(reason);
        }
    }

}
