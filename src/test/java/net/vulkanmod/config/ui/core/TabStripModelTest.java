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

    private static final Rect BAND = new Rect(20, 40, 200, TabStripModel.HEIGHT);

    private static int[] widths(int count, int each) {
        int[] widths = new int[count];
        java.util.Arrays.fill(widths, each);
        return widths;
    }

    @Test
    void tabsThatFitGetNoArrowsAndStartWhereTheBandStarts() {
        TabStripModel.Strip strip = TabStripModel.strip(widths(3, 20), BAND, 0, 0);
        assertFalse(strip.scrollable());
        assertTrue(strip.prev().isEmpty());
        assertTrue(strip.next().isEmpty());
        assertEquals(BAND.x(), strip.boxes().get(0).x());
        assertEquals(0, strip.offset());
        assertTrue(strip.boxes().get(strip.boxes().size() - 1).right() <= BAND.right());
    }

    @Test
    void tabsThatOverflowGetArrowsOnBothEdgesInsideTheBand() {
        TabStripModel.Strip strip = TabStripModel.strip(widths(12, 40), BAND, 0, 0);
        assertTrue(strip.scrollable());
        assertEquals(BAND.x(), strip.prev().x());
        assertEquals(BAND.right(), strip.next().right());
        assertTrue(strip.viewport().x() >= strip.prev().right(),
                "the tabs must start clear of the left arrow");
        assertTrue(strip.viewport().right() <= strip.next().x(),
                "the tabs must stop clear of the right arrow");
    }

    @Test
    void scrollingRightMovesTheTabsLeftAndNeverPastTheLastOne() {
        TabStripModel.Strip start = TabStripModel.strip(widths(12, 40), BAND, 0, -1);
        TabStripModel.Strip moved = TabStripModel.strip(widths(12, 40), BAND, 60, -1);
        assertTrue(moved.boxes().get(0).x() < start.boxes().get(0).x());

        TabStripModel.Strip far = TabStripModel.strip(widths(12, 40), BAND, 100_000, -1);
        assertEquals(far.viewport().right(), far.boxes().get(far.boxes().size() - 1).right(),
                "the last tab must land flush with the right edge, never beyond it");
    }

    @Test
    void anOffsetCannotDragTheStripPastItsStart() {
        TabStripModel.Strip strip = TabStripModel.strip(widths(12, 40), BAND, -500, -1);
        assertEquals(0, strip.offset());
        assertEquals(strip.viewport().x(), strip.boxes().get(0).x());
    }

    @Test
    void revealingATabOnlyNudgesTheOffsetItDoesNotRecentreIt() {
        int[] widths = widths(12, 40);
        TabStripModel.Strip held = TabStripModel.strip(widths, BAND, 120, -1);
        TabStripModel.Strip visible = TabStripModel.strip(widths, BAND, 120, 3);
        assertEquals(held.offset(), visible.offset(),
                "a tab already on screen must not move the strip");

        TabStripModel.Strip revealed = TabStripModel.strip(widths, BAND, 0, 11);
        assertTrue(revealed.offset() > 0, "the last tab must be pulled into view");
        assertTrue(revealed.boxes().get(11).right() <= revealed.viewport().right());
    }

    @Test
    void anEmptyStripIsNotScrollableAndHasNoBoxes() {
        TabStripModel.Strip none = TabStripModel.strip(new int[0], BAND, 0, 0);
        assertTrue(none.boxes().isEmpty());
        assertFalse(none.scrollable());
        assertTrue(TabStripModel.strip(widths(3, 20), Rect.EMPTY, 0, 0).boxes().isEmpty());
    }

    @Test
    void anArrowStepLandsWholeTabsAgainstTheEdgeNeverHalfOfOne() {
        TabStripModel.Strip strip = TabStripModel.strip(widths(12, 40), BAND, 0, -1);
        int stepped = TabStripModel.stepOffset(strip, 1);
        assertTrue(stepped > 0);

        TabStripModel.Strip after = TabStripModel.strip(widths(12, 40), BAND, stepped, -1);
        Rect last = null;
        for (Rect box : after.boxes()) {
            if (box.right() <= after.viewport().right()) {
                last = box;
            }
        }
        assertEquals(after.viewport().right(), last.right(),
                "the step must leave a tab flush with the right edge");
    }

    @Test
    void steppingBackReturnsToTheStartAndStops() {
        int[] widths = widths(12, 40);
        TabStripModel.Strip strip = TabStripModel.strip(widths, BAND, 0, -1);
        int forward = TabStripModel.stepOffset(strip, 1);
        TabStripModel.Strip moved = TabStripModel.strip(widths, BAND, forward, -1);
        assertEquals(0, TabStripModel.stepOffset(moved, -1));

        TabStripModel.Strip home = TabStripModel.strip(widths, BAND, 0, -1);
        assertEquals(0, TabStripModel.stepOffset(home, -1), "there is nothing to the left of the start");
    }

    @Test
    void aStripThatFitsIgnoresItsArrowsAndShowsEveryTab() {
        TabStripModel.Strip strip = TabStripModel.strip(widths(3, 20), BAND, 0, 0);
        assertEquals(0, TabStripModel.stepOffset(strip, 1));
        assertEquals(0, TabStripModel.stepOffset(strip, -1));
        for (Rect box : strip.boxes()) {
            assertTrue(TabStripModel.fullyVisible(strip, box));
        }
    }

    @Test
    void aTabHangingOverTheViewportEdgeCountsAsHidden() {
        TabStripModel.Strip strip = TabStripModel.strip(widths(12, 40), BAND, 0, -1);
        boolean anyHidden = false;
        for (Rect box : strip.boxes()) {
            if (!TabStripModel.fullyVisible(strip, box)) {
                anyHidden = true;
                assertTrue(box.right() > strip.viewport().right() || box.x() < strip.viewport().x());
            }
        }
        assertTrue(anyHidden, "an overflowing strip must hide something");
    }
}
