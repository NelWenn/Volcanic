package net.vulkanmod.config.ui.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TooltipLayoutTest {
    private static final Rect SCREEN = new Rect(0, 0, 400, 300);

    @Test
    void sitsBelowTheAnchorWhenThereIsRoom() {
        Rect anchor = new Rect(50, 40, 20, 10);
        Rect box = TooltipLayout.placeBox(anchor, 100, 20, SCREEN);

        assertEquals(anchor.x(), box.x());
        assertEquals(anchor.bottom() + TooltipLayout.GAP, box.y());
        assertEquals(100, box.width());
        assertEquals(20, box.height());
    }

    @Test
    void flipsAboveTheAnchorWhenThereIsNoRoomBelow() {
        Rect anchor = new Rect(50, SCREEN.bottom() - 20, 20, 10);
        Rect box = TooltipLayout.placeBox(anchor, 100, 40, SCREEN);

        assertEquals(anchor.y() - TooltipLayout.GAP - box.height(), box.y());
        assertTrue(box.y() >= SCREEN.y() + TooltipLayout.MARGIN);
    }

    @Test
    void staysBelowWhenTheLastPixelStillFits() {
        int height = 20;
        int top = SCREEN.bottom() - TooltipLayout.MARGIN - height - TooltipLayout.GAP - 10;
        Rect tight = new Rect(0, top, 10, 10);

        assertEquals(tight.bottom() + TooltipLayout.GAP,
                TooltipLayout.placeBox(tight, 40, height, SCREEN).y());
    }

    @Test
    void clampsToBothEdgesOfTheScreen() {
        Rect right = TooltipLayout.placeBox(new Rect(SCREEN.right() - 10, 40, 10, 10), 100, 20, SCREEN);
        assertEquals(SCREEN.right() - TooltipLayout.MARGIN, right.right());

        Rect left = TooltipLayout.placeBox(new Rect(-30, 40, 10, 10), 100, 20, SCREEN);
        assertEquals(SCREEN.x() + TooltipLayout.MARGIN, left.x());
    }

    @Test
    void clampsVerticallyWhenNeitherPlacementFits() {
        Rect screen = new Rect(0, 0, 400, 60);
        Rect box = TooltipLayout.placeBox(new Rect(10, 25, 10, 10), 100, 40, screen);

        assertTrue(box.y() >= screen.y() + TooltipLayout.MARGIN, "box escaped the top of the screen");
        assertTrue(box.bottom() <= screen.bottom() - TooltipLayout.MARGIN,
                "box escaped the bottom of the screen");
    }

    @Test
    void aBoxTooLargeForTheScreenIsCutDownToItRatherThanOverflowing() {
        Rect box = TooltipLayout.placeBox(new Rect(10, 10, 10, 10), 10_000, 10_000, SCREEN);

        assertEquals(SCREEN.width() - TooltipLayout.MARGIN * 2, box.width());
        assertEquals(SCREEN.height() - TooltipLayout.MARGIN * 2, box.height());
    }

    @Test
    void aScreenWithNoRoomAtAllProducesNoBox() {
        assertTrue(TooltipLayout.placeBox(new Rect(0, 0, 4, 4), 40, 20,
                new Rect(0, 0, TooltipLayout.MARGIN * 2, 100)).isEmpty());
        assertEquals(0, TooltipLayout.availableHeight(new Rect(0, 0, 10, 10), new Rect(0, 0, 10, 10)));
    }

    @Test
    void availableHeightReportsTheRoomierSide() {
        Rect anchor = new Rect(10, 200, 10, 10);
        int above = anchor.y() - TooltipLayout.GAP - (SCREEN.y() + TooltipLayout.MARGIN);
        assertEquals(above, TooltipLayout.availableHeight(anchor, SCREEN),
                "with the anchor low on screen the room is above it");
    }

    @Test
    void badInputIsAProgrammingErrorRatherThanASilentNoOp() {
        assertThrows(IllegalArgumentException.class,
                () -> TooltipLayout.placeBox(null, 10, 10, SCREEN));
        assertThrows(IllegalArgumentException.class,
                () -> TooltipLayout.placeBox(new Rect(0, 0, 1, 1), 10, 10, null));
        assertThrows(IllegalArgumentException.class,
                () -> TooltipLayout.availableHeight(null, SCREEN));
    }
}
