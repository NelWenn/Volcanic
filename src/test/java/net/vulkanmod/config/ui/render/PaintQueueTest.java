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

        List<PaintOp> drained = queue.drain();
        assertEquals(2, drained.size());
        assertInstanceOf(PaintOp.Fill.class, drained.get(0));
        assertEquals(A, ((PaintOp.Fill) drained.get(0)).rect());
        assertInstanceOf(PaintOp.Text.class, drained.get(1));
    }

    @Test
    void surfaceAndTextAreTheOnlyLayers() {
        assertArrayEquals(new PaintOp.Layer[]{PaintOp.Layer.SURFACE, PaintOp.Layer.TEXT}, PaintOp.Layer.values());
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

    @Test
    void stackedRowsOfOneColourBecomeOneFill() {
        PaintQueue queue = new PaintQueue();
        for (int row = 0; row < 5; row++) {
            queue.record(PaintOp.Layer.SURFACE, new PaintOp.Fill(new Rect(4, 20 + row, 3, 1), 0xFF102030));
        }

        List<PaintOp> drained = queue.drain();
        assertEquals(1, drained.size());
        assertEquals(new Rect(4, 20, 3, 5), ((PaintOp.Fill) drained.get(0)).rect());
        assertEquals(0xFF102030, ((PaintOp.Fill) drained.get(0)).argb());
    }

    @Test
    void sideBySideRunsOfOneColourBecomeOneFill() {
        PaintQueue queue = new PaintQueue();
        for (int column = 0; column < 4; column++) {
            queue.record(PaintOp.Layer.SURFACE, new PaintOp.Fill(new Rect(7 + column, 2, 1, 1), 0xFF405060));
        }

        List<PaintOp> drained = queue.drain();
        assertEquals(1, drained.size());
        assertEquals(new Rect(7, 2, 4, 1), ((PaintOp.Fill) drained.get(0)).rect());
    }

    @Test
    void keepsRunsOfDifferentColoursApart() {
        PaintQueue queue = new PaintQueue();
        queue.record(PaintOp.Layer.SURFACE, new PaintOp.Fill(new Rect(0, 0, 3, 1), 0xFF000001));
        queue.record(PaintOp.Layer.SURFACE, new PaintOp.Fill(new Rect(0, 1, 3, 1), 0xFF000002));
        assertEquals(2, queue.drain().size());
    }

    @Test
    void keepsRowsWithDifferentWidthsApart() {
        PaintQueue queue = new PaintQueue();
        queue.record(PaintOp.Layer.SURFACE, new PaintOp.Fill(new Rect(0, 0, 3, 1), 0xFF000001));
        queue.record(PaintOp.Layer.SURFACE, new PaintOp.Fill(new Rect(0, 1, 4, 1), 0xFF000001));
        assertEquals(2, queue.drain().size());
    }

    @Test
    void keepsSeparatedRowsApart() {
        PaintQueue queue = new PaintQueue();
        queue.record(PaintOp.Layer.SURFACE, new PaintOp.Fill(new Rect(0, 0, 3, 1), 0xFF000001));
        queue.record(PaintOp.Layer.SURFACE, new PaintOp.Fill(new Rect(0, 2, 3, 1), 0xFF000001));
        assertEquals(2, queue.drain().size());
    }

    @Test
    void anOpInBetweenBreaksTheRun() {
        PaintQueue queue = new PaintQueue();
        queue.record(PaintOp.Layer.SURFACE, new PaintOp.Fill(new Rect(0, 0, 3, 1), 0xFF000001));
        queue.record(PaintOp.Layer.SURFACE, new PaintOp.Gradient(new Rect(0, 0, 3, 1), 1, 2));
        queue.record(PaintOp.Layer.SURFACE, new PaintOp.Fill(new Rect(0, 1, 3, 1), 0xFF000001));

        List<PaintOp> drained = queue.drain();
        assertEquals(3, drained.size());
        assertInstanceOf(PaintOp.Gradient.class, drained.get(1));
    }

    @Test
    void mergingIsReportedBySize() {
        PaintQueue queue = new PaintQueue();
        queue.record(PaintOp.Layer.SURFACE, new PaintOp.Fill(new Rect(0, 0, 3, 1), 0xFF000001));
        queue.record(PaintOp.Layer.SURFACE, new PaintOp.Fill(new Rect(0, 1, 3, 1), 0xFF000001));
        assertEquals(1, queue.size());
    }
}
