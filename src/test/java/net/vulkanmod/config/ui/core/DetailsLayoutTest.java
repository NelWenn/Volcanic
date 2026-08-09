package net.vulkanmod.config.ui.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DetailsLayoutTest {
    private static final Rect PANEL = new Rect(658, 32, 196, 414);

    @Test
    void reservesThePaddingOnBothSides() {
        assertEquals(PANEL.width() - DetailsLayout.PAD_X * 2, DetailsLayout.textWidth(PANEL));
    }

    @Test
    void neverReportsANegativeTextWidth() {
        assertEquals(0, DetailsLayout.textWidth(new Rect(0, 0, 4, 40)));
        assertEquals(0, DetailsLayout.textWidth(Rect.EMPTY));
    }

    @Test
    void stacksLinesOnTheNinePixelRhythm() {
        assertEquals(PANEL.y() + DetailsLayout.PAD_Y, DetailsLayout.lineTop(PANEL, 0));
        for (int index = 1; index < 8; index++) {
            assertEquals(DetailsLayout.lineTop(PANEL, index - 1) + DetailsLayout.TEXT_HEIGHT,
                    DetailsLayout.lineTop(PANEL, index));
        }
    }

    @Test
    void everyLineWithinCapacityStaysInsideThePanel() {
        int capacity = DetailsLayout.lineCapacity(PANEL);
        assertTrue(capacity > 0, "the mockup panel must fit at least one line");
        assertTrue(DetailsLayout.lineTop(PANEL, capacity - 1) + DetailsLayout.TEXT_HEIGHT <= PANEL.bottom());
    }

    @Test
    void reportsNoCapacityWhenThereIsNoRoom() {
        assertEquals(0, DetailsLayout.lineCapacity(Rect.EMPTY));
        assertEquals(0, DetailsLayout.lineCapacity(new Rect(0, 0, 196, DetailsLayout.PAD_Y)));
    }

    @Test
    void barTracksTheTextColumnAndKeepsItsHeight() {
        Rect bar = DetailsLayout.bar(PANEL, 3);

        assertEquals(PANEL.x() + DetailsLayout.PAD_X, bar.x());
        assertEquals(DetailsLayout.textWidth(PANEL), bar.width());
        assertEquals(DetailsLayout.BAR_HEIGHT, bar.height());
        assertEquals(DetailsLayout.lineTop(PANEL, 3)
                + (DetailsLayout.TEXT_HEIGHT - DetailsLayout.BAR_HEIGHT) / 2, bar.y());
    }

    @Test
    void barCollapsesWhenThePanelIsTooNarrow() {
        assertTrue(DetailsLayout.bar(new Rect(0, 0, 8, 40), 0).isEmpty());
    }

    @Test
    void fillGrowsWithTheLevelAndNeverOverflowsTheTrack() {
        Rect track = DetailsLayout.bar(PANEL, 0);

        assertTrue(DetailsLayout.barFill(track, ImpactLevel.NONE).isEmpty());
        int low = DetailsLayout.barFill(track, ImpactLevel.LOW).width();
        int medium = DetailsLayout.barFill(track, ImpactLevel.MEDIUM).width();
        int high = DetailsLayout.barFill(track, ImpactLevel.HIGH).width();

        assertTrue(low < medium, "medium must read heavier than low");
        assertTrue(medium < high, "high must read heavier than medium");
        assertTrue(high < track.width(), "the spec never fills the bar to the end");
        for (ImpactLevel level : ImpactLevel.values()) {
            Rect filled = DetailsLayout.barFill(track, level);
            if (filled.isEmpty()) {
                continue;
            }
            assertEquals(track.x(), filled.x(), level + " must start at the track origin");
            assertEquals(track.y(), filled.y());
            assertEquals(track.height(), filled.height());
            assertTrue(filled.right() <= track.right(), level + " must not spill past the track");
        }
    }

    @Test
    void fillOfAnEmptyTrackIsEmpty() {
        assertTrue(DetailsLayout.barFill(Rect.EMPTY, ImpactLevel.HIGH).isEmpty());
    }

    @Test
    void impactFillFractionsAreOrderedAndBounded() {
        assertEquals(0.0f, ImpactLevel.NONE.fill());
        ImpactLevel previous = ImpactLevel.NONE;
        for (ImpactLevel level : ImpactLevel.values()) {
            assertTrue(level.fill() >= 0.0f && level.fill() < 1.0f, level + " must stay within the track");
            assertTrue(level.fill() >= previous.fill(), level + " must not read lighter than " + previous);
            previous = level;
        }
    }

    @Test
    void everyLevelNamesADistinctLabelKey() {
        assertEquals(ImpactLevel.values().length,
                java.util.Arrays.stream(ImpactLevel.values()).map(ImpactLevel::labelKey).distinct().count());
        assertEquals("vulkanmod.impact.high", ImpactLevel.HIGH.labelKey());
    }

    @Test
    void heightAndCapacityAreInverses() {
        for (int count = 1; count < 40; count++) {
            int height = DetailsLayout.height(count);
            assertTrue(DetailsLayout.lineCapacity(height) >= count,
                    "a box sized for " + count + " items must hold them");
            assertEquals(count, DetailsLayout.lineCapacity(height),
                    "and must not claim room for more");
        }
        assertEquals(0, DetailsLayout.height(0));
    }

    @Test
    void theSheetSitsAtTheBottomOfTheContentInsideItsMargin() {
        Rect content = new Rect(0, 32, 420, 207);
        Rect sheet = DetailsLayout.sheet(content, 90, 4);

        assertEquals(content.x() + 4, sheet.x());
        assertEquals(content.width() - 8, sheet.width());
        assertEquals(90, sheet.height());
        assertEquals(content.bottom() - 4, sheet.bottom());
    }

    @Test
    void theSheetIsCappedByTheContentAndCollapsesWhenThereIsNoRoom() {
        Rect content = new Rect(0, 32, 420, 207);

        assertEquals(content.height() - 8, DetailsLayout.sheet(content, 9000, 4).height());
        assertTrue(DetailsLayout.sheet(content, 0, 4).isEmpty());
        assertTrue(DetailsLayout.sheet(new Rect(0, 0, 6, 200), 40, 4).isEmpty());
        assertTrue(DetailsLayout.sheet(Rect.EMPTY, 40, 4).isEmpty());
    }

    @Test
    void rejectsInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> DetailsLayout.height(-1));
        assertThrows(IllegalArgumentException.class, () -> DetailsLayout.sheet(null, 10, 4));
        assertThrows(IllegalArgumentException.class, () -> DetailsLayout.textWidth(null));
        assertThrows(IllegalArgumentException.class, () -> DetailsLayout.lineCapacity(null));
        assertThrows(IllegalArgumentException.class, () -> DetailsLayout.lineTop(null, 0));
        assertThrows(IllegalArgumentException.class, () -> DetailsLayout.lineTop(PANEL, -1));
        assertThrows(IllegalArgumentException.class, () -> DetailsLayout.bar(PANEL, -1));
        assertThrows(IllegalArgumentException.class, () -> DetailsLayout.barFill(null, ImpactLevel.LOW));
        assertThrows(IllegalArgumentException.class, () -> DetailsLayout.barFill(PANEL, null));
    }
}
