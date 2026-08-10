package net.vulkanmod.config.ui.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PresetCardLayoutTest {
    private static final Rect WIDE = new Rect(132, 32, 700, 460);
    private static final Rect NARROW = new Rect(0, 32, 420, 240);

    @Test
    void fivePresetsStandSideBySideWhenThereIsRoomForFive() {
        assertArrayEquals(new int[] {5}, PresetCardLayout.rowPattern(5, 660));
        assertArrayEquals(new int[] {3, 2}, PresetCardLayout.rowPattern(5, 300));
    }

    @Test
    void aCardIsAlwaysTallerThanItIsWideHoweverMuchRoomThereIs() {
        for (int width : new int[] {300, 480, 700, 1200, 2400}) {
            List<Rect> cards = PresetCardLayout.cards(new Rect(0, 0, width, 700), 5, 0, Breakpoint.WIDE);
            for (Rect card : cards) {
                assertTrue(card.height() > card.width(),
                        "at " + width + "px a card was " + card.width() + "x" + card.height());
                assertTrue(card.width() <= PresetCardLayout.MAX_CARD);
            }
        }
    }

    @Test
    void theBottomRowIsCentredUnderTheTop() {
        List<Rect> cards = PresetCardLayout.cards(new Rect(132, 32, 330, 460), 5, 0, Breakpoint.WIDE);
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
        List<Rect> cards = PresetCardLayout.cards(new Rect(132, 32, 330, 460), 5, 0, Breakpoint.WIDE);

        assertEquals(cards.get(0).width(), cards.get(1).width());
        assertEquals(cards.get(1).width(), cards.get(2).width());
        assertEquals(cards.get(0).right() + PresetCardLayout.GAP, cards.get(1).x());
        assertEquals(cards.get(1).right() + PresetCardLayout.GAP, cards.get(2).x());
        assertEquals(cards.get(0).y(), cards.get(2).y());
        assertEquals(cards.get(0).bottom() + PresetCardLayout.GAP, cards.get(3).y());
    }

    @Test
    void aNarrowShellFallsBackRatherThanSquashingTheCards() {
        assertArrayEquals(new int[] {4, 1}, PresetCardLayout.rowPattern(5, 420));
        assertArrayEquals(new int[] {2, 2, 1}, PresetCardLayout.rowPattern(5, 200));
        assertArrayEquals(new int[] {1, 1, 1, 1, 1}, PresetCardLayout.rowPattern(5, 80));

        List<Rect> cards = PresetCardLayout.cards(NARROW, 5, 0, Breakpoint.COMPACT);
        assertEquals(PresetCardLayout.CARD_HEIGHT_COMPACT, cards.get(0).height());
        for (Rect card : cards) {
            assertTrue(card.width() >= 80, "a card never gets narrower than it can render");
        }
    }

    @Test
    void fourPresetsMakeOneRowWhenTheyFitAndSplitEvenlyWhenTheyDoNot() {
        assertArrayEquals(new int[] {4}, PresetCardLayout.rowPattern(4, 660));
        assertArrayEquals(new int[] {3, 1}, PresetCardLayout.rowPattern(4, 300));
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
    void degenerateShellsProduceNothing() {
        assertTrue(PresetCardLayout.cards(Rect.EMPTY, 5, 0, Breakpoint.WIDE).isEmpty());
        assertEquals(0, PresetCardLayout.rowPattern(0, 660).length);
    }

    @Test
    void rejectsInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> PresetCardLayout.cards(null, 1, 0, Breakpoint.WIDE));
        assertThrows(IllegalArgumentException.class, () -> PresetCardLayout.cards(WIDE, -1, 0, Breakpoint.WIDE));
        assertThrows(IllegalArgumentException.class, () -> PresetCardLayout.cards(WIDE, 1, -1, Breakpoint.WIDE));
        assertThrows(IllegalArgumentException.class, () -> PresetCardLayout.cardHeight(null));
        assertThrows(IllegalArgumentException.class, () -> PresetCardLayout.rowPattern(-1, 660));
        assertThrows(IllegalArgumentException.class, () -> PresetCardLayout.slots(null, true));
        assertThrows(IllegalArgumentException.class, () -> PresetCardLayout.segment(null, 0));
        assertThrows(IllegalArgumentException.class,
                () -> PresetCardLayout.segment(new Rect(0, 0, 80, 6), PresetCardLayout.SEGMENTS));
        assertThrows(IllegalArgumentException.class, () -> PresetCardLayout.segment(WIDE, -1));
    }

    @Test
    void theCardReadsTopToBottomWithNothingOverlapping() {
        for (Breakpoint bp : Breakpoint.values()) {
            Rect content = bp == Breakpoint.COMPACT ? NARROW : WIDE;
            for (Rect card : PresetCardLayout.cards(content, 5, 0, bp)) {
                PresetCardLayout.Slots s = PresetCardLayout.slots(card, bp != Breakpoint.COMPACT);
                Rect[] stack = {s.glyph(), s.name(), s.tier(), s.rule(), s.blurb(),
                        s.framesLabel(), s.framesRow(), s.looksLabel(), s.looksRow(), s.strip()};
                Rect previous = null;
                for (Rect r : stack) {
                    if (r.isEmpty()) {
                        continue;
                    }
                    assertTrue(r.y() >= card.y(), bp + ": a slot escaped the top");
                    assertTrue(r.bottom() <= card.bottom(), bp + ": a slot escaped the bottom");
                    assertTrue(r.right() <= card.right(), bp + ": a slot escaped the right edge");
                    if (previous != null) {
                        assertTrue(r.y() >= previous.bottom(),
                                bp + ": " + r + " overlaps " + previous);
                    }
                    previous = r;
                }
            }
        }
    }

    @Test
    void theStatusStripSpansTheCardFootSoTheBadgeNeverFightsTheTitle() {
        Rect card = new Rect(10, 20, 104, PresetCardLayout.CARD_HEIGHT);
        PresetCardLayout.Slots s = PresetCardLayout.slots(card, true);

        assertEquals(card.bottom() - 1, s.strip().bottom());
        assertEquals(card.width() - 2, s.strip().width());
        assertEquals(PresetCardLayout.STRIP_H, s.strip().height());
        assertTrue(s.strip().y() > s.looksRow().bottom(), "the meters clear the strip");
        assertTrue(s.name().bottom() < s.strip().y(), "the name lives nowhere near the badge now");
    }

    @Test
    void theMetersAreChunkyEnoughToReadAsBlocksNotHairlines() {
        Rect card = new Rect(0, 0, 104, PresetCardLayout.CARD_HEIGHT);
        PresetCardLayout.Slots s = PresetCardLayout.slots(card, true);

        assertTrue(s.framesRow().height() >= 5, "a meter row must be chunky, not a hairline");
        int last = -1;
        for (int index = 0; index < PresetCardLayout.SEGMENTS; index++) {
            Rect seg = PresetCardLayout.segment(s.framesRow(), index);
            assertFalse(seg.isEmpty());
            assertTrue(seg.width() >= 10, "segment " + index + " is " + seg.width() + "px wide");
            assertTrue(seg.x() > last, "segments must march left to right without touching");
            assertTrue(seg.right() <= s.framesRow().right());
            last = seg.right();
        }
    }

    @Test
    void everyCardShowsGlyphNameMetersAndBlurbAtBothHeights() {
        for (int height : new int[] {PresetCardLayout.CARD_HEIGHT, PresetCardLayout.CARD_HEIGHT_COMPACT}) {
            PresetCardLayout.Slots s = PresetCardLayout.slots(new Rect(0, 0, 104, height),
                    height >= PresetCardLayout.CARD_HEIGHT);
            assertFalse(s.glyph().isEmpty(), height + ": no glyph");
            assertFalse(s.name().isEmpty(), height + ": no name");
            assertFalse(s.tier().isEmpty(), height + ": no tier");
            assertFalse(s.blurb().isEmpty(), height + ": no room left for the description");
            assertFalse(s.framesRow().isEmpty(), height + ": no meters");
            assertEquals(PresetCardLayout.GLYPH, s.glyph().width(), "the glyph scales its art twice");
        }
    }

    @Test
    void theAccentStripeRunsTheFullHeightAndTextClearsIt() {
        Rect card = new Rect(10, 20, 104, PresetCardLayout.CARD_HEIGHT);
        PresetCardLayout.Slots s = PresetCardLayout.slots(card, true);

        assertEquals(card.y(), s.accent().y());
        assertEquals(card.height(), s.accent().height());
        assertEquals(PresetCardLayout.ACCENT_WIDTH, s.accent().width());
        assertTrue(s.name().x() >= s.accent().right(), "the name must not sit on the stripe");
    }

    @Test
    void slotsOfADegenerateCardAreAllEmpty() {
        PresetCardLayout.Slots s = PresetCardLayout.slots(new Rect(0, 0, 4, 80), true);
        assertTrue(s.name().isEmpty() && s.framesRow().isEmpty() && s.accent().isEmpty());
        PresetCardLayout.Slots squat = PresetCardLayout.slots(new Rect(0, 0, 104, 40), false);
        assertTrue(squat.name().isEmpty(), "a card too short for its own layout gives up whole");
    }

    @Test
    void theHeaderStaysAtTheTopWhileTheCardsCentreBelowIt() {
        Rect content = new Rect(132, 32, 700, 600);
        PresetCardLayout.Page page = PresetCardLayout.page(content, 5, 0, Breakpoint.WIDE);

        assertTrue(page.centred());
        assertEquals(content.y() + PresetCardLayout.OVERVIEW_MARGIN, page.legend().y(),
                "the title is pinned to the top, not floated with the cards");

        int above = page.cards().get(0).y() - (page.legend().bottom() + PresetCardLayout.LEGEND_GAP);
        int below = content.bottom() - PresetCardLayout.OVERVIEW_MARGIN - page.suggestion().bottom();
        assertTrue(Math.abs(above - below) <= 2,
                "the cards centre in what is left: " + above + " vs " + below);
    }

    @Test
    void aShortShellStopsCentringAndScrollsFromTheTop() {
        PresetCardLayout.Page page = PresetCardLayout.page(new Rect(0, 32, 420, 150), 5, 0, Breakpoint.COMPACT);

        assertFalse(page.centred());
        assertEquals(32 + PresetCardLayout.OVERVIEW_MARGIN, page.legend().y());
        assertEquals(32 + PresetCardLayout.OVERVIEW_MARGIN - 40,
                PresetCardLayout.page(new Rect(0, 32, 420, 150), 5, 40, Breakpoint.COMPACT).legend().y());
    }

    @Test
    void theHeaderTheGridAndTheSuggestionNeverOverlap() {
        for (int height : new int[] {150, 300, 460, 700}) {
            PresetCardLayout.Page page = PresetCardLayout.page(new Rect(132, 32, 700, height), 5, 0,
                    Breakpoint.WIDE);
            assertTrue(page.cards().get(0).y() >= page.legend().bottom(), "height " + height);
            Rect last = page.cards().get(page.cards().size() - 1);
            assertTrue(page.suggestion().y() >= last.bottom(), "height " + height);
        }
    }

    @Test
    void anEmptyPageProducesNothing() {
        PresetCardLayout.Page page = PresetCardLayout.page(new Rect(0, 0, 700, 400), 0, 0, Breakpoint.WIDE);
        assertTrue(page.legend().isEmpty() && page.cards().isEmpty() && page.suggestion().isEmpty());
        assertTrue(PresetCardLayout.page(Rect.EMPTY, 5, 0, Breakpoint.WIDE).cards().isEmpty());
    }
}
