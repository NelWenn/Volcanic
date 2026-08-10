package net.vulkanmod.config.ui.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmberFieldTest {
    private static final Rect AREA = new Rect(40, 20, 300, 200);

    @Test
    void everySparkStaysInsideTheAreaItWasGivenSoNothingLeaksOverTheChrome() {
        EmberField field = new EmberField(7L);
        for (int frame = 0; frame < 400; frame++) {
            field.advance(16, AREA.height());
            for (int index = 0; index < EmberField.SPARKS; index++) {
                int y = field.yOf(index, AREA);
                assertTrue(y >= AREA.y() && y <= AREA.bottom(), "spark left the band vertically at " + y);
            }
        }
    }

    @Test
    void sparksRiseAndNeverFall() {
        EmberField field = new EmberField(3L);
        field.advance(16, AREA.height());
        int before = field.yOf(0, AREA);
        for (int frame = 0; frame < 5; frame++) {
            field.advance(16, AREA.height());
        }
        assertTrue(field.yOf(0, AREA) <= before, "a spark drifted downwards");
    }

    @Test
    void aSparkThatReachesTheTopComesBackFromTheBottomInsteadOfDisappearing() {
        EmberField field = new EmberField(11L);
        for (int frame = 0; frame < 2000; frame++) {
            field.advance(16, AREA.height());
        }
        boolean anyLow = false;
        for (int index = 0; index < EmberField.SPARKS; index++) {
            anyLow |= field.yOf(index, AREA) > AREA.y() + AREA.height() / 2;
        }
        assertTrue(anyLow, "the whole field drained to the top and never recycled");
    }

    @Test
    void itStaysFaintEnoughToReadAsAtmosphereRatherThanContent() {
        EmberField field = new EmberField(5L);
        for (int frame = 0; frame < 300; frame++) {
            field.advance(16, AREA.height());
            for (int index = 0; index < EmberField.SPARKS; index++) {
                int alpha = field.colorOf(index) >>> 24;
                assertTrue(alpha <= 40, "a spark reached alpha " + alpha + ", which would draw attention");
            }
        }
    }

    @Test
    void aSparkFadesOutAsItClimbsSoNothingPopsAtTheTopEdge() {
        EmberField field = new EmberField(2L);
        field.advance(16, AREA.height());
        int low = field.colorOf(0) >>> 24;
        for (int frame = 0; frame < 300; frame++) {
            field.advance(16, AREA.height());
            if (field.yOf(0, AREA) < AREA.y() + 8) {
                assertTrue((field.colorOf(0) >>> 24) <= low, "it was still bright at the top edge");
                return;
            }
        }
    }

    @Test
    void theSameElapsedTimeAdvancesTheFieldEquallyWhateverTheFrameRate() {
        EmberField slow = new EmberField(9L);
        EmberField fast = new EmberField(9L);
        for (int frame = 0; frame < 10; frame++) {
            slow.advance(50, AREA.height());
        }
        for (int frame = 0; frame < 50; frame++) {
            fast.advance(10, AREA.height());
        }
        assertEquals(slow.yOf(0, AREA), fast.yOf(0, AREA));
    }

    @Test
    void aStalledFrameCannotTeleportTheFieldBecauseTheStepIsCapped() {
        EmberField huge = new EmberField(4L);
        EmberField capped = new EmberField(4L);
        huge.advance(9000, AREA.height());
        capped.advance(100, AREA.height());
        assertEquals(capped.yOf(0, AREA), huge.yOf(0, AREA));
    }

    @Test
    void anEmptyBandOrANegativeFrameIsHandledWithoutMoving() {
        EmberField field = new EmberField(1L);
        int before = field.yOf(0, AREA);
        field.advance(16, 0);
        assertEquals(before, field.yOf(0, AREA));
        assertThrows(IllegalArgumentException.class, () -> field.advance(-1, AREA.height()));
    }
}
