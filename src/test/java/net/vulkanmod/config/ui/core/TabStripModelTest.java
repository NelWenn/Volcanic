package net.vulkanmod.config.ui.core;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class TabStripModelTest {

    private static List<Rect> sixTabs() {
        return TabStripModel.layout(new int[]{38, 43, 52, 35, 41, 48}, 14, 48);
    }

    @Test
    void aStripThatFitsIsNotScrolled() {
        assertEquals(0, TabStripModel.scrollToReveal(sixTabs(), 5, 14, 1000));
    }

    @Test
    void scrollingBringsAnOverflowingTabFullyIntoView() {
        List<Rect> boxes = sixTabs();
        int offset = TabStripModel.scrollToReveal(boxes, 5, 14, 300);
        assertTrue(offset > 0);
        Rect revealed = TabStripModel.shifted(boxes, offset).get(5);
        assertTrue(revealed.right() <= 300);
        assertTrue(revealed.x() >= 14);
    }

    @Test
    void scrollingBackToTheFirstTabReturnsToTheOrigin() {
        assertEquals(0, TabStripModel.scrollToReveal(sixTabs(), 0, 14, 300));
    }

    @Test
    void scrollNeverExceedsWhatTheContentNeeds() {
        List<Rect> boxes = sixTabs();
        int offset = TabStripModel.scrollToReveal(boxes, 5, 14, 300);
        int total = boxes.get(boxes.size() - 1).right() - 14;
        assertEquals(total - (300 - 14), offset);
    }

    @Test
    void shiftingKeepsHitTestingAndPaintingInAgreement() {
        List<Rect> boxes = sixTabs();
        List<Rect> shifted = TabStripModel.shifted(boxes, TabStripModel.scrollToReveal(boxes, 5, 14, 300));
        Rect target = shifted.get(5);
        assertEquals(5, TabStripModel.indexAt(shifted, target.x() + 1, target.y() + 1));
    }

    @Test
    void scrollIsZeroForAnEmptyStripOrAnIndexOutOfRange() {
        assertEquals(0, TabStripModel.scrollToReveal(List.of(), 0, 14, 300));
        assertEquals(0, TabStripModel.scrollToReveal(sixTabs(), -1, 14, 300));
        assertEquals(0, TabStripModel.scrollToReveal(sixTabs(), 99, 14, 300));
    }

    @Test
    void scrollRejectsAnInvertedViewportAndNullBoxes() {
        assertThrows(IllegalArgumentException.class, () -> TabStripModel.scrollToReveal(sixTabs(), 0, 300, 14));
        assertThrows(IllegalArgumentException.class, () -> TabStripModel.scrollToReveal(null, 0, 14, 300));
        assertThrows(IllegalArgumentException.class, () -> TabStripModel.shifted(null, 5));
    }

    @Test
    void boxesArePaddedAndSpaced() {
        List<Rect> boxes = TabStripModel.layout(new int[] { 30, 40 }, 10, 20);
        assertEquals(2, boxes.size());
        assertEquals(new Rect(10, 20, 48, 17), boxes.get(0));
        assertEquals(new Rect(63, 20, 58, 17), boxes.get(1));
    }

    @Test
    void emptyInputYieldsNoBoxes() {
        assertEquals(List.of(), TabStripModel.layout(new int[0], 0, 0));
    }

    @Test
    void indexAtFindsTheBoxUnderThePointer() {
        List<Rect> boxes = TabStripModel.layout(new int[] { 30, 40 }, 10, 20);
        assertEquals(0, TabStripModel.indexAt(boxes, 10, 20));
        assertEquals(0, TabStripModel.indexAt(boxes, 57, 36));
        assertEquals(1, TabStripModel.indexAt(boxes, 63, 20));
    }

    @Test
    void indexAtReturnsMinusOneInTheGapAndOutside() {
        List<Rect> boxes = TabStripModel.layout(new int[] { 30, 40 }, 10, 20);
        assertEquals(-1, TabStripModel.indexAt(boxes, 60, 25));
        assertEquals(-1, TabStripModel.indexAt(boxes, 9, 25));
        assertEquals(-1, TabStripModel.indexAt(boxes, 30, 19));
        assertEquals(-1, TabStripModel.indexAt(boxes, 30, 37));
    }

    @Test
    void zeroWidthLabelStillGetsItsPadding() {
        assertEquals(new Rect(0, 0, 18, 17), TabStripModel.layout(new int[] { 0 }, 0, 0).get(0));
    }

    @Test
    void negativeWidthIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> TabStripModel.layout(new int[] { -1 }, 0, 0));
    }
}
