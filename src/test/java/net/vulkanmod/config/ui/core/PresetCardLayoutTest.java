package net.vulkanmod.config.ui.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PresetCardLayoutTest {
    private static final Rect WIDE = new Rect(150, 32, 654, 414);
    private static final Rect NARROW = new Rect(0, 32, 420, 207);

    @Test
    void twoColumnsOnlyWhenThereIsRoom() {
        assertEquals(2, PresetCardLayout.columns(PresetCardLayout.TWO_COLUMN_AT));
        assertEquals(1, PresetCardLayout.columns(PresetCardLayout.TWO_COLUMN_AT - 1));
        assertEquals(1, PresetCardLayout.columns(0));
    }

    @Test
    void cardsTileTheGridWithoutOverlapping() {
        List<Rect> cards = PresetCardLayout.cards(WIDE, 4, 0, Breakpoint.WIDE);

        assertEquals(4, cards.size());
        assertEquals(cards.get(0).right() + PresetCardLayout.GAP, cards.get(1).x());
        assertEquals(cards.get(0).y(), cards.get(1).y());
        assertEquals(cards.get(0).x(), cards.get(2).x());
        assertEquals(cards.get(0).bottom() + PresetCardLayout.GAP, cards.get(2).y());
        for (Rect card : cards) {
            assertEquals(PresetCardLayout.CARD_HEIGHT, card.height());
            assertTrue(card.x() >= WIDE.x() + PresetCardLayout.PAD_X);
            assertTrue(card.right() <= WIDE.right() - PresetCardLayout.PAD_X);
        }
    }

    @Test
    void oneColumnFillsTheWidthOnANarrowShell() {
        List<Rect> cards = PresetCardLayout.cards(NARROW, 3, 0, Breakpoint.COMPACT);

        assertEquals(NARROW.width() - PresetCardLayout.PAD_X * 2, cards.get(0).width());
        assertEquals(PresetCardLayout.CARD_HEIGHT_COMPACT, cards.get(0).height());
        assertEquals(cards.get(0).bottom() + PresetCardLayout.GAP, cards.get(1).y());
    }

    @Test
    void scrollMovesEveryCardByTheSameAmount() {
        List<Rect> still = PresetCardLayout.cards(WIDE, 5, 0, Breakpoint.WIDE);
        List<Rect> scrolled = PresetCardLayout.cards(WIDE, 5, 40, Breakpoint.WIDE);

        for (int i = 0; i < still.size(); i++) {
            assertEquals(still.get(i).y() - 40, scrolled.get(i).y());
            assertEquals(still.get(i).x(), scrolled.get(i).x());
        }
    }

    @Test
    void linesStackOnTheCardPitchAndStayInside() {
        Rect card = PresetCardLayout.cards(WIDE, 1, 0, Breakpoint.WIDE).get(0);
        int capacity = PresetCardLayout.lineCapacity(card);

        assertTrue(capacity >= 4, "a full-height card must hold the four rows");
        assertEquals(PresetCardLayout.line(card, 0).y() + PresetCardLayout.LINE_PITCH,
                PresetCardLayout.line(card, 1).y());
        assertTrue(PresetCardLayout.line(card, capacity - 1).bottom() <= card.bottom());
        assertEquals(card.x() + PresetCardLayout.CARD_PAD_X, PresetCardLayout.line(card, 0).x());
    }

    @Test
    void theTagHugsTheRightEdgeOfTheFirstLine() {
        Rect card = PresetCardLayout.cards(WIDE, 1, 0, Breakpoint.WIDE).get(0);
        Rect tag = PresetCardLayout.tag(card, 60);

        assertEquals(PresetCardLayout.line(card, 0).right(), tag.right());
        assertEquals(60, tag.width());
        assertEquals(PresetCardLayout.TAG_HEIGHT, tag.height());
    }

    @Test
    void aTagWiderThanTheCardIsDropped() {
        Rect card = PresetCardLayout.cards(NARROW, 1, 0, Breakpoint.COMPACT).get(0);

        assertTrue(PresetCardLayout.tag(card, 9000).isEmpty());
        assertTrue(PresetCardLayout.tag(card, 0).isEmpty());
    }

    @Test
    void contentHeightMatchesWhereTheLastCardEnds() {
        for (int count = 1; count <= 6; count++) {
            List<Rect> cards = PresetCardLayout.cards(WIDE, count, 0, Breakpoint.WIDE);
            int height = PresetCardLayout.contentHeight(count, Breakpoint.WIDE,
                    WIDE.width() - PresetCardLayout.PAD_X * 2);
            assertEquals(cards.get(count - 1).bottom() - WIDE.y(), height,
                    "count " + count + " must reserve exactly what it draws");
        }
        assertEquals(0, PresetCardLayout.contentHeight(0, Breakpoint.WIDE, 654));
    }

    @Test
    void degenerateShellsProduceNothingRatherThanNegativeRects() {
        assertTrue(PresetCardLayout.cards(Rect.EMPTY, 4, 0, Breakpoint.WIDE).isEmpty());
        assertTrue(PresetCardLayout.cards(new Rect(0, 0, 20, 200), 4, 0, Breakpoint.COMPACT).isEmpty());
        assertTrue(PresetCardLayout.line(new Rect(0, 0, 8, 62), 0).isEmpty());
        assertEquals(0, PresetCardLayout.lineCapacity(Rect.EMPTY));
    }

    @Test
    void rejectsInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> PresetCardLayout.cards(null, 1, 0, Breakpoint.WIDE));
        assertThrows(IllegalArgumentException.class, () -> PresetCardLayout.cards(WIDE, -1, 0, Breakpoint.WIDE));
        assertThrows(IllegalArgumentException.class, () -> PresetCardLayout.cards(WIDE, 1, -1, Breakpoint.WIDE));
        assertThrows(IllegalArgumentException.class, () -> PresetCardLayout.cardHeight(null));
        assertThrows(IllegalArgumentException.class, () -> PresetCardLayout.line(null, 0));
        assertThrows(IllegalArgumentException.class, () -> PresetCardLayout.line(WIDE, -1));
        assertThrows(IllegalArgumentException.class, () -> PresetCardLayout.tag(WIDE, -1));
        assertThrows(IllegalArgumentException.class,
                () -> PresetCardLayout.contentHeight(-1, Breakpoint.WIDE, 654));
    }
}
