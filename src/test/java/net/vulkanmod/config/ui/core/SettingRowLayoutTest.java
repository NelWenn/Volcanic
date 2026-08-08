package net.vulkanmod.config.ui.core;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class SettingRowLayoutTest {
    private static final Rect CONTENT = new Rect(150, 32, 500, 400);
    private static final Breakpoint WIDE = Breakpoint.WIDE;

    @Test
    void rowsStackWithAConstantPitchInsideTheContentRect() {
        List<Rect> rows = SettingRowLayout.rows(CONTENT, 3, 0, WIDE);
        assertEquals(3, rows.size());
        assertTrue(rows.get(0).x() >= CONTENT.x());
        assertTrue(rows.get(0).right() <= CONTENT.right());
        int pitch = rows.get(1).y() - rows.get(0).y();
        assertEquals(pitch, rows.get(2).y() - rows.get(0).y() - pitch);
        assertTrue(pitch > rows.get(0).height());
    }

    @Test
    void rowsAreShortEnoughToReadAsChunkyPixelCards() {
        Rect row = SettingRowLayout.rows(CONTENT, 1, 0, WIDE).get(0);
        assertTrue(row.height() >= 24 && row.height() <= 28,
                "row height should be around 26-28, was " + row.height());
        assertTrue(SettingRowLayout.CARD_RADIUS >= 8,
                "card radius should be near 8, was " + SettingRowLayout.CARD_RADIUS);
        assertTrue(SettingRowLayout.CARD_RADIUS * 2 <= row.height());
    }

    @Test
    void theCardCornerStepsVisibly() {
        Rect card = SettingRowLayout.cardBox(SettingRowLayout.rows(CONTENT, 1, 0, WIDE).get(0));
        int[] insets = RoundedScanline.insets(card.width(), card.height(), SettingRowLayout.CARD_RADIUS);
        assertTrue(insets[0] >= 5, "top scanline should be inset by at least 5px, was " + insets[0]);
        assertTrue(insets[0] > insets[1] && insets[1] > insets[2],
                "the first scanlines must each step inwards");
    }

    @Test
    void compactRowsAreShorterAndTighterThanMedium() {
        List<Rect> compact = SettingRowLayout.rows(CONTENT, 2, 0, Breakpoint.COMPACT);
        List<Rect> medium = SettingRowLayout.rows(CONTENT, 2, 0, Breakpoint.MEDIUM);
        assertTrue(compact.get(0).height() < medium.get(0).height(),
                "compact rows must be shorter: " + compact.get(0).height() + " vs " + medium.get(0).height());

        int compactGap = compact.get(1).y() - compact.get(0).bottom();
        int mediumGap = medium.get(1).y() - medium.get(0).bottom();
        assertTrue(compactGap < mediumGap, "compact rows must be tighter: " + compactGap + " vs " + mediumGap);
        assertTrue(compactGap > 0);
    }

    @Test
    void mediumAndWideShareTheSameRowMetrics() {
        assertEquals(SettingRowLayout.rows(CONTENT, 2, 0, Breakpoint.MEDIUM),
                SettingRowLayout.rows(CONTENT, 2, 0, Breakpoint.WIDE));
    }

    @Test
    void scrollingShiftsEveryRowUpByTheSameAmount() {
        List<Rect> unscrolled = SettingRowLayout.rows(CONTENT, 3, 0, WIDE);
        List<Rect> scrolled = SettingRowLayout.rows(CONTENT, 3, 40, WIDE);
        for (int i = 0; i < 3; i++) {
            assertEquals(unscrolled.get(i).y() - 40, scrolled.get(i).y());
            assertEquals(unscrolled.get(i).x(), scrolled.get(i).x());
        }
    }

    @Test
    void noSettingsMeansNoRows() {
        assertTrue(SettingRowLayout.rows(CONTENT, 0, 0, WIDE).isEmpty());
        assertTrue(SettingRowLayout.rows(Rect.EMPTY, 3, 0, WIDE).isEmpty());
    }

    @Test
    void rejectsInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> SettingRowLayout.rows(null, 1, 0, WIDE));
        assertThrows(IllegalArgumentException.class, () -> SettingRowLayout.rows(CONTENT, -1, 0, WIDE));
        assertThrows(IllegalArgumentException.class, () -> SettingRowLayout.rows(CONTENT, 1, -1, WIDE));
        assertThrows(IllegalArgumentException.class, () -> SettingRowLayout.rows(CONTENT, 1, 0, null));
    }

    @Test
    void theResetButtonSitsOutsideTheCardOnItsRightWithAGap() {
        Rect row = SettingRowLayout.rows(CONTENT, 1, 0, WIDE).get(0);
        Rect card = SettingRowLayout.cardBox(row);
        Rect reset = SettingRowLayout.resetBox(row);

        assertFalse(reset.isEmpty());
        assertTrue(reset.x() > card.right(), "reset must start after the card: " + reset.x() + " vs " + card.right());
        assertEquals(SettingRowLayout.RESET_GAP, reset.x() - card.right());
        assertEquals(row.right(), reset.right());
        assertEquals(row.width() - reset.width() - SettingRowLayout.RESET_GAP, card.width());
    }

    @Test
    void theResetButtonIsAChunkySquare() {
        Rect reset = SettingRowLayout.resetBox(SettingRowLayout.rows(CONTENT, 1, 0, WIDE).get(0));
        assertEquals(reset.width(), reset.height());
        assertTrue(reset.width() >= 14 && reset.width() <= 16,
                "reset should be a 14-16px click target, was " + reset.width());
    }

    @Test
    void theResetButtonIsVerticallyCentredInTheRow() {
        Rect row = SettingRowLayout.rows(CONTENT, 1, 0, WIDE).get(0);
        Rect reset = SettingRowLayout.resetBox(row);
        assertTrue(Math.abs((reset.y() - row.y()) - (row.bottom() - reset.bottom())) <= 1);
        assertTrue(reset.y() >= row.y() && reset.bottom() <= row.bottom());
    }

    @Test
    void resetBoxFollowsTheRowAsItScrolls() {
        Rect unscrolled = SettingRowLayout.rows(CONTENT, 2, 0, WIDE).get(1);
        Rect scrolled = SettingRowLayout.rows(CONTENT, 2, 40, WIDE).get(1);
        assertEquals(SettingRowLayout.resetBox(unscrolled).y() - 40, SettingRowLayout.resetBox(scrolled).y());
        assertEquals(SettingRowLayout.resetBox(unscrolled).x(), SettingRowLayout.resetBox(scrolled).x());
    }

    @Test
    void resetBoxIsEmptyWhenTheRowCannotHoldIt() {
        assertEquals(Rect.EMPTY, SettingRowLayout.resetBox(Rect.EMPTY));
        assertEquals(Rect.EMPTY, SettingRowLayout.resetBox(new Rect(0, 0, 8, 27)));
        assertEquals(Rect.EMPTY, SettingRowLayout.resetBox(new Rect(0, 0, 300, 4)));
    }

    @Test
    void cardBoxIsEmptyWhenTheRowCannotHoldIt() {
        assertEquals(Rect.EMPTY, SettingRowLayout.cardBox(Rect.EMPTY));
        assertEquals(Rect.EMPTY, SettingRowLayout.cardBox(new Rect(0, 0, 8, 27)));
    }

    @Test
    void cardAndResetBoxRejectInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> SettingRowLayout.resetBox(null));
        assertThrows(IllegalArgumentException.class, () -> SettingRowLayout.cardBox(null));
    }

    @Test
    void noScrollWhenEveryRowFitsInTheContentRegion() {
        assertEquals(0, SettingRowLayout.maxScroll(CONTENT, 0, WIDE));
        assertEquals(0, SettingRowLayout.maxScroll(CONTENT, 1, WIDE));
        assertEquals(0, SettingRowLayout.maxScroll(Rect.EMPTY, 40, WIDE));
    }

    @Test
    void maxScrollIsExactlyTheOverflowBelowTheContentRegion() {
        Rect small = new Rect(150, 32, 500, 140);
        int max = SettingRowLayout.maxScroll(small, 6, WIDE);
        assertTrue(max > 0);

        List<Rect> rows = SettingRowLayout.rows(small, 6, max, WIDE);
        Rect last = rows.get(rows.size() - 1);
        assertTrue(last.bottom() <= small.bottom(), "the last row must be inside the region once scrolled fully");
        assertTrue(last.bottom() > small.bottom() - 20, "scrolling must not run past the last row");

        Rect firstAtRest = SettingRowLayout.rows(small, 6, 0, WIDE).get(0);
        assertTrue(SettingRowLayout.rows(small, 6, max, WIDE).get(0).y() < firstAtRest.y());
    }

    @Test
    void maxScrollGrowsWithTheRowCountAndShrinksWhenCompact() {
        Rect small = new Rect(150, 32, 500, 140);
        assertTrue(SettingRowLayout.maxScroll(small, 8, WIDE) > SettingRowLayout.maxScroll(small, 6, WIDE));
        assertTrue(SettingRowLayout.maxScroll(small, 8, Breakpoint.COMPACT)
                < SettingRowLayout.maxScroll(small, 8, WIDE));
    }

    @Test
    void clampScrollKeepsTheOffsetBetweenZeroAndMaxScroll() {
        Rect small = new Rect(150, 32, 500, 140);
        int max = SettingRowLayout.maxScroll(small, 6, WIDE);
        assertEquals(0, SettingRowLayout.clampScroll(-40, small, 6, WIDE));
        assertEquals(max, SettingRowLayout.clampScroll(max + 500, small, 6, WIDE));
        assertEquals(7, SettingRowLayout.clampScroll(7, small, 6, WIDE));
        assertEquals(0, SettingRowLayout.clampScroll(400, CONTENT, 1, WIDE));
    }

    @Test
    void scrollBoundsRejectInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> SettingRowLayout.maxScroll(null, 1, WIDE));
        assertThrows(IllegalArgumentException.class, () -> SettingRowLayout.maxScroll(CONTENT, -1, WIDE));
        assertThrows(IllegalArgumentException.class, () -> SettingRowLayout.maxScroll(CONTENT, 1, null));
        assertThrows(IllegalArgumentException.class, () -> SettingRowLayout.clampScroll(0, CONTENT, 1, null));
    }

    @Test
    void revealingARowThatIsAlreadyOnScreenLeavesTheScrollAlone() {
        Rect small = new Rect(150, 32, 500, 140);
        assertEquals(0, SettingRowLayout.scrollToReveal(small, 6, 0, 0, WIDE));
        for (int index = 0; index < 6; index++) {
            int scroll = SettingRowLayout.scrollToReveal(small, 6, index, 0, WIDE);
            assertEquals(scroll, SettingRowLayout.scrollToReveal(small, 6, index, scroll, WIDE),
                    "revealing row " + index + " a second time must not move it again");
        }
    }

    @Test
    void revealingARowBelowTheFoldScrollsUntilItIsFullyVisible() {
        Rect small = new Rect(150, 32, 500, 140);
        for (int index = 0; index < 6; index++) {
            int scroll = SettingRowLayout.scrollToReveal(small, 6, index, 0, WIDE);
            Rect row = SettingRowLayout.rows(small, 6, scroll, WIDE).get(index);
            assertTrue(row.y() >= small.y(), "row " + index + " above the region at scroll " + scroll);
            assertTrue(row.bottom() <= small.bottom(), "row " + index + " below the region at scroll " + scroll);
        }
    }

    @Test
    void revealingARowAboveTheFoldScrollsBackUp() {
        Rect small = new Rect(150, 32, 500, 140);
        int bottom = SettingRowLayout.maxScroll(small, 6, WIDE);
        assertEquals(0, SettingRowLayout.scrollToReveal(small, 6, 0, bottom, WIDE));

        int scroll = SettingRowLayout.scrollToReveal(small, 6, 2, bottom, WIDE);
        Rect row = SettingRowLayout.rows(small, 6, scroll, WIDE).get(2);
        assertTrue(row.y() >= small.y() && row.bottom() <= small.bottom());
    }

    @Test
    void revealingTheLastRowLandsExactlyAtMaxScroll() {
        Rect small = new Rect(150, 32, 500, 140);
        assertEquals(SettingRowLayout.maxScroll(small, 6, WIDE),
                SettingRowLayout.scrollToReveal(small, 6, 5, 0, WIDE));
    }

    @Test
    void revealingNeverLeavesTheScrollableRange() {
        Rect small = new Rect(150, 32, 500, 140);
        int max = SettingRowLayout.maxScroll(small, 9, WIDE);
        for (int index = 0; index < 9; index++) {
            for (int scroll = 0; scroll <= max; scroll += 13) {
                int result = SettingRowLayout.scrollToReveal(small, 9, index, scroll, WIDE);
                assertTrue(result >= 0 && result <= max,
                        "scroll " + result + " outside 0.." + max + " for row " + index);
            }
        }
    }

    @Test
    void revealingARowThatDoesNotExistOnlyClampsTheGivenScroll() {
        Rect small = new Rect(150, 32, 500, 140);
        int max = SettingRowLayout.maxScroll(small, 6, WIDE);
        assertEquals(max, SettingRowLayout.scrollToReveal(small, 6, 6, max + 400, WIDE));
        assertEquals(0, SettingRowLayout.scrollToReveal(small, 0, 0, 0, WIDE));
    }

    @Test
    void revealRejectsInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> SettingRowLayout.scrollToReveal(null, 6, 0, 0, WIDE));
        assertThrows(IllegalArgumentException.class, () -> SettingRowLayout.scrollToReveal(CONTENT, -1, 0, 0, WIDE));
        assertThrows(IllegalArgumentException.class, () -> SettingRowLayout.scrollToReveal(CONTENT, 6, 0, -1, WIDE));
        assertThrows(IllegalArgumentException.class, () -> SettingRowLayout.scrollToReveal(CONTENT, 6, 0, 0, null));
    }

    @Test
    void trackFillSpansTheRangeAndClampsOutsideIt() {
        assertEquals(0, SettingRowLayout.trackFill(60, 10, 10, 260));
        assertEquals(60, SettingRowLayout.trackFill(60, 260, 10, 260));
        assertEquals(30, SettingRowLayout.trackFill(60, 135, 10, 260));
        assertEquals(0, SettingRowLayout.trackFill(60, -400, 10, 260));
        assertEquals(60, SettingRowLayout.trackFill(60, 4000, 10, 260));
    }

    @Test
    void trackFillIsEmptyWhenTheRangeIsDegenerate() {
        assertEquals(0, SettingRowLayout.trackFill(60, 3, 0, 0));
    }

    @Test
    void trackFillRejectsInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> SettingRowLayout.trackFill(-1, 3, 0, 10));
        assertThrows(IllegalArgumentException.class, () -> SettingRowLayout.trackFill(60, 3, 10, 0));
    }
}
