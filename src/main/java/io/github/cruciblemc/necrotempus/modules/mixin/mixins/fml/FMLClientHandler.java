package io.github.cruciblemc.necrotempus.modules.mixin.mixins.fml;

import com.mojang.realmsclient.gui.ChatFormatting;
import cpw.mods.fml.client.ExtendedServerListData;
import io.github.cruciblemc.necrotempus.Tags;
import io.github.cruciblemc.necrotempus.modules.crucible.CrucibleServerListEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ServerListEntryNormal;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;

@Mixin(value = cpw.mods.fml.client.FMLClientHandler.class, remap = false)
public class FMLClientHandler {

    @Shadow
    private Minecraft client;

    @Shadow
    private Map<ServerData, ExtendedServerListData> serverDataTag;

    @Shadow
    @Final
    private static final ResourceLocation iconSheet = new ResourceLocation("fml:textures/gui/icons.png");

    /**
     * @author brunoxkk0
     * @reason Improve crucible server compatibility
     */
    @Overwrite
    public String enhanceServerListEntry(ServerListEntryNormal serverListEntry, ServerData serverEntry, int x, int width, int y, int relativeMouseX, int relativeMouseY) {
        String tooltip;
        int idx;
        boolean blocked;
        int crucibleMode = 0;

        if (serverDataTag.containsKey(serverEntry)) {

            ExtendedServerListData extendedData = serverDataTag.get(serverEntry);

            if ("FML".equals(extendedData.type) && extendedData.isCompatible) {
                idx = 0;
                crucibleMode = extendedData.modData.containsKey("Crucible") ? (extendedData.modData.containsKey(Tags.MODID) ? 2 : 1) : 0;

                if (crucibleMode > 0) {
                    tooltip = String.format(
                            (ChatFormatting.DARK_GRAY + "Compatible %s " + ChatFormatting.DARK_GRAY + "modded server\n" + ChatFormatting.DARK_GRAY + ChatFormatting.BOLD + "%d" + ChatFormatting.DARK_GRAY + " Mods are present"),
                            (String.valueOf(crucibleMode == 1 ? ChatFormatting.GREEN : ChatFormatting.RED)) + ChatFormatting.BOLD + "Crucible" + ChatFormatting.RESET,
                            extendedData.modData.size());
                } else {
                    tooltip = String.format("Compatible FML modded server\n%d mods present", extendedData.modData.size());
                }

            } else if ("FML".equals(extendedData.type)) {
                idx = 16;
                tooltip = String.format("Incompatible FML modded server\n%d mods present", extendedData.modData.size());
            } else if ("BUKKIT".equals(extendedData.type)) {
                idx = 32;
                tooltip = "Bukkit modded server";
            } else if ("VANILLA".equals(extendedData.type)) {
                idx = 48;
                tooltip = "Vanilla server";
            } else {
                idx = 64;
                tooltip = "Unknown server data";
            }
            blocked = extendedData.isBlocked;

        } else {
            return null;
        }

        if (crucibleMode > 0) {
            this.client.getTextureManager().bindTexture(CrucibleServerListEntry.CRUCIBLE_ICONS);
            Gui.func_152125_a(x + width - 16, y + 10, 0, (float) (crucibleMode - 1) * 16, 16, 16, 12, 12, 256.0f, 256.0f);
        } else {
            this.client.getTextureManager().bindTexture(iconSheet);

            Gui.func_146110_a(x + width - 18, y + 10, 0, (float) idx, 16, 16, 256.0f, 256.0f);

            if (blocked) {
                Gui.func_146110_a(x + width - 18, y + 10, 0, 80, 16, 16, 256.0f, 256.0f);
            }
        }

        return relativeMouseX > width - 15 && relativeMouseX < width && relativeMouseY > 10 && relativeMouseY < 26 ? tooltip : null;
    }


}
