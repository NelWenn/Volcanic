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
