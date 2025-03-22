package io.github.cruciblemc.necrotempus.modules.mixin.mixins.minecraft;

import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.S2DPacketOpenWindow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.io.IOException;

@Mixin(value = S2DPacketOpenWindow.class)
abstract class S2DPacketOpenWindowMixin {

    @Shadow
    private int field_148909_a;

    @Shadow
    private int field_148907_b;

    @Shadow
    private String field_148908_c;

    @Shadow
    private int field_148905_d;

    @Shadow
    private boolean field_148906_e;

    @Shadow
    private int field_148904_f;

    /**
     * @author NecroTempus
     * @reason allow for bigger strings as container titles
     */
    @Overwrite
    public void readPacketData(PacketBuffer data) throws IOException {
        this.field_148909_a = data.readUnsignedByte();
        this.field_148907_b = data.readUnsignedByte();
        this.field_148908_c = data.readStringFromBuffer(32767); //NecroTempus: Increase the string size
        this.field_148905_d = data.readUnsignedByte();
        this.field_148906_e = data.readBoolean();

        if (this.field_148907_b == 11)
        {
            this.field_148904_f = data.readInt();
        }
    }

}
