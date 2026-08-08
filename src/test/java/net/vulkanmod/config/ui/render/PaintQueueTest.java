package net.vulkanmod.config.ui.render;

import net.vulkanmod.config.ui.core.Rect;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PaintQueueTest {
    private static final Rect A = new Rect(0, 0, 10, 10);
    private static final Rect B = new Rect(10, 10, 10, 10);

    @Test
    void emitsLayersInDeclaredOrder() {
        PaintQueue queue = new PaintQueue();
        queue.record(PaintOp.Layer.TEXT, new PaintOp.Text(0, 0, "late", 0xFFFFFFFF, false));
        queue.record(PaintOp.Layer.SURFACE, new PaintOp.Fill(A, 0xFF000000));
        queue.record(PaintOp.Layer.BORDER, new PaintOp.Fill(B, 0xFF111111));

        List<PaintOp> drained = queue.drain();
        assertEquals(3, drained.size());
        assertInstanceOf(PaintOp.Fill.class, drained.get(0));
        assertEquals(A, ((PaintOp.Fill) drained.get(0)).rect());
        assertEquals(B, ((PaintOp.Fill) drained.get(1)).rect());
        assertInstanceOf(PaintOp.Text.class, drained.get(2));
    }

    @Test
    void preservesInsertionOrderWithinALayer() {
        PaintQueue queue = new PaintQueue();
        queue.record(PaintOp.Layer.SURFACE, new PaintOp.Fill(A, 0xFF000001));
        queue.record(PaintOp.Layer.SURFACE, new PaintOp.Fill(B, 0xFF000002));

        List<PaintOp> drained = queue.drain();
        assertEquals(0xFF000001, ((PaintOp.Fill) drained.get(0)).argb());
        assertEquals(0xFF000002, ((PaintOp.Fill) drained.get(1)).argb());
    }

    @Test
    void glowSitsBetweenBorderAndText() {
        PaintQueue queue = new PaintQueue();
        queue.record(PaintOp.Layer.TEXT, new PaintOp.Text(0, 0, "t", 0xFFFFFFFF, false));
        queue.record(PaintOp.Layer.GLOW, new PaintOp.Fill(A, 0xFF222222));
        queue.record(PaintOp.Layer.BORDER, new PaintOp.Fill(B, 0xFF333333));

        List<PaintOp> drained = queue.drain();
        assertEquals(0xFF333333, ((PaintOp.Fill) drained.get(0)).argb());
        assertEquals(0xFF222222, ((PaintOp.Fill) drained.get(1)).argb());
        assertInstanceOf(PaintOp.Text.class, drained.get(2));
    }

    @Test
    void drainEmptiesTheQueue() {
        PaintQueue queue = new PaintQueue();
        queue.record(PaintOp.Layer.SURFACE, new PaintOp.Fill(A, 0xFF000000));
        assertEquals(1, queue.size());
        queue.drain();
        assertEquals(0, queue.size());
        assertEquals(List.of(), queue.drain());
    }

    @Test
    void clearDiscardsWithoutEmitting() {
        PaintQueue queue = new PaintQueue();
        queue.record(PaintOp.Layer.SURFACE, new PaintOp.Fill(A, 0xFF000000));
        queue.clear();
        assertEquals(0, queue.size());
    }

    @Test
    void skipsEmptyRectangles() {
        PaintQueue queue = new PaintQueue();
        queue.record(PaintOp.Layer.SURFACE, new PaintOp.Fill(new Rect(0, 0, 0, 10), 0xFF000000));
        assertEquals(0, queue.size());
    }

    @Test
    void gradientsRecordOnTheGivenLayer() {
        PaintQueue queue = new PaintQueue();
        queue.record(PaintOp.Layer.SURFACE, new PaintOp.Gradient(A, 0xFF000001, 0xFF000002));
        assertEquals(1, queue.size());
        assertInstanceOf(PaintOp.Gradient.class, queue.drain().get(0));
    }

    @Test
    void skipsEmptyGradients() {
        PaintQueue queue = new PaintQueue();
        queue.record(PaintOp.Layer.SURFACE, new PaintOp.Gradient(new Rect(0, 0, 0, 10), 1, 2));
        assertEquals(0, queue.size());
    }
}
