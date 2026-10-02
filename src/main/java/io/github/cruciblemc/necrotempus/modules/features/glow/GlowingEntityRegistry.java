package io.github.cruciblemc.necrotempus.modules.features.glow;

import java.util.HashMap;
import java.util.Map;

/**
 * Tracks glow state by entity id. Packet mutations are scheduled on the client thread;
 * entity resolution and colour fallback are handled by the render layer.
 */
public class GlowingEntityRegistry {

    public static final int INFINITE = -1;

    private final Map<Integer, GlowEntry> entries = new HashMap<>();
    private final Map<Integer, GlowVolume> volumes = new HashMap<>();

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
        volumes.clear();
    }

    public boolean isEmpty() {
        return entries.isEmpty() && volumes.isEmpty();
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

    public void setVolume(int volumeId, GlowVolume volume) {
        volumes.put(volumeId, volume);
    }

    public void removeVolume(int volumeId) {
        volumes.remove(volumeId);
    }

    public GlowVolume volumeFor(int volumeId) {
        return volumes.get(volumeId);
    }

    public GlowVolume[] glowingVolumes() {
        return volumes.values()
            .toArray(new GlowVolume[volumes.size()]);
    }

    public void tick() {
        entries.values()
            .removeIf(e -> {
                if (e.remainingTicks < 0) return false;
                return --e.remainingTicks <= 0;
            });
        volumes.values()
            .removeIf(v -> {
                if (v.remainingTicks < 0) return false;
                return --v.remainingTicks <= 0;
            });
    }
}
