package net.vulkanmod.config.ui.render;

import net.vulkanmod.config.ui.core.Rect;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SurfacePainterContractTest {
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
        assertInstanceOf(PaintOp.RoundedSurface.class, drained.get(0));
        assertInstanceOf(PaintOp.RoundedSurface.class, drained.get(1));
        assertInstanceOf(PaintOp.Fill.class, drained.get(2));
        assertFalse(overlaps(((PaintOp.RoundedSurface) drained.get(0)).rect(),
                ((PaintOp.RoundedSurface) drained.get(1)).rect()));
    }

    @Test
    void ignoringGlowLeavesASurfaceIdenticalToAGlowlessOne() {
        PaintQueue glowing = new PaintQueue();
        PaintQueue plain = new PaintQueue();
        glowing.record(PaintOp.Layer.SURFACE,
                new PaintOp.RoundedSurface(CARD, 6, FILL, BORDER, GLOW, 24));
        plain.record(PaintOp.Layer.SURFACE,
                new PaintOp.RoundedSurface(CARD, 6, FILL, BORDER, 0, 0));

        List<PaintOp> glowingOps = glowing.drain();
        List<PaintOp> plainOps = plain.drain();

        assertEquals(1, glowingOps.size());
        assertEquals(1, plainOps.size());

        PaintOp.RoundedSurface withGlow = (PaintOp.RoundedSurface) glowingOps.get(0);
        assertEquals(plainOps.get(0), new PaintOp.RoundedSurface(withGlow.rect(), withGlow.radius(),
                withGlow.fillArgb(), withGlow.borderArgb(), 0, 0));
    }

    @Test
    void aGlowNeverSuppressesOrDuplicatesTheSurfaceItDecorates() {
        PaintQueue queue = new PaintQueue();
        queue.record(PaintOp.Layer.SURFACE,
                new PaintOp.RoundedSurface(CARD, 6, FILL, BORDER, GLOW, 255));
        assertEquals(1, queue.size());

        queue.clear();
        queue.record(PaintOp.Layer.SURFACE,
                new PaintOp.RoundedSurface(new Rect(0, 0, 0, 40), 6, FILL, BORDER, GLOW, 255));
        assertEquals(0, queue.size());
    }

    private static boolean overlaps(Rect a, Rect b) {
        return a.x() < b.right() && b.x() < a.right() && a.y() < b.bottom() && b.y() < a.bottom();
    }
}
