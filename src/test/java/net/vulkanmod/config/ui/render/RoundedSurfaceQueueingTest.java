package net.vulkanmod.config.ui.render;

import net.vulkanmod.config.ui.core.Rect;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RoundedSurfaceQueueingTest {
    private static final Rect CARD = new Rect(20, 20, 200, 40);
    private static final Rect NEIGHBOUR = new Rect(20, 70, 200, 40);
    private static final Rect STRIP = new Rect(0, 0, 200, 1);
    private static final int FILL = 0xFF1B1B1F;
    private static final int BORDER = 0xFF3A3A42;
    private static final int GLOW = 0xFFFF5A1E;

    @Test
    void aFillRecordedAfterASurfaceInTheSameLayerDrainsAfterIt() {
        PaintQueue queue = new PaintQueue();
        queue.record(PaintOp.Layer.SURFACE,
                new PaintOp.RoundedSurface(CARD, 6, FILL, BORDER, GLOW, 24));
        queue.record(PaintOp.Layer.SURFACE, new PaintOp.Fill(STRIP, BORDER));

        List<PaintOp> drained = queue.drain();

        assertEquals(2, drained.size());
        assertInstanceOf(PaintOp.RoundedSurface.class, drained.get(0));
        assertInstanceOf(PaintOp.Fill.class, drained.get(1));
        assertEquals(CARD, ((PaintOp.RoundedSurface) drained.get(0)).rect());
        assertEquals(STRIP, ((PaintOp.Fill) drained.get(1)).rect());
    }

    @Test
    void aFillRecordedBeforeASurfaceInTheSameLayerDrainsBeforeIt() {
        PaintQueue queue = new PaintQueue();
        queue.record(PaintOp.Layer.SURFACE, new PaintOp.Fill(STRIP, BORDER));
        queue.record(PaintOp.Layer.SURFACE,
                new PaintOp.RoundedSurface(CARD, 6, FILL, BORDER, GLOW, 24));

        List<PaintOp> drained = queue.drain();

        assertEquals(2, drained.size());
        assertInstanceOf(PaintOp.Fill.class, drained.get(0));
        assertInstanceOf(PaintOp.RoundedSurface.class, drained.get(1));
    }

    @Test
    void consecutiveSurfacesInOneLayerDrainAsOneUninterruptedRun() {
        PaintQueue queue = new PaintQueue();
        queue.record(PaintOp.Layer.SURFACE,
                new PaintOp.RoundedSurface(CARD, 6, FILL, BORDER, GLOW, 24));
        queue.record(PaintOp.Layer.SURFACE,
                new PaintOp.RoundedSurface(NEIGHBOUR, 6, FILL, BORDER, GLOW, 24));
        queue.record(PaintOp.Layer.SURFACE, new PaintOp.Fill(STRIP, BORDER));

        List<PaintOp> drained = queue.drain();

        assertEquals(3, drained.size());
        assertEquals(CARD, ((PaintOp.RoundedSurface) drained.get(0)).rect());
        assertEquals(NEIGHBOUR, ((PaintOp.RoundedSurface) drained.get(1)).rect());
        assertInstanceOf(PaintOp.Fill.class, drained.get(2));
    }

    @Test
    void aSurfaceWithAnEmptyRectIsNeverRecorded() {
        PaintQueue queue = new PaintQueue();
        queue.record(PaintOp.Layer.SURFACE,
                new PaintOp.RoundedSurface(new Rect(0, 0, 0, 40), 6, FILL, BORDER, GLOW, 255));
        assertEquals(0, queue.size());

        queue.record(PaintOp.Layer.SURFACE,
                new PaintOp.RoundedSurface(new Rect(0, 0, 200, 0), 6, FILL, BORDER, GLOW, 255));
        assertEquals(0, queue.size());

        queue.record(PaintOp.Layer.SURFACE,
                new PaintOp.RoundedSurface(CARD, 6, FILL, BORDER, GLOW, 255));
        assertEquals(1, queue.size());
    }
}
