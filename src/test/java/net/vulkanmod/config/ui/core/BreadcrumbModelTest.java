package net.vulkanmod.config.ui.core;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class BreadcrumbModelTest {

    @Test
    void segmentsAreSeparatedByAFixedAdvance() {
        List<Rect> boxes = BreadcrumbModel.layout(new int[] { 40, 30 }, 5, 8);
        assertEquals(new Rect(5, 8, 40, 9), boxes.get(0));
        assertEquals(new Rect(59, 8, 30, 9), boxes.get(1));
    }

    @Test
    void aSingleSegmentHasNoSeparator() {
        assertEquals(List.of(new Rect(0, 0, 40, 9)),
                BreadcrumbModel.layout(new int[] { 40 }, 0, 0));
    }

    @Test
    void emptyInputYieldsNoBoxes() {
        assertEquals(List.of(), BreadcrumbModel.layout(new int[0], 0, 0));
    }

    @Test
    void indexAtFindsTheSegmentAndTheGapYieldsMinusOne() {
        List<Rect> boxes = BreadcrumbModel.layout(new int[] { 40, 30 }, 5, 8);
        assertEquals(0, BreadcrumbModel.indexAt(boxes, 5, 8));
        assertEquals(1, BreadcrumbModel.indexAt(boxes, 59, 12));
        assertEquals(-1, BreadcrumbModel.indexAt(boxes, 50, 12));
    }

    @Test
    void negativeWidthIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> BreadcrumbModel.layout(new int[] { -1 }, 0, 0));
    }
}
