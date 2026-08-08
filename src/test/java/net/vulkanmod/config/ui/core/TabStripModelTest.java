package net.vulkanmod.config.ui.core;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class TabStripModelTest {

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
