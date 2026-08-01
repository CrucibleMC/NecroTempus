package io.github.cruciblemc.necrotempus.utils;

import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.IResource;
import net.minecraft.util.ResourceLocation;

public class TextureUtils {

    public static BufferedImage getBufferedImageFromResource(ResourceLocation resource) {
        try {
            TextureManager textureManager = Minecraft.getMinecraft()
                .getTextureManager();

            ITextureObject textureObject = textureManager.getTexture(resource);
            if (textureObject == null) {
                IResource iResource = Minecraft.getMinecraft()
                    .getResourceManager()
                    .getResource(resource);
                return ImageIO.read(iResource.getInputStream());
            }
        } catch (IOException ignored) {}
        return null;
    }
}
