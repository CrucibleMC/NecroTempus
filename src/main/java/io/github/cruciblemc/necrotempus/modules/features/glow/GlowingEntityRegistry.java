package io.github.cruciblemc.necrotempus.modules.features.glow;

import java.util.HashMap;
import java.util.Map;

/**
 * Minecraft-free registry of glowing entities keyed by entity id. Pure logic so it
 * is unit-testable. Entity resolution and colour fallback live in the render layer.
 * All access happens on the client thread (mutations scheduled there by the packet
 * handler; ticks and reads on the client/render thread).
 */
public class GlowingEntityRegistry {

    public static final int INFINITE = -1;

    private final Map<Integer, GlowEntry> entries = new HashMap<>();

    public void set(int entityId, int rgb, int durationTicks) {
        int remaining = durationTicks <= 0 ? INFINITE : durationTicks;
        GlowEntry existing = entries.get(entityId);
        if (existing != null) {
            existing.rgb = rgb;
            existing.remainingTicks = remaining;
        } else {
            entries.put(entityId, new GlowEntry(rgb, remaining));
        }
    }

    public void remove(int entityId) {
        entries.remove(entityId);
    }

    public void clear() {
        entries.clear();
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public Integer colorFor(int entityId) {
        GlowEntry e = entries.get(entityId);
        return e == null ? null : e.rgb;
    }

    public int[] glowingIds() {
        int[] ids = new int[entries.size()];
        int i = 0;
        for (Integer id : entries.keySet()) ids[i++] = id;
        return ids;
    }

    public void tick() {
        entries.values()
            .removeIf(e -> {
                if (e.remainingTicks < 0) return false; // infinite
                return --e.remainingTicks <= 0;
            });
    }
}
