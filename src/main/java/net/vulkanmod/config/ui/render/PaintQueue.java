package net.vulkanmod.config.ui.render;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class PaintQueue {
    private final Map<PaintOp.Layer, List<PaintOp>> layers = new EnumMap<>(PaintOp.Layer.class);

    public PaintQueue() {
        for (PaintOp.Layer layer : PaintOp.Layer.values()) {
            layers.put(layer, new ArrayList<>());
        }
    }

    public void record(PaintOp.Layer layer, PaintOp op) {
        if (isDegenerate(op)) {
            return;
        }
        layers.get(layer).add(op);
    }

    public List<PaintOp> drain() {
        List<PaintOp> ordered = new ArrayList<>(size());
        for (PaintOp.Layer layer : PaintOp.Layer.values()) {
            ordered.addAll(layers.get(layer));
        }
        clear();
        return ordered;
    }

    public void clear() {
        for (List<PaintOp> ops : layers.values()) {
            ops.clear();
        }
    }

    public int size() {
        int total = 0;
        for (List<PaintOp> ops : layers.values()) {
            total += ops.size();
        }
        return total;
    }

    private static boolean isDegenerate(PaintOp op) {
        if (op instanceof PaintOp.Fill fill) {
            return fill.rect().isEmpty();
        }
        if (op instanceof PaintOp.RoundedSurface surface) {
            return surface.rect().isEmpty();
        }
        if (op instanceof PaintOp.Text text) {
            return text.value() == null || text.value().isEmpty();
        }
        return false;
    }
}
