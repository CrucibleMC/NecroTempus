package io.github.cruciblemc.necrotempus.modules.mixin.mixins.bukkit.player;

import io.github.cruciblemc.necrotempus.api.title.TitleComponent;
import io.github.cruciblemc.necrotempus.api.title.TitleElement;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.S2APacketParticles;
import net.minecraft.util.ChatComponentText;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.craftbukkit.CraftParticle;
import org.craftbukkit.EnumParticle;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Collections;
import java.util.HashSet;

@Mixin(value = org.bukkit.craftbukkit.v1_7_R4.entity.CraftPlayer.class, remap = false)
public abstract class CraftPlayer {

    @Shadow public abstract EntityPlayerMP getHandle();

    @Shadow public abstract Player getPlayer();

    @Shadow public abstract boolean equals(Object o);

    /**
     * Sends a title and a subtitle message to the player. If either of these
     * values are null, they will not be sent and the display will remain
     * unchanged. If they are empty strings, the display will be updated as
     * such. If the strings contain a new line, only the first line will be
     * sent. The titles will be displayed with the client's default timings.
     *
     * @param title Title text
     * @param subtitle Subtitle text
     * @deprecated API behavior subject to change
     */
    @Deprecated
    @Intrinsic
    public void sendTitle(String title, String subtitle){
        sendTitle(title, subtitle, 10, 70, 20);
    }

    /**
     * Sends a title and a subtitle message to the player. If either of these
     * values are null, they will not be sent and the display will remain
     * unchanged. If they are empty strings, the display will be updated as
     * such. If the strings contain a new line, only the first line will be
     * sent. All timings values may take a value of -1 to indicate that they
     * will use the last value sent (or the defaults if no title has been
     * displayed).
     *
     * @param title Title text
     * @param subtitle Subtitle text
     * @param fadeIn time in ticks for titles to fade in. Defaults to 10.
     * @param stay time in ticks for titles to stay. Defaults to 70.
     * @param fadeOut time in ticks for titles to fade out. Defaults to 20.
     */
    @Intrinsic
    public void sendTitle(String title, String subtitle, int fadeIn, int stay, int fadeOut){

        if(title == null && subtitle == null) return;

        TitleComponent titleComponent = new TitleComponent();

        if(title != null)
            titleComponent.addElement(TitleElement.titleOf(new ChatComponentText(title)));

        if(subtitle != null)
            titleComponent.addElement(TitleElement.subtitleOf(new ChatComponentText(subtitle)));

        titleComponent.setFadeIn(fadeIn * 50);
        titleComponent.setStay(stay * 50);
        titleComponent.setFadeOut(fadeOut * 50);

        TitleComponent.getTitleManager().set(new HashSet<>(Collections.singleton(getHandle().getUniqueID())), titleComponent);

    }

    /**
     * Resets the title displayed to the player. This will clear the displayed
     * title / subtitle and reset timings to their default values.
     */
    @Intrinsic
    public void resetTitle(){
        TitleComponent.getTitleManager().set(new HashSet<>(Collections.singleton(getHandle().getUniqueID())), new TitleComponent());
    }

    /**
     * Spawns the particle (the number of times specified by count)
     * at the target location.
     *
     * @param particle the particle to spawn
     * @param location the location to spawn at
     * @param count    the number of particles
     */

    @Intrinsic
    public void spawnParticle(Particle particle, Location location, int count) {
        spawnParticle(particle, location.getX(), location.getY(), location.getZ(), count);
    }

    /**
     * Spawns the particle (the number of times specified by count)
     * at the target location.
     *
     * @param particle the particle to spawn
     * @param x        the position on the x axis to spawn at
     * @param y        the position on the y axis to spawn at
     * @param z        the position on the z axis to spawn at
     * @param count    the number of particles
     */
    @Intrinsic
    public void spawnParticle(Particle particle, double x, double y, double z, int count) {
        spawnParticle(particle, x, y, z, count, (Object)null);
    }

    /**
     * Spawns the particle (the number of times specified by count)
     * at the target location.
     *
     * @param particle the particle to spawn
     * @param location the location to spawn at
     * @param count    the number of particles
     * @param data     the data to use for the particle or null,
     *                 the type of this depends on {@link Particle#getDataType()}
     */
    @Intrinsic
    public <T> void spawnParticle(Particle particle, Location location, int count, T data) {
        spawnParticle(particle, location.getX(), location.getY(), location.getZ(), count, data);
    }


    /**
     * Spawns the particle (the number of times specified by count)
     * at the target location.
     *
     * @param particle the particle to spawn
     * @param x        the position on the x axis to spawn at
     * @param y        the position on the y axis to spawn at
     * @param z        the position on the z axis to spawn at
     * @param count    the number of particles
     * @param data     the data to use for the particle or null,
     *                 the type of this depends on {@link Particle#getDataType()}
     */
    @Intrinsic
    public <T> void spawnParticle(Particle particle, double x, double y, double z, int count, T data) {
        spawnParticle(particle, x, y, z, count, 0.0D, 0.0D, 0.0D, data);
    }

    /**
     * Spawns the particle (the number of times specified by count)
     * at the target location. The position of each particle will be
     * randomized positively and negatively by the offset parameters
     * on each axis.
     *
     * @param particle the particle to spawn
     * @param location the location to spawn at
     * @param count    the number of particles
     * @param offsetX  the maximum random offset on the X axis
     * @param offsetY  the maximum random offset on the Y axis
     * @param offsetZ  the maximum random offset on the Z axis
     */
    @Intrinsic
    public void spawnParticle(Particle particle, Location location, int count, double offsetX, double offsetY, double offsetZ) {
        spawnParticle(particle, location.getX(), location.getY(), location.getZ(), count, offsetX, offsetY, offsetZ);
    }

    /**
     * Spawns the particle (the number of times specified by count)
     * at the target location. The position of each particle will be
     * randomized positively and negatively by the offset parameters
     * on each axis.
     *
     * @param particle the particle to spawn
     * @param x        the position on the x axis to spawn at
     * @param y        the position on the y axis to spawn at
     * @param z        the position on the z axis to spawn at
     * @param count    the number of particles
     * @param offsetX  the maximum random offset on the X axis
     * @param offsetY  the maximum random offset on the Y axis
     * @param offsetZ  the maximum random offset on the Z axis
     */
    @Intrinsic
    public void spawnParticle(Particle particle, double x, double y, double z, int count, double offsetX, double offsetY, double offsetZ) {
        spawnParticle(particle, x, y, z, count, offsetX, offsetY, offsetZ, (Object)null);
    }

    /**
     * Spawns the particle (the number of times specified by count)
     * at the target location. The position of each particle will be
     * randomized positively and negatively by the offset parameters
     * on each axis.
     *
     * @param particle the particle to spawn
     * @param location the location to spawn at
     * @param count    the number of particles
     * @param offsetX  the maximum random offset on the X axis
     * @param offsetY  the maximum random offset on the Y axis
     * @param offsetZ  the maximum random offset on the Z axis
     * @param data     the data to use for the particle or null,
     *                 the type of this depends on {@link Particle#getDataType()}
     */
    @Intrinsic
    public <T> void spawnParticle(Particle particle, Location location, int count, double offsetX, double offsetY, double offsetZ, T data) {
        spawnParticle(particle, location.getX(), location.getY(), location.getZ(), count, offsetX, offsetY, offsetZ, data);
    }

    /**
     * Spawns the particle (the number of times specified by count)
     * at the target location. The position of each particle will be
     * randomized positively and negatively by the offset parameters
     * on each axis.
     *
     * @param particle the particle to spawn
     * @param x        the position on the x axis to spawn at
     * @param y        the position on the y axis to spawn at
     * @param z        the position on the z axis to spawn at
     * @param count    the number of particles
     * @param offsetX  the maximum random offset on the X axis
     * @param offsetY  the maximum random offset on the Y axis
     * @param offsetZ  the maximum random offset on the Z axis
     * @param data     the data to use for the particle or null,
     *                 the type of this depends on {@link Particle#getDataType()}
     */
    @Intrinsic
    public <T> void spawnParticle(Particle particle, double x, double y, double z, int count, double offsetX, double offsetY, double offsetZ, T data) {
        spawnParticle(particle, x, y, z, count, offsetX, offsetY, offsetZ, 1.0D, data);
    }

    /**
     * Spawns the particle (the number of times specified by count)
     * at the target location. The position of each particle will be
     * randomized positively and negatively by the offset parameters
     * on each axis.
     *
     * @param particle the particle to spawn
     * @param location the location to spawn at
     * @param count    the number of particles
     * @param offsetX  the maximum random offset on the X axis
     * @param offsetY  the maximum random offset on the Y axis
     * @param offsetZ  the maximum random offset on the Z axis
     * @param extra    the extra data for this particle, depends on the
     *                 particle used (normally speed)
     */
    @Intrinsic
    public void spawnParticle(Particle particle, Location location, int count, double offsetX, double offsetY, double offsetZ, double extra) {
        spawnParticle(particle, location.getX(), location.getY(), location.getZ(), count, offsetX, offsetY, offsetZ, extra);
    }

    /**
     * Spawns the particle (the number of times specified by count)
     * at the target location. The position of each particle will be
     * randomized positively and negatively by the offset parameters
     * on each axis.
     *
     * @param particle the particle to spawn
     * @param x        the position on the x axis to spawn at
     * @param y        the position on the y axis to spawn at
     * @param z        the position on the z axis to spawn at
     * @param count    the number of particles
     * @param offsetX  the maximum random offset on the X axis
     * @param offsetY  the maximum random offset on the Y axis
     * @param offsetZ  the maximum random offset on the Z axis
     * @param extra    the extra data for this particle, depends on the
     *                 particle used (normally speed)
     */
    @Intrinsic
    public void spawnParticle(Particle particle, double x, double y, double z, int count, double offsetX, double offsetY, double offsetZ, double extra) {
        spawnParticle(particle, x, y, z, count, offsetX, offsetY, offsetZ, extra, (Object)null);
    }

    /**
     * Spawns the particle (the number of times specified by count)
     * at the target location. The position of each particle will be
     * randomized positively and negatively by the offset parameters
     * on each axis.
     *
     * @param particle the particle to spawn
     * @param location the location to spawn at
     * @param count    the number of particles
     * @param offsetX  the maximum random offset on the X axis
     * @param offsetY  the maximum random offset on the Y axis
     * @param offsetZ  the maximum random offset on the Z axis
     * @param extra    the extra data for this particle, depends on the
     *                 particle used (normally speed)
     * @param data     the data to use for the particle or null,
     *                 the type of this depends on {@link Particle#getDataType()}
     */
    @Intrinsic
    public <T> void spawnParticle(Particle particle, Location location, int count, double offsetX, double offsetY, double offsetZ, double extra, T data) {
        spawnParticle(particle, location.getX(), location.getY(), location.getZ(), count, offsetX, offsetY, offsetZ, extra, data);
    }

    /**
     * Spawns the particle (the number of times specified by count)
     * at the target location. The position of each particle will be
     * randomized positively and negatively by the offset parameters
     * on each axis.
     *
     * @param particle the particle to spawn
     * @param x        the position on the x axis to spawn at
     * @param y        the position on the y axis to spawn at
     * @param z        the position on the z axis to spawn at
     * @param count    the number of particles
     * @param offsetX  the maximum random offset on the X axis
     * @param offsetY  the maximum random offset on the Y axis
     * @param offsetZ  the maximum random offset on the Z axis
     * @param extra    the extra data for this particle, depends on the
     *                 particle used (normally speed)
     * @param data     the data to use for the particle or null,
     *                 the type of this depends on {@link Particle#getDataType()}
     */
    @Intrinsic
    public <T> void spawnParticle(Particle particle, double x, double y, double z, int count, double offsetX, double offsetY, double offsetZ, double extra, T data) {

        if (data != null && !particle.getDataType().isInstance(data))
            throw new IllegalArgumentException("data should be " + particle.getDataType() + " got " + data.getClass());

        try{

            EnumParticle enumParticle = CraftParticle.toNMS(particle);
            int[] data_arr = CraftParticle.toData(particle,data);
            String name = enumParticle.b();

            name = switch (enumParticle.d()){
                default -> name;
                case 1 -> name + data_arr[0] + "_0";
                case 2 -> name + data_arr[0] + "_" + data_arr[1];
            };

            S2APacketParticles s2APacketParticles = new S2APacketParticles(
                    name,
                    (float) x,
                    (float) y,
                    (float) z,
                    (float) offsetX,
                    (float) offsetY,
                    (float) offsetZ,
                    (float) extra,
                    count
            );

            getHandle().playerNetServerHandler.sendPacket(s2APacketParticles);

        }catch (Exception exception){
            exception.printStackTrace();
        }
    }


}