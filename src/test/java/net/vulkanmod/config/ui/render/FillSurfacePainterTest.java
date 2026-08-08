package net.vulkanmod.config.ui.render;

import net.vulkanmod.config.ui.core.Rect;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FillSurfacePainterTest {

    @Test
    void radiusZeroReturnsTheWholeRect() {
        Rect rect = new Rect(0, 0, 40, 20);
        assertEquals(List.of(rect), FillSurfacePainter.decomposeRoundedFill(rect, 0));
    }

    @Test
    void wideRectReturnsTheThreeStrips() {
        Rect rect = new Rect(0, 0, 500, 30);
        List<Rect> strips = FillSurfacePainter.decomposeRoundedFill(rect, 5);

        assertEquals(3, strips.size());
        assertEquals(new Rect(5, 0, 490, 30), strips.get(0));
        assertEquals(new Rect(0, 5, 5, 20), strips.get(1));
        assertEquals(new Rect(495, 5, 5, 20), strips.get(2));

        int combinedArea = strips.stream().mapToInt(r -> r.width() * r.height()).sum();
        assertEquals(500 * 30 - 4 * 5 * 5, combinedArea);
    }

    @Test
    void theThreeStripsNeverOverlap() {
        Rect rect = new Rect(0, 0, 500, 30);
        List<Rect> strips = FillSurfacePainter.decomposeRoundedFill(rect, 5);

        for (int i = 0; i < strips.size(); i++) {
            for (int j = i + 1; j < strips.size(); j++) {
                assertFalse(overlaps(strips.get(i), strips.get(j)));
            }
        }
    }

    @Test
    void squareAtTheClampBoundaryFallsBackToTheFullRect() {
        Rect rect = new Rect(0, 0, 10, 10);
        assertEquals(List.of(rect), FillSurfacePainter.decomposeRoundedFill(rect, 5));
    }

    @Test
    void switchGeometryFallsBackToTheFullRect() {
        Rect rect = new Rect(0, 0, 26, 12);
        assertEquals(List.of(rect), FillSurfacePainter.decomposeRoundedFill(rect, 11));
    }

    @Test
    void pillGeometryFallsBackToTheFullRect() {
        Rect rect = new Rect(0, 0, 60, 17);
        assertEquals(List.of(rect), FillSurfacePainter.decomposeRoundedFill(rect, 11));
    }

    @Test
    void lowCoverageSquareFallsBackToTheFullRect() {
        Rect rect = new Rect(0, 0, 20, 20);
        assertEquals(List.of(rect), FillSurfacePainter.decomposeRoundedFill(rect, 9));
    }

    @Test
    void radiusLargerThanHalfTheShorterSideIsClamped() {
        Rect rect = new Rect(0, 0, 10, 10);
        List<Rect> atBoundary = FillSurfacePainter.decomposeRoundedFill(rect, 5);
        List<Rect> huge = FillSurfacePainter.decomposeRoundedFill(rect, 1000);
        assertEquals(atBoundary, huge);
        assertEquals(List.of(rect), huge);
    }

    @Test
    void anEmptyRectReturnsAnEmptyList() {
        Rect rect = new Rect(0, 0, 0, 10);
        assertEquals(List.of(), FillSurfacePainter.decomposeRoundedFill(rect, 5));
    }

    @Test
    void noReturnedRectangleIsEverEmpty() {
        List<List<Rect>> cases = List.of(
                FillSurfacePainter.decomposeRoundedFill(new Rect(0, 0, 40, 20), 0),
                FillSurfacePainter.decomposeRoundedFill(new Rect(0, 0, 500, 30), 5),
                FillSurfacePainter.decomposeRoundedFill(new Rect(0, 0, 10, 10), 5),
                FillSurfacePainter.decomposeRoundedFill(new Rect(0, 0, 26, 12), 11),
                FillSurfacePainter.decomposeRoundedFill(new Rect(0, 0, 60, 17), 11),
                FillSurfacePainter.decomposeRoundedFill(new Rect(0, 0, 20, 20), 9),
                FillSurfacePainter.decomposeRoundedFill(new Rect(0, 0, 10, 10), 1000),
                FillSurfacePainter.decomposeRoundedFill(new Rect(0, 0, 0, 10), 5)
        );

        for (List<Rect> rects : cases) {
            for (Rect rect : rects) {
                assertFalse(rect.isEmpty());
            }
        }
    }

    private static boolean overlaps(Rect a, Rect b) {
        return a.x() < b.right() && b.x() < a.right() && a.y() < b.bottom() && b.y() < a.bottom();
    }
}
