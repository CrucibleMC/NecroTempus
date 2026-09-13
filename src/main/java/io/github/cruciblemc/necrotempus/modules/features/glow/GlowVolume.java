package io.github.cruciblemc.necrotempus.modules.features.glow;

/** Client-side wireframe volume rendered into the glow mask. */
public class GlowVolume {

    public final double minX;
    public final double minY;
    public final double minZ;
    public final double maxX;
    public final double maxY;
    public final double maxZ;
    public final int rgb;
    public int remainingTicks;

    public GlowVolume(
        double minX,
        double minY,
        double minZ,
        double maxX,
        double maxY,
        double maxZ,
        int rgb,
        int durationTicks) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
        this.rgb = rgb;
        this.remainingTicks = durationTicks <= 0 ? GlowingEntityRegistry.INFINITE : durationTicks;
    }
}
