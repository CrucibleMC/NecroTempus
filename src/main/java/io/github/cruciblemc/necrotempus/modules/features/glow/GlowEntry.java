package io.github.cruciblemc.necrotempus.modules.features.glow;

public class GlowEntry {

    /** 0xRRGGBB, or -1 for "no colour supplied". */
    public int rgb;

    /** Client ticks left before expiry; < 0 means infinite. */
    public int remainingTicks;

    public GlowEntry(int rgb, int remainingTicks) {
        this.rgb = rgb;
        this.remainingTicks = remainingTicks;
    }
}
