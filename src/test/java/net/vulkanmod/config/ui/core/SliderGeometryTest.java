package net.vulkanmod.config.ui.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SliderGeometryTest {
    private static final Rect TRACK = new Rect(100, 50, 200, 3);
    private static final Rect CARD = new Rect(20, 40, 200, 27);

    @Test
    void theKnobSitsAtTheLeftEdgeAtMinimumAndTheRightEdgeAtMaximum() {
        assertEquals(TRACK.x(), SliderGeometry.knob(TRACK, 10, 10, 260, 6).x());
        assertEquals(TRACK.right() - 6, SliderGeometry.knob(TRACK, 260, 10, 260, 6).x());
    }

    @Test
    void theKnobNeverLeavesTheTrack() {
        for (int value = 10; value <= 260; value += 7) {
            Rect knob = SliderGeometry.knob(TRACK, value, 10, 260, 6);
            assertTrue(knob.x() >= TRACK.x(), "knob left of track at " + value);
            assertTrue(knob.right() <= TRACK.right(), "knob right of track at " + value);
        }
    }

    @Test
    void aValueOutsideTheRangeIsClampedRatherThanExtrapolated() {
        assertEquals(TRACK.x(), SliderGeometry.knob(TRACK, -50, 10, 260, 6).x());
        assertEquals(TRACK.right() - 6, SliderGeometry.knob(TRACK, 9999, 10, 260, 6).x());
    }

    @Test
    void clickingTheEndsOfTheTrackGivesTheEndsOfTheRange() {
        assertEquals(10, SliderGeometry.valueAt(TRACK, TRACK.x(), 10, 260, 10));
        assertEquals(260, SliderGeometry.valueAt(TRACK, TRACK.right(), 10, 260, 10));
    }

    @Test
    void aCursorOutsideTheTrackClampsInsteadOfRunningAway() {
        assertEquals(10, SliderGeometry.valueAt(TRACK, TRACK.x() - 500, 10, 260, 10));
        assertEquals(260, SliderGeometry.valueAt(TRACK, TRACK.right() + 500, 10, 260, 10));
    }

    @Test
    void everyValueItReportsIsOnAStepBoundary() {
        for (int x = TRACK.x() - 10; x <= TRACK.right() + 10; x++) {
            int value = SliderGeometry.valueAt(TRACK, x, 10, 260, 10);
            assertEquals(0, (value - 10) % 10, "off-step value " + value + " at x=" + x);
        }
    }

    @Test
    void aRangeThatIsNotAWholeNumberOfStepsIsNeverOvershot() {
        assertEquals(20, SliderGeometry.valueAt(TRACK, TRACK.right(), 0, 25, 10));
    }

    @Test
    void knobAndValueAtAgreeWithEachOther() {
        for (int value = 10; value <= 260; value += 10) {
            Rect knob = SliderGeometry.knob(TRACK, value, 10, 260, 6);
            assertEquals(value, SliderGeometry.valueAt(TRACK, knob.x() + 3, 10, 260, 10),
                    "round trip failed for " + value);
        }
    }

    @Test
    void theyStillAgreeWhenAStepIsThinnerThanTheKnob() {
        for (int value = 0; value <= 100; value++) {
            Rect knob = SliderGeometry.knob(TRACK, value, 0, 100, SliderGeometry.KNOB_WIDTH);
            int centre = knob.x() + SliderGeometry.KNOB_WIDTH / 2;
            assertEquals(value, SliderGeometry.valueAt(TRACK, centre, 0, 100, 1),
                    "round trip failed for " + value);
        }
    }

    @Test
    void theKnobStandsProudOfTheTrackItSitsOn() {
        Rect knob = SliderGeometry.knob(TRACK, 135, 10, 260, 6);
        assertEquals(6, knob.height());
        assertTrue(knob.y() <= TRACK.y(), "knob top below track top");
        assertTrue(knob.bottom() >= TRACK.bottom(), "knob bottom above track bottom");
    }

    @Test
    void aDegenerateRangeDoesNotDivideByZero() {
        assertEquals(50, SliderGeometry.valueAt(TRACK, TRACK.x() + 100, 50, 50, 1));
        assertEquals(TRACK.x(), SliderGeometry.knob(TRACK, 50, 50, 50, 6).x());
    }

    @Test
    void theTrackIsPinnedToTheRightOfTheCardAndSpansItsHeight() {
        Rect track = SliderGeometry.track(CARD, 12, 56);
        assertEquals(CARD.right() - 12 - 56, track.x());
        assertEquals(CARD.y(), track.y());
        assertEquals(56, track.width());
        assertEquals(CARD.height(), track.height());
    }

    @Test
    void aCardWithNoRoomBesideItsTitleReportsNoTrack() {
        assertTrue(SliderGeometry.track(new Rect(20, 40, 68, 27), 12, 56).isEmpty());
        assertTrue(SliderGeometry.track(Rect.EMPTY, 12, 56).isEmpty());
    }

    @Test
    void aKnobOnACardTrackStaysInsideTheCardAndRoundTrips() {
        Rect track = SliderGeometry.track(CARD, 12, 56);
        for (int value = 10; value <= 260; value += 10) {
            Rect knob = SliderGeometry.knob(track, value, 10, 260, SliderGeometry.KNOB_WIDTH);
            assertTrue(knob.x() >= track.x(), "knob left of track at " + value);
            assertTrue(knob.right() <= track.right(), "knob right of track at " + value);
            assertTrue(knob.y() >= CARD.y() && knob.bottom() <= CARD.bottom(), "knob outside card at " + value);
            assertEquals(value,
                    SliderGeometry.valueAt(track, knob.x() + SliderGeometry.KNOB_WIDTH / 2, 10, 260, 10),
                    "round trip failed for " + value);
        }
    }

    @Test
    void rejectsInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> SliderGeometry.track(null, 12, 56));
        assertThrows(IllegalArgumentException.class, () -> SliderGeometry.track(CARD, -1, 56));
        assertThrows(IllegalArgumentException.class, () -> SliderGeometry.track(CARD, 12, 0));
        assertThrows(IllegalArgumentException.class, () -> SliderGeometry.knob(null, 1, 0, 10, 6));
        assertThrows(IllegalArgumentException.class, () -> SliderGeometry.knob(TRACK, 1, 10, 0, 6));
        assertThrows(IllegalArgumentException.class, () -> SliderGeometry.knob(TRACK, 1, 0, 10, 0));
        assertThrows(IllegalArgumentException.class, () -> SliderGeometry.valueAt(TRACK, 0, 0, 10, 0));
    }
}
