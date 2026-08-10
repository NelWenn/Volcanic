package net.vulkanmod.config.ui.core;

import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoalSceneTest {
    private static final Rect CONTENT = new Rect(120, 60, 900, 520);

    @Test
    void theArtTableIsIntactAndEverySiteFallsInsideTheTexture() {
        assertTrue(CoalArt.siteCount() > 0);
        for (int site = 0; site < CoalArt.siteCount(); site++) {
            assertTrue(CoalArt.siteX(site) >= 0 && CoalArt.siteX(site) < CoalArt.TEX_W,
                    "site " + site + " sits outside the texture horizontally");
            assertTrue(CoalArt.siteY(site) >= 0 && CoalArt.siteY(site) < CoalArt.TEX_H,
                    "site " + site + " sits outside the texture vertically");
            assertTrue(CoalArt.siteHeat(site) > 0);
        }
    }

    @Test
    void theGlowSitesAreDrawnFromTheHottestPartOfTheArtWhereTheLavaShows() {
        for (int site = 0; site < CoalScene.GLOW_SITES; site++) {
            assertTrue(CoalArt.siteY(site) > CoalArt.TEX_H / 2,
                    "site " + site + " is in the dark upper half, it would glow in mid air");
        }
    }

    @Test
    void theTilesCoverTheWholeWidthWithoutLeavingAGap() {
        CoalScene scene = new CoalScene(1L);
        for (int width : new int[] {1, 400, 880, 881, 1920, 3400}) {
            Rect content = new Rect(0, 0, width, 400);
            int tiles = scene.tiles(content);
            assertTrue(tiles * scene.tileWidth() >= width, "width " + width + " left a gap");
            assertTrue((tiles - 1) * scene.tileWidth() < width, "width " + width + " drew a spare tile");
        }
        assertEquals(0, scene.tiles(new Rect(0, 0, 0, 100)));
    }

    @Test
    void everyTileSitsOnTheBottomEdgeOfTheContent() {
        CoalScene scene = new CoalScene(2L);
        for (int tile = 0; tile < scene.tiles(CONTENT); tile++) {
            Rect rect = scene.tileRect(tile, CONTENT);
            assertEquals(CONTENT.bottom(), rect.bottom(), "tile " + tile + " floated off the bottom");
            assertEquals(scene.bedHeight(), rect.height());
            assertEquals(CONTENT.x() + tile * scene.tileWidth(), rect.x());
        }
    }

    @Test
    void aGlowSitsOnTheEmberItBelongsToRatherThanFloatingAboveTheBed() {
        CoalScene scene = new CoalScene(3L);
        for (int site = 0; site < CoalScene.GLOW_SITES; site++) {
            int y = scene.glowY(site, CONTENT);
            assertTrue(y >= CONTENT.bottom() - scene.bedHeight(), "site " + site + " glowed above the bed");
            assertTrue(y <= CONTENT.bottom(), "site " + site + " glowed below the page");
        }
    }

    @Test
    void theGlowBreathesOutOfStepAndNeverGoesOpaque() {
        CoalScene scene = new CoalScene(4L);
        int brightest = 0;
        for (int frame = 0; frame < 400; frame++) {
            scene.advance(16, CONTENT);
            for (int site = 0; site < CoalScene.GLOW_SITES; site++) {
                brightest = Math.max(brightest, scene.glowArgb(site) >>> 24);
            }
        }
        assertTrue(brightest > 20, "the glow never lit up");
        assertTrue(brightest <= 100, "the glow reached alpha " + brightest + ", it would flatten the art");

        long shades = IntStream.range(0, CoalScene.GLOW_SITES)
                .map(site -> scene.glowArgb(site) >>> 24).distinct().count();
        assertTrue(shades >= 10, "only " + shades + " distinct glows, the bed pulses in unison");
    }

    @Test
    void sparksStayInsideThePageAndAlwaysRise() {
        CoalScene scene = new CoalScene(5L);
        scene.advance(16, CONTENT);
        int[] before = IntStream.range(0, CoalScene.PARTICLES)
                .map(index -> scene.particleY(index, CONTENT)).toArray();
        for (int frame = 0; frame < 300; frame++) {
            scene.advance(16, CONTENT);
            for (int index = 0; index < CoalScene.PARTICLES; index++) {
                int x = scene.particleX(index, CONTENT);
                int y = scene.particleY(index, CONTENT);
                assertTrue(x >= CONTENT.x() && x < CONTENT.right(), "a spark left the page sideways");
                assertTrue(y >= CONTENT.y() && y < CONTENT.bottom(), "a spark left the page vertically");
            }
        }
        assertTrue(scene.particleY(0, CONTENT) <= before[0] || scene.particleY(0, CONTENT) > 0);
    }

    @Test
    void sparksStartLowBecauseTheyComeOffTheCoalsRatherThanTheWholePage() {
        CoalScene scene = new CoalScene(6L);
        int low = 0;
        for (int index = 0; index < CoalScene.PARTICLES; index++) {
            if (scene.particleY(index, CONTENT) > CONTENT.bottom() - scene.bedHeight() * 2) {
                low++;
            }
        }
        assertTrue(low >= CoalScene.PARTICLES / 3,
                "only " + low + " sparks began near the bed, they are not coming off the coals");
    }

    @Test
    void aSparkCoolsAsItClimbsSoNothingPopsOutAtTheTop() {
        CoalScene scene = new CoalScene(7L);
        scene.advance(16, CONTENT);
        int start = scene.particleArgb(0) >>> 24;
        int faintest = start;
        for (int frame = 0; frame < 400; frame++) {
            scene.advance(16, CONTENT);
            faintest = Math.min(faintest, scene.particleArgb(0) >>> 24);
        }
        assertTrue(faintest < start, "the spark never dimmed on its way up");
    }

    @Test
    void theSceneRunsAtTheSamePaceWhateverTheFrameRateAndSurvivesAStall() {
        CoalScene slow = new CoalScene(8L);
        CoalScene fast = new CoalScene(8L);
        for (int frame = 0; frame < 10; frame++) {
            slow.advance(50, CONTENT);
        }
        for (int frame = 0; frame < 50; frame++) {
            fast.advance(10, CONTENT);
        }
        assertEquals(slow.particleY(0, CONTENT), fast.particleY(0, CONTENT));

        CoalScene huge = new CoalScene(9L);
        CoalScene capped = new CoalScene(9L);
        huge.advance(9000, CONTENT);
        capped.advance(100, CONTENT);
        assertEquals(capped.particleY(0, CONTENT), huge.particleY(0, CONTENT));
        assertThrows(IllegalArgumentException.class, () -> new CoalScene(1L).advance(-1, CONTENT));
    }
}
