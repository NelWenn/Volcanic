package net.vulkanmod.config.ui.core;

import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoalSceneTest {
    private static final Rect CONTENT = new Rect(120, 60, 900, 520);

    private static CoalScene run(long seed, int frames) {
        CoalScene scene = new CoalScene(seed);
        for (int frame = 0; frame < frames; frame++) {
            scene.advance(16, CONTENT);
        }
        return scene;
    }

    @Test
    void theArtTableIsIntactAndEverySiteFallsInsideTheTexture() {
        assertTrue(CoalArt.siteCount() >= CoalScene.GLOW_SITES);
        for (int site = 0; site < CoalArt.siteCount(); site++) {
            assertTrue(CoalArt.siteX(site) >= 0 && CoalArt.siteX(site) < CoalArt.TEX_W);
            assertTrue(CoalArt.siteY(site) >= 0 && CoalArt.siteY(site) < CoalArt.TEX_H);
            assertTrue(CoalArt.siteHeat(site) > 0);
        }
        for (int site = 0; site < CoalScene.GLOW_SITES; site++) {
            assertTrue(CoalArt.siteY(site) > CoalArt.TEX_H / 2,
                    "site " + site + " would glow in the dark half, above the coals");
        }
    }

    @Test
    void theGlowLandsOnTheArtworksOwnPixelGridRatherThanFloatingOverIt() {
        CoalScene scene = new CoalScene(1L);
        assertEquals(CoalScene.SCALE, scene.glowSize(), "a glow must be exactly one painted pixel");
        for (int site = 0; site < CoalScene.GLOW_SITES; site++) {
            int x = scene.glowX(site, 0, CONTENT) - CONTENT.x();
            int y = scene.glowY(site, CONTENT) - (CONTENT.bottom() - scene.bedHeight());
            assertEquals(0, x % CoalScene.SCALE, "site " + site + " is off the grid horizontally");
            assertEquals(0, y % CoalScene.SCALE, "site " + site + " is off the grid vertically");
        }
    }

    @Test
    void theTilesCoverTheWholeWidthWithoutLeavingAGapOrDrawingASpare() {
        CoalScene scene = new CoalScene(1L);
        for (int width : new int[] {1, 400, 512, 513, 1920, 3400}) {
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
            assertEquals(CONTENT.bottom(), rect.bottom());
            assertEquals(scene.bedHeight(), rect.height());
        }
    }

    @Test
    void theThreeFamiliesAreAllPresentAndEachIndexBelongsToExactlyOne() {
        CoalScene scene = new CoalScene(3L);
        int[] tally = new int[3];
        for (int index = 0; index < CoalScene.PARTICLES; index++) {
            tally[scene.kindOf(index)]++;
        }
        assertEquals(CoalScene.SPARKS, tally[CoalScene.SPARK]);
        assertEquals(CoalScene.LAVAS, tally[CoalScene.LAVA]);
        assertEquals(CoalScene.SMOKES, tally[CoalScene.SMOKE]);
    }

    @Test
    void aLavaBlobIsThrownUpAndFallsBackDownAgain() {
        CoalScene scene = new CoalScene(4L);
        int blob = CoalScene.SPARKS;
        int start = scene.yOf(blob, CONTENT);
        int highest = start;
        boolean fell = false;
        int previous = start;
        for (int frame = 0; frame < 90; frame++) {
            scene.advance(16, CONTENT);
            int y = scene.yOf(blob, CONTENT);
            highest = Math.min(highest, y);
            if (y > previous) {
                fell = true;
            }
            previous = y;
        }
        assertTrue(highest < start, "the blob never rose");
        assertTrue(fell, "the blob never came back down, so gravity is not acting on it");
    }

    @Test
    void smokeOnlyEverRisesBecauseNothingPullsItBack() {
        CoalScene scene = new CoalScene(5L);
        int puff = CoalScene.SPARKS + CoalScene.LAVAS;
        int previous = scene.yOf(puff, CONTENT);
        for (int frame = 0; frame < 60; frame++) {
            scene.advance(16, CONTENT);
            int y = scene.yOf(puff, CONTENT);
            if (y > previous + 1) {
                throw new AssertionError("smoke fell from " + previous + " to " + y);
            }
            previous = y;
        }
    }

    @Test
    void sparksAreTheSmallestAndTheShortestLivedOfTheThree() {
        CoalScene scene = run(6L, 4);
        for (int index = 0; index < CoalScene.SPARKS; index++) {
            assertEquals(1, scene.sizeOf(index), "a spark must stay a single pixel");
        }
        for (int index = CoalScene.SPARKS; index < CoalScene.SPARKS + CoalScene.LAVAS; index++) {
            assertTrue(scene.sizeOf(index) >= 2, "a lava blob must be chunkier than a spark");
        }
        for (int index = CoalScene.SPARKS + CoalScene.LAVAS; index < CoalScene.PARTICLES; index++) {
            assertTrue(scene.sizeOf(index) >= 2, "a puff of smoke must be the fattest of the three");
        }
    }

    @Test
    void smokeStaysFaintWhileSparksBurnBright() {
        CoalScene scene = run(7L, 20);
        for (int index = CoalScene.SPARKS + CoalScene.LAVAS; index < CoalScene.PARTICLES; index++) {
            assertTrue((scene.argbOf(index) >>> 24) <= 60,
                    "smoke at alpha " + (scene.argbOf(index) >>> 24) + " would hide the text");
        }
        int brightestSpark = IntStream.range(0, CoalScene.SPARKS)
                .map(index -> scene.argbOf(index) >>> 24).max().orElse(0);
        assertTrue(brightestSpark > 120, "sparks are meant to read as hot, got " + brightestSpark);
    }

    @Test
    void aLavaBlobCoolsAsItFalls() {
        CoalScene scene = new CoalScene(8L);
        int blob = CoalScene.SPARKS;
        int hot = (scene.argbOf(blob) >>> 16) & 0xFF;
        int coolest = hot;
        for (int frame = 0; frame < 80; frame++) {
            scene.advance(16, CONTENT);
            coolest = Math.min(coolest, (scene.argbOf(blob) >>> 16) & 0xFF);
        }
        assertTrue(coolest < hot, "the blob never darkened during its flight");
    }

    @Test
    void everyParticleStaysInsideThePageItIsDrawnOn() {
        CoalScene scene = new CoalScene(9L);
        for (int frame = 0; frame < 500; frame++) {
            scene.advance(16, CONTENT);
            for (int index = 0; index < CoalScene.PARTICLES; index++) {
                int x = scene.xOf(index, CONTENT);
                int y = scene.yOf(index, CONTENT);
                assertTrue(x > CONTENT.x() - 200 && x < CONTENT.right() + 200,
                        "a particle ran far off sideways: " + x);
                assertTrue(y > CONTENT.y() - 200 && y < CONTENT.bottom() + 200,
                        "a particle ran far off vertically: " + y);
            }
        }
    }

    @Test
    void everyParticleIsRecycledRatherThanLeftDeadOnScreen() {
        CoalScene scene = run(10L, 900);
        for (int index = 0; index < CoalScene.PARTICLES; index++) {
            assertTrue((scene.argbOf(index) >>> 24) >= 0);
            assertTrue(scene.sizeOf(index) >= 1, "particle " + index + " decayed to nothing");
        }
    }

    @Test
    void theSceneRunsAtTheSamePaceWhateverTheFrameRateAndSurvivesAStall() {
        CoalScene slow = new CoalScene(11L);
        CoalScene fast = new CoalScene(11L);
        for (int frame = 0; frame < 10; frame++) {
            slow.advance(50, CONTENT);
        }
        for (int frame = 0; frame < 50; frame++) {
            fast.advance(10, CONTENT);
        }
        assertEquals(slow.yOf(0, CONTENT), fast.yOf(0, CONTENT), 1);

        CoalScene huge = new CoalScene(12L);
        CoalScene capped = new CoalScene(12L);
        huge.advance(9000, CONTENT);
        capped.advance(100, CONTENT);
        assertEquals(capped.yOf(0, CONTENT), huge.yOf(0, CONTENT));
        assertThrows(IllegalArgumentException.class, () -> new CoalScene(1L).advance(-1, CONTENT));
    }
}
