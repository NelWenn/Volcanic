package net.vulkanmod.config.ui.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("InfoRowLayout")
class InfoRowLayoutTest {
    private static final Rect CONTENT = new Rect(132, 32, 388, 400);
    private static final boolean[] SHAPE = {true, false, false, true, false};

    @Test
    @DisplayName("rows stack in order and never overlap")
    void rowsNeverOverlap() {
        List<Rect> rows = InfoRowLayout.rows(CONTENT, SHAPE, 0);

        assertEquals(SHAPE.length, rows.size());
        for (int index = 1; index < rows.size(); index++) {
            assertEquals(rows.get(index - 1).bottom(), rows.get(index).y(),
                    "row " + index + " must start exactly where the previous one ends");
        }
    }

    @Test
    @DisplayName("a section heading is taller than a data row, so it reads as a heading")
    void sectionsAreTaller() {
        List<Rect> rows = InfoRowLayout.rows(CONTENT, SHAPE, 0);

        assertTrue(rows.get(0).height() > rows.get(1).height());
        assertEquals(InfoRowLayout.SECTION_H, rows.get(0).height());
        assertEquals(InfoRowLayout.ROW_H, rows.get(1).height());
    }

    @Test
    @DisplayName("the label and the value share a row without ever touching")
    void labelAndValueNeverCollide() {
        Rect row = InfoRowLayout.rows(CONTENT, SHAPE, 0).get(1);
        Rect label = InfoRowLayout.label(row);
        Rect value = InfoRowLayout.value(row);

        assertFalse(label.isEmpty());
        assertFalse(value.isEmpty());
        assertTrue(label.right() < value.x(), "the label would run into the value");
        assertEquals(row.right() - InfoRowLayout.SCROLLBAR, value.right(),
                "the value must stop clear of the scroll bar");
        assertTrue(value.width() > row.width() / 3,
                "the value column must be wide enough for a real value, not a truncated one");
        assertTrue(label.y() >= row.y() && label.bottom() <= row.bottom());
    }

    @Test
    @DisplayName("a section paints its accent, label and rule inside its own row")
    void sectionPartsStayInsideTheRow() {
        Rect row = InfoRowLayout.rows(CONTENT, SHAPE, 0).get(0);

        for (Rect part : new Rect[] {InfoRowLayout.sectionAccent(row),
                InfoRowLayout.sectionLabel(row), InfoRowLayout.sectionRule(row)}) {
            assertTrue(part.y() >= row.y() && part.bottom() <= row.bottom());
            assertTrue(part.x() >= row.x() && part.right() <= row.right());
        }
        assertTrue(InfoRowLayout.sectionLabel(row).x() > InfoRowLayout.sectionAccent(row).right());
    }

    @Test
    @DisplayName("height matches the last row, so the page never scrolls short")
    void heightCoversTheLastRow() {
        List<Rect> rows = InfoRowLayout.rows(CONTENT, SHAPE, 0);
        int used = rows.get(rows.size() - 1).bottom() - CONTENT.y() + InfoRowLayout.BOTTOM;

        assertEquals(used, InfoRowLayout.contentHeight(SHAPE));
    }

    @Test
    @DisplayName("scrolling moves every row by exactly the scroll amount")
    void scrollShiftsEveryRow() {
        List<Rect> resting = InfoRowLayout.rows(CONTENT, SHAPE, 0);
        List<Rect> scrolled = InfoRowLayout.rows(CONTENT, SHAPE, 25);

        for (int index = 0; index < resting.size(); index++) {
            assertEquals(resting.get(index).y() - 25, scrolled.get(index).y());
        }
    }

    @Test
    @DisplayName("a row too narrow for both columns gives up the label rather than overlapping")
    void narrowRowsDegradeRatherThanOverlap() {
        Rect narrow = new Rect(0, 0, InfoRowLayout.TEXT_X + InfoRowLayout.SCROLLBAR + 4,
                InfoRowLayout.ROW_H);

        assertTrue(InfoRowLayout.label(narrow).isEmpty());
        assertFalse(InfoRowLayout.value(narrow).isEmpty(), "the value is what the reader came for");
    }

    @Test
    @DisplayName("no rows and a degenerate content area yield nothing rather than negative boxes")
    void degenerateInputsYieldNothing() {
        assertTrue(InfoRowLayout.rows(CONTENT, new boolean[0], 0).isEmpty());
        assertTrue(InfoRowLayout.rows(new Rect(0, 0, InfoRowLayout.PAD_X * 2, 100), SHAPE, 0).isEmpty());
        assertTrue(InfoRowLayout.label(Rect.EMPTY).isEmpty());
        assertTrue(InfoRowLayout.sectionRule(Rect.EMPTY).isEmpty());
    }

    @Test
    @DisplayName("null inputs are rejected rather than painting nowhere")
    void nullsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> InfoRowLayout.rows(null, SHAPE, 0));
        assertThrows(IllegalArgumentException.class, () -> InfoRowLayout.rows(CONTENT, null, 0));
        assertThrows(IllegalArgumentException.class, () -> InfoRowLayout.contentHeight(null));
    }
}
