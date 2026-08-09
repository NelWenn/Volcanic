package net.vulkanmod.config.ui.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PresetCardLayoutTest {
    private static final Rect WIDE = new Rect(132, 32, 700, 460);
    private static final Rect NARROW = new Rect(0, 32, 420, 240);

    @Test
    void fivePresetsSitThreeAboveTwoWhenThereIsRoom() {
        assertArrayEquals(new int[] {3, 2}, PresetCardLayout.rowPattern(5, 660));
    }

    @Test
    void theBottomRowIsCentredUnderTheTop() {
        List<Rect> cards = PresetCardLayout.cards(WIDE, 5, 0, Breakpoint.WIDE);
        assertEquals(5, cards.size());

        int topLeft = cards.get(0).x();
        int topRight = cards.get(2).right();
        int bottomLeft = cards.get(3).x();
        int bottomRight = cards.get(4).right();

        assertEquals(topLeft + topRight, bottomLeft + bottomRight,
                "the two-card row must share the three-card row's centre");
        assertTrue(bottomLeft > topLeft, "the bottom row is inset");
    }

    @Test
    void theThreeCardsOnTheTopRowAreEqualAndAdjacent() {
        List<Rect> cards = PresetCardLayout.cards(WIDE, 5, 0, Breakpoint.WIDE);

        assertEquals(cards.get(0).width(), cards.get(1).width());
        assertEquals(cards.get(1).width(), cards.get(2).width());
        assertEquals(cards.get(0).right() + PresetCardLayout.GAP, cards.get(1).x());
        assertEquals(cards.get(1).right() + PresetCardLayout.GAP, cards.get(2).x());
        assertEquals(cards.get(0).y(), cards.get(2).y());
        assertEquals(cards.get(0).bottom() + PresetCardLayout.GAP, cards.get(3).y());
    }

    @Test
    void aNarrowShellFallsBackRatherThanSquashingTheCards() {
        assertArrayEquals(new int[] {2, 2, 1}, PresetCardLayout.rowPattern(5, 420));
        assertArrayEquals(new int[] {1, 1, 1, 1, 1}, PresetCardLayout.rowPattern(5, 200));

        List<Rect> cards = PresetCardLayout.cards(NARROW, 5, 0, Breakpoint.COMPACT);
        assertEquals(PresetCardLayout.CARD_HEIGHT_COMPACT, cards.get(0).height());
        for (Rect card : cards) {
            assertTrue(card.width() >= 118, "a card never gets narrower than it can render");
        }
    }

    @Test
    void fourPresetsMakeTwoEvenRowsRatherThanThreePlusOne() {
        assertArrayEquals(new int[] {3, 1}, PresetCardLayout.rowPattern(4, 660));
    }

    @Test
    void everyCardStaysInsideTheContent() {
        for (int count = 1; count <= 5; count++) {
            for (Rect card : PresetCardLayout.cards(WIDE, count, 0, Breakpoint.WIDE)) {
                assertTrue(card.x() >= WIDE.x() + PresetCardLayout.PAD_X);
                assertTrue(card.right() <= WIDE.right() - PresetCardLayout.PAD_X);
            }
        }
    }

    @Test
    void scrollMovesTheCards() {
        assertEquals(PresetCardLayout.cards(WIDE, 5, 0, Breakpoint.WIDE).get(0).y() - 30,
                PresetCardLayout.cards(WIDE, 5, 30, Breakpoint.WIDE).get(0).y());
    }

    @Test
    void contentHeightCoversTheLastCardAndTheBottomMargin() {
        int usable = WIDE.width() - PresetCardLayout.PAD_X * 2;
        List<Rect> cards = PresetCardLayout.cards(WIDE, 5, 0, Breakpoint.WIDE);
        int height = PresetCardLayout.contentHeight(5, Breakpoint.WIDE, usable);

        assertEquals(cards.get(4).bottom() - WIDE.y() + PresetCardLayout.BOTTOM, height);
    }

    @Test
    void theBarsFillInProportionAndNeverOverflow() {
        Rect card = PresetCardLayout.cards(WIDE, 5, 0, Breakpoint.WIDE).get(0);
        Rect track = PresetCardLayout.bar(card, 0, 20);

        assertTrue(track.right() <= card.right() - PresetCardLayout.CARD_PAD);
        assertTrue(PresetCardLayout.barFill(track, 0, 5).isEmpty());
        assertEquals(track.width(), PresetCardLayout.barFill(track, 5, 5).width());
        assertTrue(PresetCardLayout.barFill(track, 2, 5).width()
                < PresetCardLayout.barFill(track, 4, 5).width());
        assertEquals(track.width(), PresetCardLayout.barFill(track, 99, 5).width(), "clamped, not overflowing");
    }

    @Test
    void degenerateShellsProduceNothing() {
        assertTrue(PresetCardLayout.cards(Rect.EMPTY, 5, 0, Breakpoint.WIDE).isEmpty());
        assertTrue(PresetCardLayout.bar(new Rect(0, 0, 40, 80), 0, 20).isEmpty());
        assertEquals(0, PresetCardLayout.rowPattern(0, 660).length);
    }

    @Test
    void rejectsInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> PresetCardLayout.cards(null, 1, 0, Breakpoint.WIDE));
        assertThrows(IllegalArgumentException.class, () -> PresetCardLayout.cards(WIDE, -1, 0, Breakpoint.WIDE));
        assertThrows(IllegalArgumentException.class, () -> PresetCardLayout.cards(WIDE, 1, -1, Breakpoint.WIDE));
        assertThrows(IllegalArgumentException.class, () -> PresetCardLayout.cardHeight(null));
        assertThrows(IllegalArgumentException.class, () -> PresetCardLayout.bar(null, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> PresetCardLayout.bar(WIDE, -1, 0));
        assertThrows(IllegalArgumentException.class, () -> PresetCardLayout.barFill(WIDE, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> PresetCardLayout.rowPattern(-1, 660));
    }

    @Test
    void nothingInsideACardOverlapsAnythingElse() {
        for (Breakpoint bp : Breakpoint.values()) {
            Rect content = bp == Breakpoint.COMPACT ? NARROW : WIDE;
            for (Rect card : PresetCardLayout.cards(content, 5, 0, bp)) {
                PresetCardLayout.Slots s = PresetCardLayout.slots(card, bp != Breakpoint.COMPACT);
                Rect[] stack = {s.name(), s.blurb(), s.changes(), s.framesBar(), s.looksBar(), s.measured()};
                Rect previous = null;
                for (Rect r : stack) {
                    if (r.isEmpty()) {
                        continue;
                    }
                    assertTrue(r.y() >= card.y(), bp + ": a slot escaped the top");
                    assertTrue(r.bottom() <= card.bottom(), bp + ": a slot escaped the bottom");
                    assertTrue(r.right() <= card.right(), bp + ": a slot escaped the right edge");
                    if (previous != null) {
                        assertTrue(r.y() >= previous.bottom() - 1,
                                bp + ": " + r + " overlaps " + previous);
                    }
                    previous = r;
                }
            }
        }
    }

    @Test
    void aShortCardDropsTheChangesLineRatherThanStackingOnTheBars() {
        Rect tall = new Rect(0, 0, 220, PresetCardLayout.CARD_HEIGHT);
        Rect squat = new Rect(0, 0, 220, PresetCardLayout.CARD_HEIGHT_COMPACT);

        assertFalse(PresetCardLayout.slots(tall, true).changes().isEmpty());
        assertTrue(PresetCardLayout.slots(squat, false).changes().isEmpty());
        assertFalse(PresetCardLayout.slots(squat, false).framesBar().isEmpty(),
                "the bars are the point of the card and survive");
    }

    @Test
    void theAccentStripeRunsTheFullHeightAndTextClearsIt() {
        Rect card = new Rect(10, 20, 220, PresetCardLayout.CARD_HEIGHT);
        PresetCardLayout.Slots s = PresetCardLayout.slots(card, true);

        assertEquals(card.y(), s.accent().y());
        assertEquals(card.height(), s.accent().height());
        assertEquals(PresetCardLayout.ACCENT_WIDTH, s.accent().width());
        assertTrue(s.name().x() >= s.accent().right(), "the name must not sit on the stripe");
    }

    @Test
    void slotsOfADegenerateCardAreAllEmpty() {
        PresetCardLayout.Slots s = PresetCardLayout.slots(new Rect(0, 0, 4, 80), true);
        assertTrue(s.name().isEmpty() && s.framesBar().isEmpty() && s.accent().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> PresetCardLayout.slots(null, true));
    }

    @Test
    void theHeaderStaysAtTheTopWhileTheCardsCentreBelowIt() {
        Rect content = new Rect(132, 32, 700, 600);
        PresetCardLayout.Page page = PresetCardLayout.page(content, 5, 0, Breakpoint.WIDE);

        assertTrue(page.centred());
        assertEquals(content.y() + PresetCardLayout.OVERVIEW_MARGIN, page.header().y(),
                "the title is pinned to the top, not floated with the cards");

        int above = page.cards().get(0).y() - (page.header().bottom() + PresetCardLayout.HEADER_GAP);
        int below = content.bottom() - PresetCardLayout.OVERVIEW_MARGIN - page.suggestion().bottom();
        assertTrue(Math.abs(above - below) <= 2,
                "the cards centre in what is left: " + above + " vs " + below);
    }

    @Test
    void aShortShellStopsCentringAndScrollsFromTheTop() {
        PresetCardLayout.Page page = PresetCardLayout.page(new Rect(0, 32, 420, 150), 5, 0, Breakpoint.COMPACT);

        assertFalse(page.centred());
        assertEquals(32 + PresetCardLayout.OVERVIEW_MARGIN, page.header().y());
        assertEquals(32 + PresetCardLayout.OVERVIEW_MARGIN - 40,
                PresetCardLayout.page(new Rect(0, 32, 420, 150), 5, 40, Breakpoint.COMPACT).header().y());
    }

    @Test
    void theHeaderTheGridAndTheSuggestionNeverOverlap() {
        for (int height : new int[] {150, 300, 460, 700}) {
            PresetCardLayout.Page page = PresetCardLayout.page(new Rect(132, 32, 700, height), 5, 0,
                    Breakpoint.WIDE);
            assertTrue(page.cards().get(0).y() >= page.header().bottom(), "height " + height);
            Rect last = page.cards().get(page.cards().size() - 1);
            assertTrue(page.suggestion().y() >= last.bottom(), "height " + height);
        }
    }

    @Test
    void anEmptyPageProducesNothing() {
        PresetCardLayout.Page page = PresetCardLayout.page(new Rect(0, 0, 700, 400), 0, 0, Breakpoint.WIDE);
        assertTrue(page.header().isEmpty() && page.cards().isEmpty() && page.suggestion().isEmpty());
        assertTrue(PresetCardLayout.page(Rect.EMPTY, 5, 0, Breakpoint.WIDE).cards().isEmpty());
    }
}
