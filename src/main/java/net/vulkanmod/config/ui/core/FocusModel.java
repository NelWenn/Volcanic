package net.vulkanmod.config.ui.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FocusModel {
    private final List<String> regionOrder = new ArrayList<>();
    private final Map<String, FocusRing> rings = new LinkedHashMap<>();
    private String activeRegion;

    public void addRegion(String regionId) {
        if (regionId == null || regionId.isBlank()) {
            throw new IllegalArgumentException("region id must not be blank");
        }
        if (rings.containsKey(regionId)) {
            throw new IllegalArgumentException("duplicate region id " + regionId);
        }
        regionOrder.add(regionId);
        rings.put(regionId, new FocusRing());
    }

    public FocusRing ring(String regionId) {
        FocusRing ring = rings.get(regionId);
        if (ring == null) {
            throw new IllegalArgumentException("unknown region id " + regionId);
        }
        return ring;
    }

    public String activeRegion() {
        return activeRegion;
    }

    public boolean focusRegion(String regionId) {
        if (!rings.containsKey(regionId)) {
            return false;
        }
        activeRegion = regionId;
        return true;
    }

    public String focused() {
        return activeRegion == null ? null : rings.get(activeRegion).focused();
    }

    public int regionCount() {
        return regionOrder.size();
    }

    public void clear() {
        regionOrder.clear();
        rings.clear();
        activeRegion = null;
    }

    public boolean apply(KeyAction action) {
        return switch (action) {
            case NEXT -> stepRegion(1);
            case PREVIOUS -> stepRegion(-1);
            case UP, DOWN, HOME, END -> activeRegion != null && rings.get(activeRegion).apply(action);
            default -> false;
        };
    }

    private boolean stepRegion(int direction) {
        if (regionOrder.isEmpty()) {
            return false;
        }
        int size = regionOrder.size();
        int start = activeRegion == null ? (direction > 0 ? -1 : 0) : regionOrder.indexOf(activeRegion);
        int limit = activeRegion == null ? size : size - 1;
        for (int i = 1; i <= limit; i++) {
            int index = Math.floorMod(start + direction * i, size);
            String candidate = regionOrder.get(index);
            FocusRing candidateRing = rings.get(candidate);
            if (candidateRing.focused() == null) {
                candidateRing.apply(direction > 0 ? KeyAction.NEXT : KeyAction.END);
            }
            if (candidateRing.focused() != null) {
                activeRegion = candidate;
                return true;
            }
        }
        return false;
    }
}
