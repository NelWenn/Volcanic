package net.vulkanmod.config.ui.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TooltipLayoutTest {
    private static final Rect SCREEN = new Rect(0, 0, 400, 300);

    @Test
    void sitsBelowTheAnchorWhenThereIsRoom() {
        Rect anchor = new Rect(40, 60, 120, 27);

        Rect box = TooltipLayout.place(anchor, 100, 1, SCREEN);

        assertEquals(anchor.x(), box.x());
        assertEquals(anchor.bottom() + TooltipLayout.GAP, box.y());
        assertEquals(100 + TooltipLayout.PAD_X * 2, box.width());
        assertEquals(TooltipLayout.TEXT_HEIGHT + TooltipLayout.PAD_Y * 2, box.height());
    }

    @Test
    void growsByOneLinePitchPerExtraLine() {
        Rect one = TooltipLayout.place(new Rect(0, 0, 10, 10), 50, 1, SCREEN);
        Rect three = TooltipLayout.place(new Rect(0, 0, 10, 10), 50, 3, SCREEN);

        assertEquals(one.height() + 2 * (TooltipLayout.TEXT_HEIGHT + TooltipLayout.LINE_GAP), three.height());
    }

    @Test
    void flipsAboveTheAnchorWhenThereIsNoRoomBelow() {
        Rect anchor = new Rect(40, 250, 120, 27);

        Rect box = TooltipLayout.place(anchor, 100, 2, SCREEN);

        assertEquals(anchor.y() - TooltipLayout.GAP - box.height(), box.y());
        assertTrue(box.bottom() <= anchor.y(), "the flipped box must clear the anchor");
    }

    @Test
    void staysBelowWhenTheLastPixelStillFits() {
        Rect anchor = new Rect(0, 0, 10, 10);
        int height = TooltipLayout.place(anchor, 40, 1, SCREEN).height();
        Rect tight = new Rect(0, SCREEN.bottom() - TooltipLayout.MARGIN - height - TooltipLayout.GAP - 10, 10, 10);

        assertEquals(tight.bottom() + TooltipLayout.GAP, TooltipLayout.place(tight, 40, 1, SCREEN).y());
    }

    @Test
    void clampsToTheRightEdgeOfTheScreen() {
        Rect anchor = new Rect(380, 60, 15, 27);

        Rect box = TooltipLayout.place(anchor, 100, 1, SCREEN);

        assertEquals(SCREEN.right() - TooltipLayout.MARGIN, box.right());
    }

    @Test
    void clampsToTheLeftEdgeOfTheScreen() {
        Rect anchor = new Rect(-40, 60, 120, 27);

        Rect box = TooltipLayout.place(anchor, 100, 1, SCREEN);

        assertEquals(SCREEN.x() + TooltipLayout.MARGIN, box.x());
    }

    @Test
    void clampsVerticallyWhenNeitherPlacementFits() {
        Rect screen = new Rect(0, 0, 400, 60);
        Rect anchor = new Rect(40, 20, 120, 20);

        Rect box = TooltipLayout.place(anchor, 100, 3, screen);

        assertTrue(box.y() >= screen.y() + TooltipLayout.MARGIN, "box escaped the top of the screen");
        assertTrue(box.bottom() <= screen.bottom() - TooltipLayout.MARGIN, "box escaped the bottom of the screen");
    }

    @Test
    void shrinksToFitAScreenSmallerThanTheText() {
        Rect screen = new Rect(0, 0, 60, 50);

        Rect box = TooltipLayout.place(new Rect(10, 10, 20, 10), 400, 6, screen);

        assertEquals(screen.width() - TooltipLayout.MARGIN * 2, box.width());
        assertEquals(screen.height() - TooltipLayout.MARGIN * 2, box.height());
        assertTrue(box.x() >= screen.x() && box.right() <= screen.right());
        assertTrue(box.y() >= screen.y() && box.bottom() <= screen.bottom());
    }

    @Test
    void offsetScreensAreClampedToTheirOwnOrigin() {
        Rect screen = new Rect(100, 50, 200, 120);

        Rect box = TooltipLayout.place(new Rect(90, 40, 10, 10), 400, 9, screen);

        assertEquals(screen.x() + TooltipLayout.MARGIN, box.x());
        assertEquals(screen.y() + TooltipLayout.MARGIN, box.y());
    }

    @Test
    void aScreenWithNoRoomLeftGetsNoBox() {
        assertEquals(Rect.EMPTY, TooltipLayout.place(new Rect(0, 0, 10, 10), 20, 1,
                new Rect(0, 0, TooltipLayout.MARGIN * 2, 300)));
        assertEquals(Rect.EMPTY, TooltipLayout.place(new Rect(0, 0, 10, 10), 20, 1,
                new Rect(0, 0, 400, TooltipLayout.MARGIN * 2)));
        assertEquals(Rect.EMPTY, TooltipLayout.place(new Rect(0, 0, 10, 10), 20, 1, Rect.EMPTY));
    }

    @Test
    void everyLineSitsInsideTheBoxItWasSizedFor() {
        Rect box = TooltipLayout.place(new Rect(40, 60, 120, 27), 100, 4, SCREEN);

        assertEquals(box.y() + TooltipLayout.PAD_Y, TooltipLayout.lineTop(box, 0));
        assertTrue(TooltipLayout.lineTop(box, 3) + TooltipLayout.TEXT_HEIGHT <= box.bottom() - TooltipLayout.PAD_Y,
                "the last line overflows the box");
    }

    @Test
    void aBoxHoldsExactlyTheLinesItWasPlacedFor() {
        assertEquals(4, TooltipLayout.lineCapacity(TooltipLayout.place(new Rect(40, 60, 120, 27), 100, 4, SCREEN)));
        assertEquals(1, TooltipLayout.lineCapacity(TooltipLayout.place(new Rect(40, 60, 120, 27), 100, 1, SCREEN)));
        assertEquals(0, TooltipLayout.lineCapacity(Rect.EMPTY));
    }

    @Test
    void aBoxTheScreenShrankHoldsFewerLinesThanItWasAskedFor() {
        Rect screen = new Rect(0, 0, 200, 60);
        Rect box = TooltipLayout.place(new Rect(10, 10, 20, 10), 100, 9, screen);

        int capacity = TooltipLayout.lineCapacity(box);
        assertTrue(capacity < 9, "the shrunken box still claims to hold every line");
        assertTrue(capacity > 0, "the shrunken box holds nothing at all");
        assertTrue(TooltipLayout.lineTop(box, capacity - 1) + TooltipLayout.TEXT_HEIGHT
                <= box.bottom() - TooltipLayout.PAD_Y, "the last line it admits still overflows");
    }

    @Test
    void theTextWidthOnOfferLeavesRoomForBothPaddings() {
        assertEquals(400 - (TooltipLayout.MARGIN + TooltipLayout.PAD_X) * 2, TooltipLayout.maxTextWidth(SCREEN));
        assertEquals(0, TooltipLayout.maxTextWidth(new Rect(0, 0, 4, 4)));
    }

    @Test
    void invalidInputIsRefused() {
        Rect anchor = new Rect(0, 0, 10, 10);

        assertThrows(IllegalArgumentException.class, () -> TooltipLayout.place(null, 10, 1, SCREEN));
        assertThrows(IllegalArgumentException.class, () -> TooltipLayout.place(anchor, 10, 1, null));
        assertThrows(IllegalArgumentException.class, () -> TooltipLayout.place(anchor, -1, 1, SCREEN));
        assertThrows(IllegalArgumentException.class, () -> TooltipLayout.place(anchor, 10, 0, SCREEN));
        assertThrows(IllegalArgumentException.class, () -> TooltipLayout.lineTop(null, 0));
        assertThrows(IllegalArgumentException.class, () -> TooltipLayout.lineTop(anchor, -1));
        assertThrows(IllegalArgumentException.class, () -> TooltipLayout.maxTextWidth(null));
    }

    @Test
    void aBoxSizedToTheAvailableHeightNeverCoversItsAnchorRow() {
        int[][] shells = {{600, 390, 27, 32}, {420, 273, 22, 25}};
        for (int[] shell : shells) {
            Rect content = new Rect(0, 32, shell[0], shell[1] - 66);
            for (int index = 0; index < 8; index++) {
                int top = content.y() + 70 + index * shell[3];
                if (top + shell[2] > content.bottom()) {
                    break;
                }
                Rect row = new Rect(content.x() + 14, top, content.width() - 28, shell[2]);
                int height = TooltipLayout.availableHeight(row, content);
                if (height <= 0) {
                    continue;
                }
                Rect box = TooltipLayout.placeBox(row, 196, height, content);
                assertFalse(box.isEmpty(), "row " + index + " of " + shell[0] + " must place");
                assertTrue(box.bottom() <= row.y() || box.y() >= row.bottom(),
                        "row " + index + " of " + shell[0] + ": the card covered its own row");
                assertTrue(box.y() >= content.y() && box.bottom() <= content.bottom(),
                        "row " + index + " of " + shell[0] + ": the card escaped the content");
            }
        }
    }

    @Test
    void availableHeightTakesTheRoomierSide() {
        Rect screen = new Rect(0, 0, 400, 300);
        assertTrue(TooltipLayout.availableHeight(new Rect(0, 10, 100, 20), screen)
                > TooltipLayout.availableHeight(new Rect(0, 260, 100, 20), screen));
        assertEquals(0, TooltipLayout.availableHeight(new Rect(0, 0, 100, 300), screen));
    }

    @Test
    void placeBoxAgreesWithPlaceOnTheSameGeometry() {
        Rect screen = new Rect(0, 0, 400, 300);
        Rect anchor = new Rect(40, 60, 120, 27);
        Rect viaPlace = TooltipLayout.place(anchor, 100, 3, screen);
        Rect viaBox = TooltipLayout.placeBox(anchor, 100 + TooltipLayout.PAD_X * 2,
                3 * TooltipLayout.TEXT_HEIGHT + 2 * TooltipLayout.LINE_GAP + TooltipLayout.PAD_Y * 2, screen);
        assertEquals(viaPlace, viaBox);
    }
}
