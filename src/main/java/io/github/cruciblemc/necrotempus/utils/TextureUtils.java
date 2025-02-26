package io.github.cruciblemc.necrotempus.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.IResource;
import net.minecraft.util.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class TextureUtils {
    public static BufferedImage getBufferedImageFromResource(ResourceLocation resource) {
        try {
            // Obtém o gerenciador de texturas
            TextureManager textureManager = Minecraft.getMinecraft().getTextureManager();

            // Obtém a textura associada ao ResourceLocation
            ITextureObject textureObject = textureManager.getTexture(resource);
            if (textureObject == null) {
                // Caso a textura não esteja carregada, tente obtê-la do sistema de recursos
                IResource iResource = Minecraft.getMinecraft().getResourceManager().getResource(resource);
                return ImageIO.read(iResource.getInputStream());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
