package net.vulkanmod.config.ui.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ListModelTest {

    private static ListModel threeCards() {
        ListModel model = new ListModel(6);
        model.add(30);
        model.add(44);
        model.add(30);
        return model;
    }

    @Test
    void offsetsAccumulateWithGaps() {
        ListModel model = threeCards();
        assertEquals(0, model.offsetOf(0));
        assertEquals(36, model.offsetOf(1));
        assertEquals(86, model.offsetOf(2));
    }

    @Test
    void totalHeightExcludesTrailingGap() {
        assertEquals(116, threeCards().totalHeight());
    }

    @Test
    void emptyModelHasNoHeight() {
        assertEquals(0, new ListModel(6).totalHeight());
        assertEquals(0, new ListModel(6).count());
    }

    @Test
    void indexAtFindsEntries() {
        ListModel model = threeCards();
        assertEquals(0, model.indexAt(0));
        assertEquals(0, model.indexAt(29));
        assertEquals(1, model.indexAt(36));
        assertEquals(2, model.indexAt(115));
    }

    @Test
    void indexAtReturnsMinusOneInGapsAndOutOfRange() {
        ListModel model = threeCards();
        assertEquals(-1, model.indexAt(30));
        assertEquals(-1, model.indexAt(35));
        assertEquals(-1, model.indexAt(-1));
        assertEquals(-1, model.indexAt(116));
    }

    @Test
    void maxScrollIsZeroWhenContentFits() {
        assertEquals(0, threeCards().maxScroll(400));
    }

    @Test
    void maxScrollIsOverflow() {
        assertEquals(16, threeCards().maxScroll(100));
    }

    @Test
    void scrollClampsAtBothEnds() {
        ListModel model = threeCards();
        assertEquals(0, model.clampScroll(-50, 100));
        assertEquals(16, model.clampScroll(9999, 100));
        assertEquals(10, model.clampScroll(10, 100));
        assertEquals(0, model.clampScroll(40, 400));
    }

    @Test
    void visibleRangeSkipsScrolledOutEntries() {
        ListModel model = threeCards();
        assertEquals(0, model.firstVisible(0));
        assertEquals(1, model.firstVisible(36));
        assertEquals(2, model.firstVisible(90));
    }

    @Test
    void lastVisibleCoversThePartiallyShownEntry() {
        ListModel model = threeCards();
        assertEquals(0, model.lastVisible(0, 20));
        assertEquals(1, model.lastVisible(0, 40));
        assertEquals(2, model.lastVisible(0, 200));
    }

    @Test
    void clearResetsEverything() {
        ListModel model = threeCards();
        model.clear();
        assertEquals(0, model.count());
        assertEquals(0, model.totalHeight());
        assertEquals(-1, model.indexAt(0));
    }

    @Test
    void rejectsNegativeHeight() {
        assertThrows(IllegalArgumentException.class, () -> new ListModel(6).add(-1));
    }

    @Test
    void rejectsOutOfRangeIndex() {
        ListModel model = threeCards();
        assertThrows(IndexOutOfBoundsException.class, () -> model.offsetOf(3));
        assertThrows(IndexOutOfBoundsException.class, () -> model.heightOf(-1));
    }
}
