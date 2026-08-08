package net.vulkanmod.config.ui.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class FocusTree {
    private final List<String> order = new ArrayList<>();
    private final Map<String, Boolean> enabled = new HashMap<>();
    private String focused;

    public void register(String id, boolean isEnabled) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("focus id must not be blank");
        }
        if (enabled.containsKey(id)) {
            throw new IllegalArgumentException("duplicate focus id " + id);
        }
        order.add(id);
        enabled.put(id, isEnabled);
    }

    public void setEnabled(String id, boolean isEnabled) {
        if (!enabled.containsKey(id)) {
            return;
        }
        enabled.put(id, isEnabled);
        if (!isEnabled && id.equals(focused)) {
            focused = null;
        }
    }

    public void clear() {
        order.clear();
        enabled.clear();
        focused = null;
    }

    public int size() {
        return order.size();
    }

    public String focused() {
        return focused;
    }

    public boolean focus(String id) {
        if (!Boolean.TRUE.equals(enabled.get(id))) {
            return false;
        }
        focused = id;
        return true;
    }

    public boolean apply(KeyAction action) {
        return switch (action) {
            case NEXT, DOWN -> step(1);
            case PREVIOUS, UP -> step(-1);
            case HOME -> jump(0, 1);
            case END -> jump(order.size() - 1, -1);
            default -> false;
        };
    }

    private boolean step(int direction) {
        if (order.isEmpty()) {
            return false;
        }
        int start = focused == null ? (direction > 0 ? -1 : 0) : order.indexOf(focused);
        for (int i = 1; i <= order.size(); i++) {
            int index = Math.floorMod(start + direction * i, order.size());
            String candidate = order.get(index);
            if (Boolean.TRUE.equals(enabled.get(candidate))) {
                focused = candidate;
                return true;
            }
        }
        return false;
    }

    private boolean jump(int from, int direction) {
        for (int index = from; index >= 0 && index < order.size(); index += direction) {
            String candidate = order.get(index);
            if (Boolean.TRUE.equals(enabled.get(candidate))) {
                boolean moved = !candidate.equals(focused);
                focused = candidate;
                return moved;
            }
        }
        return false;
    }
}
