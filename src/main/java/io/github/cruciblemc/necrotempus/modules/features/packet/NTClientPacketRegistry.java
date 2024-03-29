package io.github.cruciblemc.necrotempus.modules.features.packet;

import cpw.mods.fml.common.FMLCommonHandler;

public interface NTClientPacketRegistry {

    static void init() {
        FMLCommonHandler.instance().bus().register(NTClientPacket.NecroTempusClient.getInstance());
    }

}
