package io.github.cruciblemc.necrotempus.modules.mixin.plugin;

import com.google.common.io.Files;

import java.io.*;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public enum TargetedMod {

    //
    // IMPORTANT: Do not make any references to any mod from this file. This file is loaded quite early on and if
    // you refer to other mods you load them as well. The consequence is: You can't inject any previously loaded classes!
    // Exception: Tags.java, as long as it is used for Strings only!
    //

    VANILLA("Minecraft", "unused", true),
    BOTANIA("Botania", "botania", true, "botania"),
    CUSTOM_NPCS("CustomNPC", "CustomNPC", true, "customnpcs");

    public final String modName;
    public final String jarNamePrefixLowercase;
    // Optional dependencies can be omitted in development. Especially skipping GT5U will drastically speed up your game start!
    public final boolean loadInDevelopment;
    public final String[] modId;

    TargetedMod(String modName, String jarNamePrefix, boolean loadInDevelopment, String... modId) {
        this.modName = modName;
        this.jarNamePrefixLowercase = jarNamePrefix.toLowerCase();
        this.loadInDevelopment = loadInDevelopment;
        this.modId = modId;
    }

    public boolean isMatchingJar(Path path) {

        final String pathString = path.toString();
        final String nameLowerCase = Files.getNameWithoutExtension(pathString).toLowerCase();
        final String fileExtension = Files.getFileExtension(pathString);

        String modIdString = null;

        if (modId != null) {
            try {
                modIdString = getModID(path.toFile());
            } catch (Exception ignored) {
            }
        }

        return (nameLowerCase.startsWith(jarNamePrefixLowercase) || (modIdString != null && Arrays.asList(modId).contains(modIdString))) && "jar".equals(fileExtension);
    }

    private static String getModID(File file) throws IOException {

        try (ZipFile zipFile = new ZipFile(file)) {

            Enumeration<? extends ZipEntry> zipEntryEnumeration = zipFile.entries();

            while (zipEntryEnumeration.hasMoreElements()) {

                ZipEntry zipEntry = zipEntryEnumeration.nextElement();

                if (zipEntry != null && zipEntry.getName().equalsIgnoreCase("mcmod.info")) {

                    try (
                            InputStream inputStream = zipFile.getInputStream(zipEntry);
                            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))
                    ) {
                        String read;

                        while ((read = bufferedReader.readLine()) != null) {
                            if (read.contains("\"modid\"")) {
                                return read
                                        .replaceAll("\"", "")
                                        .replaceAll("modid", "")
                                        .replaceAll(":", "")
                                        .replaceAll(",", "")
                                        .replaceAll("\t", "")
                                        .replaceAll("\n", "")
                                        .replaceAll(" ", "")
                                        .replaceAll("\\{", "")
                                        .replaceAll("}", "");
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    @Override
    public String toString() {
        return "TargetedMod{" +
                "modName='" + modName + '\'' +
                ", jarNamePrefixLowercase='" + jarNamePrefixLowercase + '\'' +
                '}';
    }
}
