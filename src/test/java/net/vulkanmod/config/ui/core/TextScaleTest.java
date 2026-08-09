package net.vulkanmod.config.ui.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TextScaleTest {
    @Test
    void theSmallSizeLandsOnWholePhysicalPixelsAtEveryGuiScale() {
        for (int guiScale = 2; guiScale <= 8; guiScale++) {
            float small = TextScale.small(guiScale);
            assertTrue(TextScale.isCrisp(guiScale, small),
                    "gui scale " + guiScale + " produced a blurry " + small);
            assertEquals(guiScale - 1, TextScale.physicalPixels(guiScale, small),
                    "small text must sit exactly one physical pixel below normal");
        }
    }

    @Test
    void theSmallSizeIsAlwaysSmallerButNeverVanishes() {
        for (int guiScale = 2; guiScale <= 8; guiScale++) {
            float small = TextScale.small(guiScale);
            assertTrue(small < 1.0f, "gui scale " + guiScale + " did not shrink");
            assertTrue(small >= 0.5f, "gui scale " + guiScale + " shrank past half");
        }
    }

    @Test
    void atGuiScaleOneThereIsNoSmallerSizeAndWeSaySoRatherThanBlur() {
        assertEquals(1.0f, TextScale.small(1));
        assertEquals(1.0f, TextScale.small(1.5));
    }

    @Test
    void aFractionalGuiScaleStillYieldsACrispResult() {
        assertTrue(TextScale.isCrisp(4.0, TextScale.small(4.0)));
        assertEquals(3, TextScale.physicalPixels(4.0, TextScale.small(4.0)));
    }

    @Test
    void anArbitraryScaleIsReportedAsBlurryRatherThanAccepted() {
        assertFalse(TextScale.isCrisp(4.0, 0.6f));
        assertFalse(TextScale.isCrisp(3.0, 0.5f));
        assertTrue(TextScale.isCrisp(4.0, 0.5f));
    }

    @Test
    void lineHeightAndWidthsFollowTheScale() {
        assertEquals(9, TextScale.lineHeight(1.0f));
        assertEquals(7, TextScale.lineHeight(0.75f));
        assertEquals(1, TextScale.lineHeight(0.01f));
        assertEquals(75, TextScale.scaled(100, 0.75f));
    }

    @Test
    void rejectsInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> TextScale.small(0));
        assertThrows(IllegalArgumentException.class, () -> TextScale.small(-2));
        assertThrows(IllegalArgumentException.class, () -> TextScale.lineHeight(0));
        assertThrows(IllegalArgumentException.class, () -> TextScale.scaled(10, -1));
        assertThrows(IllegalArgumentException.class, () -> TextScale.physicalPixels(4, 0));
    }
}
