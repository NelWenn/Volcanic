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
        assertEquals(row.right(), reset.right());
    }

    @Test
    void theStarSitsInTheGutterBetweenTheCardAndTheReset() {
        Rect row = SettingRowLayout.rows(CONTENT, 1, 0, WIDE).get(0);
        Rect card = SettingRowLayout.cardBox(row);
        Rect star = SettingRowLayout.starBox(row);
        Rect reset = SettingRowLayout.resetBox(row);

        assertFalse(star.isEmpty());
        assertEquals(SettingRowLayout.RESET_GAP, star.x() - card.right());
        assertTrue(star.right() < reset.x(),
                "the star must not overlap the reset: " + star.right() + " vs " + reset.x());
        assertTrue(reset.x() - star.right() > 0, "the two controls need a gap between them");
        assertEquals(row.right(), reset.right());
    }

    @Test
    void theGutterIsWideEnoughForBothControls() {
        Rect row = SettingRowLayout.rows(CONTENT, 1, 0, WIDE).get(0);
        Rect card = SettingRowLayout.cardBox(row);
        Rect star = SettingRowLayout.starBox(row);
        Rect reset = SettingRowLayout.resetBox(row);

        assertEquals(row.width(), card.width() + (star.x() - card.right()) + star.width()
                + (reset.x() - star.right()) + reset.width());
    }

    @Test
    void theStarIsAChunkySquareVerticallyCentredInTheRow() {
        Rect row = SettingRowLayout.rows(CONTENT, 1, 0, WIDE).get(0);
        Rect star = SettingRowLayout.starBox(row);

        assertEquals(star.width(), star.height());
        assertTrue(star.width() >= 14 && star.width() <= 16,
                "star should be a 14-16px click target, was " + star.width());
        assertTrue(Math.abs((star.y() - row.y()) - (row.bottom() - star.bottom())) <= 1);
        assertTrue(star.y() >= row.y() && star.bottom() <= row.bottom());
    }

    @Test
    void aCompactRowStillHoldsBothControlsSideBySide() {
        Rect row = SettingRowLayout.rows(CONTENT, 1, 0, Breakpoint.COMPACT).get(0);
        Rect card = SettingRowLayout.cardBox(row);
        Rect star = SettingRowLayout.starBox(row);
        Rect reset = SettingRowLayout.resetBox(row);

        assertFalse(card.isEmpty());
        assertFalse(star.isEmpty());
        assertFalse(reset.isEmpty());
        assertTrue(card.right() < star.x() && star.right() < reset.x());
        assertEquals(row.right(), reset.right());
        assertTrue(star.y() >= row.y() && star.bottom() <= row.bottom(),
                "the star must fit the 22px compact row: " + star + " in " + row);
        assertTrue(reset.y() >= row.y() && reset.bottom() <= row.bottom());
    }

    @Test
    void theNarrowestCompactWindowStillLeavesRoomForTheCard() {
        Rect narrow = new Rect(0, 32, 320, 200);
        Rect row = SettingRowLayout.rows(narrow, 1, 0, Breakpoint.COMPACT).get(0);
        Rect card = SettingRowLayout.cardBox(row);

        assertFalse(card.isEmpty());
        assertTrue(card.width() > row.width() / 2,
                "the gutter must not eat the card at 320px: card " + card.width() + " of row " + row.width());
        assertTrue(card.right() < SettingRowLayout.starBox(row).x());
    }

    @Test
    void starBoxFollowsTheRowAsItScrolls() {
        Rect unscrolled = SettingRowLayout.rows(CONTENT, 2, 0, WIDE).get(1);
        Rect scrolled = SettingRowLayout.rows(CONTENT, 2, 40, WIDE).get(1);
        assertEquals(SettingRowLayout.starBox(unscrolled).y() - 40, SettingRowLayout.starBox(scrolled).y());
        assertEquals(SettingRowLayout.starBox(unscrolled).x(), SettingRowLayout.starBox(scrolled).x());
    }

    @Test
    void starBoxIsEmptyWhenTheRowCannotHoldIt() {
        assertEquals(Rect.EMPTY, SettingRowLayout.starBox(Rect.EMPTY));
        assertEquals(Rect.EMPTY, SettingRowLayout.starBox(new Rect(0, 0, 8, 27)));
        assertEquals(Rect.EMPTY, SettingRowLayout.starBox(new Rect(0, 0, 300, 4)));
        assertThrows(IllegalArgumentException.class, () -> SettingRowLayout.starBox(null));
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
    void theBadgeSitsAfterTheTitleWithASmallGap() {
        Rect row = SettingRowLayout.rows(CONTENT, 1, 0, WIDE).get(0);
        Rect card = SettingRowLayout.cardBox(row);
        Rect badge = SettingRowLayout.badgeBox(row, 80);

        assertFalse(badge.isEmpty());
        int titleEnd = card.x() + SettingRowLayout.CARD_PAD_X + 80;
        int gap = badge.x() - titleEnd;
        assertTrue(gap > 0 && gap <= 8, "the badge should follow the title by a few pixels, was " + gap);
        assertTrue(badge.right() <= card.right());
    }

    @Test
    void aLongerTitlePushesTheBadgeRightByExactlyTheExtraWidth() {
        Rect row = SettingRowLayout.rows(CONTENT, 1, 0, WIDE).get(0);
        assertEquals(40, SettingRowLayout.badgeBox(row, 120).x() - SettingRowLayout.badgeBox(row, 80).x());
    }

    @Test
    void theBadgeIsAChunkySquareVerticallyCentredInTheRow() {
        Rect row = SettingRowLayout.rows(CONTENT, 1, 0, WIDE).get(0);
        Rect badge = SettingRowLayout.badgeBox(row, 60);

        assertEquals(badge.width(), badge.height());
        assertTrue(badge.width() >= 6 && badge.width() <= 9,
                "the badge should be a 6-9px pixel glyph, was " + badge.width());
        assertTrue(Math.abs((badge.y() - row.y()) - (row.bottom() - badge.bottom())) <= 1);
        assertTrue(badge.y() >= row.y() && badge.bottom() <= row.bottom());
    }

    @Test
    void aSecondBadgeSitsOneGapAfterTheFirstWithoutOverlapping() {
        Rect row = SettingRowLayout.rows(CONTENT, 1, 0, WIDE).get(0);
        Rect card = SettingRowLayout.cardBox(row);
        Rect first = SettingRowLayout.badgeBox(row, 60);
        Rect second = SettingRowLayout.badgeBox(row, 60 + SettingRowLayout.BADGE_ADVANCE);

        assertFalse(second.isEmpty());
        assertTrue(second.x() > first.right(), "badges must not overlap: " + first + " then " + second);
        assertEquals(first.x() - (card.x() + SettingRowLayout.CARD_PAD_X + 60), second.x() - first.right(),
                "the gap between two badges must match the gap after the title");
        assertEquals(first.y(), second.y());
    }

    @Test
    void theBadgeIsEmptyRatherThanRunningPastTheCard() {
        Rect row = SettingRowLayout.rows(CONTENT, 1, 0, WIDE).get(0);
        Rect card = SettingRowLayout.cardBox(row);

        assertTrue(SettingRowLayout.badgeBox(row, card.width()).isEmpty());
        for (int textWidth = 0; textWidth < card.width() * 2; textWidth += 3) {
            Rect badge = SettingRowLayout.badgeBox(row, textWidth);
            assertTrue(badge.isEmpty() || badge.right() < card.right(),
                    "badge for a " + textWidth + "px title escaped the card: " + badge + " in " + card);
        }
    }

    @Test
    void theBadgeIsEmptyWhenTheRowCannotHoldIt() {
        assertEquals(Rect.EMPTY, SettingRowLayout.badgeBox(Rect.EMPTY, 0));
        assertEquals(Rect.EMPTY, SettingRowLayout.badgeBox(new Rect(0, 0, 8, 27), 0));
        assertEquals(Rect.EMPTY, SettingRowLayout.badgeBox(new Rect(0, 0, 300, 4), 0));
    }

    @Test
    void theBadgeFitsACompactRow() {
        Rect row = SettingRowLayout.rows(new Rect(0, 32, 320, 200), 1, 0, Breakpoint.COMPACT).get(0);
        Rect badge = SettingRowLayout.badgeBox(row, 40);

        assertFalse(badge.isEmpty());
        assertTrue(badge.y() >= row.y() && badge.bottom() <= row.bottom(),
                "the badge must fit the 22px compact row: " + badge + " in " + row);
    }

    @Test
    void badgeBoxFollowsTheRowAsItScrolls() {
        Rect unscrolled = SettingRowLayout.rows(CONTENT, 2, 0, WIDE).get(1);
        Rect scrolled = SettingRowLayout.rows(CONTENT, 2, 40, WIDE).get(1);
        assertEquals(SettingRowLayout.badgeBox(unscrolled, 60).y() - 40,
                SettingRowLayout.badgeBox(scrolled, 60).y());
        assertEquals(SettingRowLayout.badgeBox(unscrolled, 60).x(), SettingRowLayout.badgeBox(scrolled, 60).x());
    }

    @Test
    void badgeBoxRejectsInvalidInput() {
        Rect row = SettingRowLayout.rows(CONTENT, 1, 0, WIDE).get(0);
        assertThrows(IllegalArgumentException.class, () -> SettingRowLayout.badgeBox(null, 0));
        assertThrows(IllegalArgumentException.class, () -> SettingRowLayout.badgeBox(row, -1));
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

    @Test
    void theCyclerPartsTileTheRightEdgeWithoutOverlapping() {
        Rect row = new Rect(164, 102, 422, 27);
        Rect prev = SettingRowLayout.cyclerPrevBox(row);
        Rect value = SettingRowLayout.cyclerValueBox(row);
        Rect next = SettingRowLayout.cyclerNextBox(row);

        Rect region = SettingRowLayout.cyclerBox(row);
        assertEquals(SettingRowLayout.ARROW_SIZE, prev.width());
        assertEquals(SettingRowLayout.ARROW_SIZE, next.width());
        assertEquals(prev.right(), value.x(), "no gap between the previous arrow and the value");
        assertEquals(value.right(), next.x(), "no gap between the value and the next arrow");
        assertEquals(region.x(), prev.x());
        assertEquals(region.right(), next.right());
        assertEquals(SettingRowLayout.cardBox(row).right() - SettingRowLayout.CARD_PAD_X, next.right());
        assertTrue(value.width() > SettingRowLayout.ARROW_SIZE * 2,
                "the value column must be the widest part");
    }

    @Test
    void theCyclerStaysInsideTheCardAndClearsTheStar() {
        Rect row = new Rect(164, 102, 422, 27);
        Rect card = SettingRowLayout.cardBox(row);

        assertTrue(SettingRowLayout.cyclerPrevBox(row).x() >= card.x() + SettingRowLayout.CARD_PAD_X);
        assertTrue(SettingRowLayout.cyclerNextBox(row).right() <= card.right());
        assertTrue(SettingRowLayout.cyclerNextBox(row).right() <= SettingRowLayout.starBox(row).x(),
                "the cycler must not sit under the favourite star");
    }

    @Test
    void theCyclerCollapsesRatherThanOverlapTheTitleOnANarrowRow() {
        assertTrue(SettingRowLayout.cyclerPrevBox(new Rect(0, 0, 170, 27)).isEmpty());
        assertTrue(SettingRowLayout.cyclerValueBox(new Rect(0, 0, 170, 27)).isEmpty());
        assertTrue(SettingRowLayout.cyclerNextBox(new Rect(0, 0, 170, 27)).isEmpty());
        assertTrue(SettingRowLayout.cyclerBox(new Rect(0, 0, 170, 27)).isEmpty());
        assertTrue(SettingRowLayout.cyclerPrevBox(Rect.EMPTY).isEmpty());
    }

    @Test
    void theCyclerRegionGrowsWithTheRowAndLeavesRoomForTheTitle() {
        int previous = 0;
        for (int width : new int[] {300, 422, 700, 1200}) {
            Rect row = new Rect(164, 102, width, 27);
            Rect region = SettingRowLayout.cyclerBox(row);
            Rect card = SettingRowLayout.cardBox(row);
            assertTrue(region.width() >= previous, "the region must not shrink as the row grows");
            previous = region.width();
            assertTrue(region.x() - (card.x() + SettingRowLayout.CARD_PAD_X) >= 100,
                    "the title keeps its room at width " + width);
            assertTrue(region.right() <= card.right() - SettingRowLayout.CARD_PAD_X);
        }
    }

    @Test
    void theArrowsAreSquareButtonsCentredOnTheRow() {
        Rect row = new Rect(164, 102, 422, 27);
        Rect prev = SettingRowLayout.cyclerPrevBox(row);

        assertEquals(SettingRowLayout.ARROW_SIZE, prev.width());
        assertEquals(SettingRowLayout.ARROW_SIZE, prev.height());
        assertEquals(row.y() + (row.height() - SettingRowLayout.ARROW_SIZE) / 2, prev.y());
        assertTrue(prev.bottom() <= row.bottom());
    }

    @Test
    void cyclerBoxesRejectANullRow() {
        assertThrows(IllegalArgumentException.class, () -> SettingRowLayout.cyclerPrevBox(null));
        assertThrows(IllegalArgumentException.class, () -> SettingRowLayout.cyclerValueBox(null));
        assertThrows(IllegalArgumentException.class, () -> SettingRowLayout.cyclerNextBox(null));
    }
}
