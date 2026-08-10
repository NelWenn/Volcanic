package net.vulkanmod.config.ui.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class HoverState {
    private final int durationMs;
    private final Map<String, Long> elapsed = new HashMap<>();
    private final Set<String> seen = new HashSet<>();

    private final boolean settleOnFirstSight;

    public HoverState(int durationMs) {
        this(durationMs, false);
    }

    public HoverState(int durationMs, boolean settleOnFirstSight) {
        if (durationMs <= 0) {
            throw new IllegalArgumentException("durationMs must be positive: " + durationMs);
        }
        this.durationMs = durationMs;
        this.settleOnFirstSight = settleOnFirstSight;
    }

    public float advance(String key, boolean active, long deltaMs) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("key must not be blank");
        }
        if (deltaMs < 0) {
            throw new IllegalArgumentException("deltaMs must not be negative: " + deltaMs);
        }

        long current = elapsed.containsKey(key) ? elapsed.get(key)
                : settleOnFirstSight && active ? durationMs : 0L;
        long next = Math.max(0L, Math.min(durationMs, active ? current + deltaMs : current - deltaMs));
        elapsed.put(key, next);
        seen.add(key);
        return Motion.ease(next, durationMs);
    }

    public void endFrame() {
        elapsed.keySet().retainAll(seen);
        seen.clear();
    }
}
