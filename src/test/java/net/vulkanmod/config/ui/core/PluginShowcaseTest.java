package net.vulkanmod.config.ui.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PluginShowcaseTest {
    private static final Rect FRAME = new Rect(50, 40, 432, 243);

    @Test
    void theFrameKeepsSixteenNinthsUntilTheCapAndNeverBelowIt() {
        assertEquals(243, PluginShowcase.height(432));
        assertEquals(144, PluginShowcase.height(256));
        assertEquals(PluginShowcase.MAX_H, PluginShowcase.height(2000), "a wide window caps the frame");
        assertEquals(0, PluginShowcase.height(0));
    }

    @Test
    void aPixelPerfectBannerIsUsedWholeAndUncropped() {
        PluginShowcase.Crop crop = PluginShowcase.cover(432, 243, 256, 144);
        assertEquals(0, crop.u());
        assertEquals(0, crop.v());
        assertEquals(256, crop.uw());
        assertEquals(144, crop.vh());
    }

    @Test
    void aClassicImageAtTheWrongRatioIsCentreCroppedNeverStretched() {
        PluginShowcase.Crop wide = PluginShowcase.cover(432, 243, 1920, 1080);
        assertEquals(1920, wide.uw());
        assertEquals(1080, wide.vh());

        PluginShowcase.Crop square = PluginShowcase.cover(432, 243, 800, 800);
        assertEquals(800, square.uw());
        assertEquals(450, square.vh(), "a square source loses height, not shape");
        assertEquals(0, square.u());
        assertEquals(175, square.v(), "the crop is centred vertically");

        PluginShowcase.Crop tall = PluginShowcase.cover(432, 243, 600, 1200);
        assertEquals(600, tall.uw());
        assertEquals(338, tall.vh(), 1);
        assertTrue(tall.v() > 0 && tall.u() == 0);
    }

    @Test
    void theCropAlwaysMatchesTheFrameShapeSoNothingEverDistorts() {
        for (int[] tex : new int[][] {{256, 144}, {1920, 1080}, {800, 800}, {100, 700}, {3, 5}}) {
            PluginShowcase.Crop crop = PluginShowcase.cover(432, 243, tex[0], tex[1]);
            if (Math.min(tex[0], tex[1]) >= 16) {
                float frameRatio = 432 / 243.0f;
                float cropRatio = crop.uw() / (float) crop.vh();
                assertTrue(Math.abs(frameRatio - cropRatio) < 0.05f,
                        tex[0] + "x" + tex[1] + " cropped to ratio " + cropRatio);
            }
            assertTrue(crop.u() >= 0 && crop.v() >= 0);
            assertTrue(crop.u() + crop.uw() <= tex[0]);
            assertTrue(crop.v() + crop.vh() <= tex[1]);
        }
    }

    @Test
    void theSlotsReadBottomUpWithoutOverlapAndStayInsideTheFrame() {
        PluginShowcase.Slots slots = PluginShowcase.slots(FRAME);
        assertTrue(slots.icon().bottom() <= FRAME.bottom() - PluginShowcase.PAD + 1);
        assertTrue(slots.button().right() <= FRAME.right() - PluginShowcase.PAD + 1);
        assertTrue(slots.title().y() < slots.byline().y());
        assertTrue(slots.byline().y() < slots.desc().y());
        assertTrue(slots.desc().bottom() <= slots.tags().y());
        assertTrue(slots.title().x() >= slots.icon().right());
        assertTrue(slots.tags().right() <= slots.button().x());
        assertTrue(slots.title().y() >= FRAME.y() + PluginShowcase.PAD);
    }

    @Test
    void aTightFrameGivesUpLinesBeforeItGivesUpTheTitle() {
        PluginShowcase.Slots tight = PluginShowcase.slots(new Rect(0, 0, 260, 100));
        assertTrue(!tight.title().isEmpty(), "the title must survive any playable frame");
        assertTrue(tight.desc().height() <= PluginShowcase.SMALL_LINE,
                "a squat frame keeps at most one description line");
        assertTrue(PluginShowcase.slots(new Rect(0, 0, 90, 40)).title().isEmpty(),
                "an impossible frame yields empty slots, not overlap");
        assertThrows(IllegalArgumentException.class, () -> PluginShowcase.slots(null));
    }

    @Test
    void expandingAPluginPushesTheRowsBelowItAndGrowsThePage() {
        Rect content = new Rect(120, 40, 520, 600);
        PluginPageLayout.Page flat = PluginPageLayout.page(content, 3, 2, 0, Breakpoint.WIDE);
        PluginPageLayout.Page open = PluginPageLayout.page(content, 3, 2, 0, Breakpoint.WIDE, 1);

        assertTrue(flat.showcase().isEmpty());
        assertTrue(!open.showcase().isEmpty());
        assertEquals(flat.plugins().rows().get(0), open.plugins().rows().get(0),
                "rows above the showcase do not move");
        assertEquals(flat.plugins().rows().get(1), open.plugins().rows().get(1));
        assertTrue(open.plugins().rows().get(2).y() > flat.plugins().rows().get(2).y(),
                "the row below the showcase must give way");
        assertTrue(open.showcase().y() > open.plugins().rows().get(1).bottom());
        assertTrue(open.showcase().bottom() <= open.plugins().rows().get(2).y());
        assertTrue(open.height() > flat.height());
        assertTrue(open.mods().heading().y() > flat.mods().heading().y());
        assertTrue(PluginPageLayout.maxScroll(new Rect(0, 0, 520, 200), 3, 2, Breakpoint.WIDE, true)
                > PluginPageLayout.maxScroll(new Rect(0, 0, 520, 200), 3, 2, Breakpoint.WIDE, false));
    }
}
