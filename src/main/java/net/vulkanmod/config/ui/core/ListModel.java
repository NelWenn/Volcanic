package net.vulkanmod.config.ui.core;

import java.util.ArrayList;
import java.util.List;

public final class ListModel {
    private final List<Integer> heights = new ArrayList<>();
    private final List<Integer> offsets = new ArrayList<>();
    private final int gap;
    private int totalHeight;

    public ListModel(int gap) {
        if (gap < 0) {
            throw new IllegalArgumentException("gap must not be negative");
        }
        this.gap = gap;
    }

    public int add(int height) {
        if (height < 0) {
            throw new IllegalArgumentException("height must not be negative");
        }
        int offset = heights.isEmpty() ? 0 : totalHeight + gap;
        offsets.add(offset);
        heights.add(height);
        totalHeight = offset + height;
        return heights.size() - 1;
    }

    public void clear() {
        heights.clear();
        offsets.clear();
        totalHeight = 0;
    }

    public int count() {
        return heights.size();
    }

    public int heightOf(int index) {
        return heights.get(index);
    }

    public int offsetOf(int index) {
        return offsets.get(index);
    }

    public int totalHeight() {
        return totalHeight;
    }

    public int maxScroll(int viewportHeight) {
        return Math.max(0, totalHeight - Math.max(0, viewportHeight));
    }

    public int clampScroll(int scroll, int viewportHeight) {
        return Math.min(Math.max(0, scroll), maxScroll(viewportHeight));
    }

    public int indexAt(int contentY) {
        if (contentY < 0 || contentY >= totalHeight) {
            return -1;
        }
        int low = 0;
        int high = heights.size() - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            int start = offsets.get(mid);
            int end = start + heights.get(mid);
            if (contentY < start) {
                high = mid - 1;
            } else if (contentY >= end) {
                low = mid + 1;
            } else {
                return mid;
            }
        }
        return -1;
    }

    public int firstVisible(int scroll) {
        if (heights.isEmpty()) {
            return -1;
        }
        for (int i = 0; i < heights.size(); i++) {
            if (offsets.get(i) + heights.get(i) > scroll) {
                return i;
            }
        }
        return heights.size() - 1;
    }

    public int lastVisible(int scroll, int viewportHeight) {
        if (heights.isEmpty()) {
            return -1;
        }
        int bottom = scroll + Math.max(0, viewportHeight);
        for (int i = heights.size() - 1; i >= 0; i--) {
            if (offsets.get(i) < bottom) {
                return i;
            }
        }
        return 0;
    }
}
