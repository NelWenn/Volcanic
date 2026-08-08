package net.vulkanmod.config.ui.core;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class SettingRowLayoutTest {
    private static final Rect CONTENT = new Rect(150, 32, 500, 400);

    @Test
    void rowsStackWithAConstantPitchInsideTheContentRect() {
        List<Rect> rows = SettingRowLayout.rows(CONTENT, 3, 0);
        assertEquals(3, rows.size());
        assertTrue(rows.get(0).x() >= CONTENT.x());
        assertTrue(rows.get(0).right() <= CONTENT.right());
        int pitch = rows.get(1).y() - rows.get(0).y();
        assertEquals(pitch, rows.get(2).y() - rows.get(1).y());
        assertTrue(pitch > rows.get(0).height());
    }

    @Test
    void scrollingShiftsEveryRowUpByTheSameAmount() {
        List<Rect> unscrolled = SettingRowLayout.rows(CONTENT, 3, 0);
        List<Rect> scrolled = SettingRowLayout.rows(CONTENT, 3, 40);
        for (int i = 0; i < 3; i++) {
            assertEquals(unscrolled.get(i).y() - 40, scrolled.get(i).y());
            assertEquals(unscrolled.get(i).x(), scrolled.get(i).x());
        }
    }

    @Test
    void noSettingsMeansNoRows() {
        assertTrue(SettingRowLayout.rows(CONTENT, 0, 0).isEmpty());
        assertTrue(SettingRowLayout.rows(Rect.EMPTY, 3, 0).isEmpty());
    }

    @Test
    void rejectsInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> SettingRowLayout.rows(null, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> SettingRowLayout.rows(CONTENT, -1, 0));
        assertThrows(IllegalArgumentException.class, () -> SettingRowLayout.rows(CONTENT, 1, -1));
    }

    @Test
    void resetBoxSitsAtTheRightEndOfTheRowAndIsVerticallyCentred() {
        Rect row = SettingRowLayout.rows(CONTENT, 1, 0).get(0);
        Rect reset = SettingRowLayout.resetBox(row);
        assertFalse(reset.isEmpty());
        assertTrue(reset.right() < row.right());
        assertTrue(reset.x() > row.x() + row.width() / 2);
        assertTrue(Math.abs((reset.y() - row.y()) - (row.bottom() - reset.bottom())) <= 1);
        assertTrue(reset.y() >= row.y() && reset.bottom() <= row.bottom());
    }

    @Test
    void resetBoxFollowsTheRowAsItScrolls() {
        Rect unscrolled = SettingRowLayout.rows(CONTENT, 2, 0).get(1);
        Rect scrolled = SettingRowLayout.rows(CONTENT, 2, 40).get(1);
        assertEquals(SettingRowLayout.resetBox(unscrolled).y() - 40, SettingRowLayout.resetBox(scrolled).y());
        assertEquals(SettingRowLayout.resetBox(unscrolled).x(), SettingRowLayout.resetBox(scrolled).x());
    }

    @Test
    void resetBoxIsEmptyWhenTheRowCannotHoldIt() {
        assertEquals(Rect.EMPTY, SettingRowLayout.resetBox(Rect.EMPTY));
        assertEquals(Rect.EMPTY, SettingRowLayout.resetBox(new Rect(0, 0, 8, SettingRowLayout.ROW_HEIGHT)));
        assertEquals(Rect.EMPTY, SettingRowLayout.resetBox(new Rect(0, 0, 300, 4)));
    }

    @Test
    void resetBoxRejectsInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> SettingRowLayout.resetBox(null));
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
