package net.vulkanmod.config.ui.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ScrollIndicatorTest {
    private static final Rect VIEWPORT = new Rect(150, 32, 500, 400);

    @Test
    void contentThatFitsHasNoIndicator() {
        assertEquals(ScrollIndicator.NONE, ScrollIndicator.of(VIEWPORT, 0, 0));
        assertEquals(ScrollIndicator.NONE, ScrollIndicator.of(VIEWPORT, 399, 0));
        assertEquals(ScrollIndicator.NONE, ScrollIndicator.of(VIEWPORT, 400, 0));
        assertFalse(ScrollIndicator.of(VIEWPORT, 400, 0).visible());
    }

    @Test
    void anAbsentIndicatorIsEmptyRectsNeverNull() {
        ScrollIndicator none = ScrollIndicator.of(VIEWPORT, 100, 0);
        assertNotNull(none.track());
        assertNotNull(none.thumb());
        assertTrue(none.track().isEmpty());
        assertTrue(none.thumb().isEmpty());
    }

    @Test
    void anEmptyViewportHasNoIndicator() {
        assertEquals(ScrollIndicator.NONE, ScrollIndicator.of(Rect.EMPTY, 900, 0));
        assertEquals(ScrollIndicator.NONE, ScrollIndicator.of(new Rect(0, 0, 3, 400), 900, 0));
    }

    @Test
    void overflowingContentGetsAVisibleIndicator() {
        ScrollIndicator indicator = ScrollIndicator.of(VIEWPORT, 800, 0);
        assertTrue(indicator.visible());
        assertFalse(indicator.track().isEmpty());
        assertFalse(indicator.thumb().isEmpty());
    }

    @Test
    void theTrackStaysInsideTheViewportOnItsRightEdge() {
        Rect track = ScrollIndicator.of(VIEWPORT, 800, 0).track();
        assertTrue(track.x() >= VIEWPORT.x());
        assertTrue(track.right() <= VIEWPORT.right());
        assertTrue(track.x() > VIEWPORT.x() + VIEWPORT.width() / 2, "the track belongs on the right edge");
        assertEquals(VIEWPORT.y(), track.y());
        assertEquals(VIEWPORT.bottom(), track.bottom());
        assertTrue(track.width() <= 6, "a thin track, was " + track.width());
    }

    @Test
    void theThumbSharesTheTrackColumnSoDrawingAndHitTestingCannotDisagree() {
        for (int scroll = 0; scroll <= 400; scroll += 37) {
            ScrollIndicator indicator = ScrollIndicator.of(VIEWPORT, 800, scroll);
            assertEquals(indicator.track().x(), indicator.thumb().x(), "scroll " + scroll);
            assertEquals(indicator.track().width(), indicator.thumb().width(), "scroll " + scroll);
        }
    }

    @Test
    void theThumbIsProportionalToTheVisibleFractionUntilItHitsTheCap() {
        assertEquals(100, ScrollIndicator.of(VIEWPORT, 1600, 0).thumb().height());
        assertEquals(133, ScrollIndicator.of(VIEWPORT, 1200, 0).thumb().height());
    }

    @Test
    void aBarelyOverflowingListStillGetsAShortThumbThatTravels() {
        ScrollIndicator indicator = ScrollIndicator.of(VIEWPORT, 413, 0);
        int thumb = indicator.thumb().height();
        assertTrue(thumb <= VIEWPORT.height() / 3,
                "a 13px overflow must not produce a near-full-height thumb, was " + thumb);
        assertTrue(indicator.track().height() - thumb > VIEWPORT.height() / 2,
                "the thumb must have room to travel visibly");
    }

    @Test
    void theThumbNeverShrinksBelowAGrabbableSize() {
        Rect thumb = ScrollIndicator.of(VIEWPORT, 400000, 0).thumb();
        assertTrue(thumb.height() >= 8, "thumb should stay grabbable, was " + thumb.height());
    }

    @Test
    void theThumbIsFlushWithTheTrackAtBothEndsOfTheScrollRange() {
        int contentHeight = 977;
        int maxScroll = contentHeight - VIEWPORT.height();

        ScrollIndicator top = ScrollIndicator.of(VIEWPORT, contentHeight, 0);
        assertEquals(top.track().y(), top.thumb().y());

        ScrollIndicator bottom = ScrollIndicator.of(VIEWPORT, contentHeight, maxScroll);
        assertEquals(bottom.track().bottom(), bottom.thumb().bottom());
    }

    @Test
    void theThumbStaysInsideTheTrackAndOnlyEverMovesDownwards() {
        int contentHeight = 977;
        int previous = Integer.MIN_VALUE;
        for (int scroll = 0; scroll <= contentHeight - VIEWPORT.height(); scroll++) {
            ScrollIndicator indicator = ScrollIndicator.of(VIEWPORT, contentHeight, scroll);
            Rect track = indicator.track();
            Rect thumb = indicator.thumb();
            assertTrue(thumb.y() >= track.y(), "thumb escaped above the track at scroll " + scroll);
            assertTrue(thumb.bottom() <= track.bottom(), "thumb escaped below the track at scroll " + scroll);
            assertTrue(thumb.y() >= previous, "thumb moved back up at scroll " + scroll);
            previous = thumb.y();
        }
    }

    @Test
    void scrollPastTheEndIsClampedToTheBottomOfTheTrack() {
        int contentHeight = 977;
        ScrollIndicator overshot = ScrollIndicator.of(VIEWPORT, contentHeight, 100000);
        assertEquals(overshot.track().bottom(), overshot.thumb().bottom());
        assertEquals(ScrollIndicator.of(VIEWPORT, contentHeight, contentHeight - VIEWPORT.height()), overshot);
    }

    @Test
    void aViewportShorterThanTheMinimumThumbStillKeepsTheThumbInsideTheTrack() {
        Rect tiny = new Rect(0, 0, 40, 5);
        ScrollIndicator indicator = ScrollIndicator.of(tiny, 300, 2);
        assertTrue(indicator.thumb().y() >= indicator.track().y());
        assertTrue(indicator.thumb().bottom() <= indicator.track().bottom());
    }

    @Test
    void rejectsInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> ScrollIndicator.of(null, 800, 0));
        assertThrows(IllegalArgumentException.class, () -> ScrollIndicator.of(VIEWPORT, -1, 0));
        assertThrows(IllegalArgumentException.class, () -> ScrollIndicator.of(VIEWPORT, 800, -1));
        assertThrows(IllegalArgumentException.class, () -> new ScrollIndicator(null, Rect.EMPTY));
        assertThrows(IllegalArgumentException.class, () -> new ScrollIndicator(Rect.EMPTY, null));
    }

    @Test
    void theSettingsListFeedsTheIndicatorTheSameHeightItScrollsBy() {
        Rect content = new Rect(150, 32, 500, 140);
        for (int count = 0; count < 12; count++) {
            for (Breakpoint breakpoint : Breakpoint.values()) {
                int contentHeight = SettingRowLayout.contentHeight(count, breakpoint);
                assertEquals(SettingRowLayout.maxScroll(content, count, breakpoint),
                        Math.max(0, contentHeight - content.height()),
                        "count " + count + " at " + breakpoint);
                assertEquals(contentHeight > content.height(),
                        ScrollIndicator.of(content, contentHeight, 0).visible(),
                        "count " + count + " at " + breakpoint);
            }
        }
    }

    @Test
    void contentHeightCoversEveryRowPlusTheHeaderAndTheBottomPadding() {
        Rect tall = new Rect(0, 0, 500, 10000);
        for (Breakpoint breakpoint : Breakpoint.values()) {
            int contentHeight = SettingRowLayout.contentHeight(6, breakpoint);
            Rect last = SettingRowLayout.rows(tall, 6, 0, breakpoint).get(5);
            assertTrue(contentHeight > last.bottom() - tall.y(), "content must extend past the last row");
            assertTrue(contentHeight < last.bottom() - tall.y() + 30, "content must not overshoot the last row");
        }
        assertEquals(0, SettingRowLayout.contentHeight(0, Breakpoint.WIDE));
    }

    @Test
    void contentHeightRejectsInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> SettingRowLayout.contentHeight(-1, Breakpoint.WIDE));
        assertThrows(IllegalArgumentException.class, () -> SettingRowLayout.contentHeight(1, null));
    }
}
