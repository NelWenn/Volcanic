package net.vulkanmod.config.ui.core;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompatPageLayoutTest {
    private static final Rect CONTENT = new Rect(120, 40, 300, 200);

    private static List<Integer> counts(int... values) {
        List<Integer> counts = new ArrayList<>(values.length);
        for (int value : values) {
            counts.add(value);
        }
        return counts;
    }

    private static List<Rect> allRows(CompatPageLayout.Page page) {
        List<Rect> rows = new ArrayList<>();
        for (PluginPageLayout.Block block : page.blocks()) {
            rows.addAll(block.rows());
        }
        return rows;
    }

    @Test
    void everySectionGetsItsHeadingRuleAndExactlyTheRowsItAskedFor() {
        CompatPageLayout.Page page = CompatPageLayout.page(CONTENT, counts(3, 2, 4), 0);
        assertEquals(3, page.blocks().size());
        assertEquals(3, page.blocks().get(0).rows().size());
        assertEquals(2, page.blocks().get(1).rows().size());
        assertEquals(4, page.blocks().get(2).rows().size());
        for (PluginPageLayout.Block block : page.blocks()) {
            assertFalse(block.heading().isEmpty(), "a section without a heading cannot be named");
            assertFalse(block.rule().isEmpty());
            assertFalse(block.count().isEmpty());
        }
    }

    @Test
    void rowsNeverOverlapAndAlwaysRunDownThePage() {
        List<Rect> rows = allRows(CompatPageLayout.page(CONTENT, counts(2, 3, 1), 0));
        assertEquals(6, rows.size());
        for (int index = 1; index < rows.size(); index++) {
            assertTrue(rows.get(index).y() >= rows.get(index - 1).bottom(),
                    "row " + index + " climbed back over the one before it");
        }
    }

    @Test
    void aSectionAlwaysStartsBelowTheOneAboveIt() {
        CompatPageLayout.Page page = CompatPageLayout.page(CONTENT, counts(2, 2), 0);
        Rect lastOfFirst = page.blocks().get(0).rows().get(1);
        assertTrue(page.blocks().get(1).heading().y() >= lastOfFirst.bottom(),
                "the second heading landed inside the first section");
    }

    @Test
    void anEmptySectionTakesNoRoomAtAllAndTheNextOneClosesTheGap() {
        int packed = CompatPageLayout.contentHeight(counts(2, 3));
        assertEquals(packed, CompatPageLayout.contentHeight(counts(2, 0, 3)),
                "a section with nothing in it must not reserve a heading");
        CompatPageLayout.Page page = CompatPageLayout.page(CONTENT, counts(2, 0, 3), 0);
        assertEquals(3, page.blocks().size(), "the empty section keeps its slot so indices still line up");
        assertTrue(page.blocks().get(1).rows().isEmpty());
        assertTrue(page.blocks().get(1).heading().isEmpty());
        assertEquals(5, allRows(page).size());
    }

    @Test
    void thePageIsExactlyAsTallAsItsLastRowPlusTheBottomMargin() {
        List<Integer> counts = counts(3, 2);
        CompatPageLayout.Page page = CompatPageLayout.page(CONTENT, counts, 0);
        List<Rect> rows = allRows(page);
        Rect last = rows.get(rows.size() - 1);
        assertEquals(last.bottom() - CONTENT.y() + PluginPageLayout.BOTTOM,
                CompatPageLayout.contentHeight(counts));
        assertEquals(CompatPageLayout.contentHeight(counts), page.height());
    }

    @Test
    void scrollingMovesTheWholePageAndNothingElse() {
        CompatPageLayout.Page still = CompatPageLayout.page(CONTENT, counts(4, 2), 0);
        CompatPageLayout.Page moved = CompatPageLayout.page(CONTENT, counts(4, 2), 30);
        List<Rect> before = allRows(still);
        List<Rect> after = allRows(moved);
        assertEquals(before.size(), after.size());
        for (int index = 0; index < before.size(); index++) {
            assertEquals(before.get(index).translated(0, -30), after.get(index));
        }
        assertEquals(still.height(), moved.height(), "scrolling must not change how tall the page is");
    }

    @Test
    void withNothingToReportThePageShowsOneCentredCardInstead() {
        CompatPageLayout.Page page = CompatPageLayout.page(CONTENT, counts(0, 0, 0), 0);
        assertTrue(page.blocks().isEmpty());
        assertFalse(page.empty().isEmpty());
        assertEquals(PluginPageLayout.EMPTY_H, page.empty().height());
        assertTrue(page.empty().x() >= CONTENT.x());
        assertTrue(page.empty().right() <= CONTENT.right());
        assertEquals(PluginPageLayout.EMPTY_H + PluginPageLayout.BOTTOM, page.height());
        assertEquals(page.height(), CompatPageLayout.contentHeight(List.of()));
    }

    @Test
    void everyRowStaysInsideTheContentColumn() {
        for (Rect row : allRows(CompatPageLayout.page(CONTENT, counts(2, 2), 0))) {
            assertTrue(row.x() >= CONTENT.x() + PluginPageLayout.PAD_X);
            assertTrue(row.right() <= CONTENT.right() - PluginPageLayout.PAD_X);
            assertEquals(CompatPageLayout.ROW_H, row.height());
        }
    }

    @Test
    void aRowIsTallEnoughToCarryBothItsNameAndItsNote() {
        Rect row = allRows(CompatPageLayout.page(CONTENT, counts(1), 0)).get(0);
        PluginPageLayout.Slots slots = PluginPageLayout.slots(row, false);
        assertFalse(slots.name().isEmpty());
        assertFalse(slots.note().isEmpty(), "the second line is where the explanation goes");
        assertFalse(slots.state().isEmpty());
        assertTrue(slots.note().y() >= slots.name().bottom());
    }

    @Test
    void aPageThatFitsNeedsNoScrollAndATallOneScrollsExactlyTheOverflow() {
        assertEquals(0, CompatPageLayout.maxScroll(CONTENT, counts(1)));
        List<Integer> many = counts(20, 20);
        assertEquals(CompatPageLayout.contentHeight(many) - CONTENT.height(),
                CompatPageLayout.maxScroll(CONTENT, many));
    }

    @Test
    void aColumnWithNoRoomDrawsNothingRatherThanNegativeRectangles() {
        CompatPageLayout.Page none = CompatPageLayout.page(new Rect(0, 0, 0, 0), counts(3), 0);
        assertTrue(none.blocks().isEmpty());
        assertTrue(none.empty().isEmpty());
        assertEquals(0, none.height());
        assertTrue(CompatPageLayout.page(new Rect(0, 0, PluginPageLayout.PAD_X * 2, 80), counts(3), 0)
                .blocks().isEmpty(), "a column narrower than its own padding has no usable width");
    }

    @Test
    void badInputIsAProgrammingErrorRatherThanASilentNoOp() {
        assertThrows(IllegalArgumentException.class,
                () -> CompatPageLayout.page(null, counts(1), 0));
        assertThrows(IllegalArgumentException.class,
                () -> CompatPageLayout.page(CONTENT, null, 0));
        assertThrows(IllegalArgumentException.class,
                () -> CompatPageLayout.page(CONTENT, counts(1), -1));
        assertThrows(IllegalArgumentException.class,
                () -> CompatPageLayout.page(CONTENT, counts(2, -1), 0));
        assertThrows(IllegalArgumentException.class,
                () -> CompatPageLayout.contentHeight(Arrays.asList(1, null)));
        assertThrows(IllegalArgumentException.class,
                () -> CompatPageLayout.maxScroll(null, counts(1)));
    }
}
