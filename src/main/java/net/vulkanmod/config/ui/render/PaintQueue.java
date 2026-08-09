package net.vulkanmod.config.ui.render;

import net.vulkanmod.config.ui.core.Rect;

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
        List<PaintOp> ops = layers.get(layer);
        if (!ops.isEmpty() && op instanceof PaintOp.Fill next
                && ops.get(ops.size() - 1) instanceof PaintOp.Fill previous) {
            Rect run = joined(previous, next);
            if (run != null) {
                ops.set(ops.size() - 1, new PaintOp.Fill(run, previous.argb()));
                return;
            }
        }
        ops.add(op);
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

    private static Rect joined(PaintOp.Fill previous, PaintOp.Fill next) {
        if (previous.argb() != next.argb()) {
            return null;
        }
        Rect a = previous.rect();
        Rect b = next.rect();
        if (a.x() == b.x() && a.width() == b.width() && a.bottom() == b.y()) {
            return new Rect(a.x(), a.y(), a.width(), a.height() + b.height());
        }
        if (a.y() == b.y() && a.height() == b.height() && a.right() == b.x()) {
            return new Rect(a.x(), a.y(), a.width() + b.width(), a.height());
        }
        return null;
    }

    private static boolean isDegenerate(PaintOp op) {
        return switch (op) {
            case PaintOp.Fill(Rect rect, int ignoredArgb) -> rect.isEmpty();
            case PaintOp.Gradient(Rect rect, int ignoredTop, int ignoredBottom) -> rect.isEmpty();
            case PaintOp.Text text -> text.value() == null || text.value().isEmpty();
        };
    }
}
