package net.vulkanmod.config.ui.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RoundedScanlineTest {
    @Test
    void zeroRadiusIsASquareRect() {
        int[] insets = RoundedScanline.insets(20, 6, 0);
        assertEquals(6, insets.length);
        for (int inset : insets) {
            assertEquals(0, inset);
        }
    }

    @Test
    void cornersInsetAndMiddleDoesNot() {
        int[] insets = RoundedScanline.insets(40, 12, 4);
        assertTrue(insets[0] > 0);
        assertTrue(insets[insets.length - 1] > 0);
        assertEquals(0, insets[6]);
    }

    @Test
    void insetsAreVerticallySymmetric() {
        int[] insets = RoundedScanline.insets(40, 13, 5);
        for (int row = 0; row < insets.length; row++) {
            assertEquals(insets[row], insets[insets.length - 1 - row]);
        }
    }

    @Test
    void radiusIsClampedToHalfTheShortestSide() {
        assertArrayEquals(RoundedScanline.insets(10, 10, 5), RoundedScanline.insets(10, 10, 99));
    }

    @Test
    void everyRowOfAClampedShapeStillDraws() {
        List<Rect> spans = RoundedScanline.fillSpans(new Rect(0, 0, 10, 10), 5);
        assertEquals(10, spans.size());
        for (Rect span : spans) {
            assertFalse(span.isEmpty());
        }
    }

    @Test
    void fillSpansCoverOneRowEachAndStayInsideTheRect() {
        Rect box = new Rect(7, 3, 30, 9);
        for (Rect span : RoundedScanline.fillSpans(box, 4)) {
            assertEquals(1, span.height());
            assertTrue(span.x() >= box.x());
            assertTrue(span.right() <= box.right());
            assertTrue(span.y() >= box.y() && span.y() < box.bottom());
        }
    }

    @Test
    void squareOutlineIsAHollowRing() {
        Rect box = new Rect(0, 0, 5, 4);
        boolean[][] painted = paint(box, RoundedScanline.outlineSpans(box, 0));
        for (int row = 0; row < 4; row++) {
            for (int column = 0; column < 5; column++) {
                boolean edge = row == 0 || row == 3 || column == 0 || column == 4;
                assertEquals(edge, painted[row][column]);
            }
        }
    }

    @Test
    void outlineIsContainedInTheFillAndLeavesTheCentreHollow() {
        Rect box = new Rect(0, 0, 24, 11);
        boolean[][] fill = paint(box, RoundedScanline.fillSpans(box, 4));
        boolean[][] outline = paint(box, RoundedScanline.outlineSpans(box, 4));
        for (int row = 0; row < box.height(); row++) {
            for (int column = 0; column < box.width(); column++) {
                if (outline[row][column]) {
                    assertTrue(fill[row][column]);
                }
            }
        }
        assertFalse(outline[5][12]);
    }

    @Test
    void emptyRectProducesNoSpans() {
        assertTrue(RoundedScanline.fillSpans(new Rect(0, 0, 0, 8), 3).isEmpty());
        assertTrue(RoundedScanline.outlineSpans(new Rect(0, 0, 8, 0), 3).isEmpty());
    }

    @Test
    void rejectsNegativeAndNullInputs() {
        assertThrows(IllegalArgumentException.class, () -> RoundedScanline.insets(-1, 4, 0));
        assertThrows(IllegalArgumentException.class, () -> RoundedScanline.insets(4, -1, 0));
        assertThrows(IllegalArgumentException.class, () -> RoundedScanline.insets(4, 4, -1));
        assertThrows(IllegalArgumentException.class, () -> RoundedScanline.fillSpans(null, 0));
        assertThrows(IllegalArgumentException.class, () -> RoundedScanline.outlineSpans(null, 0));
    }

    private static boolean[][] paint(Rect box, List<Rect> spans) {
        boolean[][] grid = new boolean[box.height()][box.width()];
        for (Rect span : spans) {
            for (int column = span.x(); column < span.right(); column++) {
                grid[span.y() - box.y()][column - box.x()] = true;
            }
        }
        return grid;
    }
}
