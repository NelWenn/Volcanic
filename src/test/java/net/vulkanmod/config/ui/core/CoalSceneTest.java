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
        assertTrue(CoalArt.siteCount() > 0);
        for (int site = 0; site < CoalArt.siteCount(); site++) {
            assertTrue(CoalArt.siteX(site) >= 0 && CoalArt.siteX(site) < CoalArt.TEX_W);
            assertTrue(CoalArt.siteY(site) >= 0 && CoalArt.siteY(site) < CoalArt.TEX_H);
            assertTrue(CoalArt.siteHeat(site) > 0);
        }
        for (int site = 0; site < CoalArt.siteCount(); site++) {
            assertTrue(CoalArt.siteY(site) > CoalArt.TEX_H / 2,
                    "site " + site + " would spawn particles above the coals");
        }
    }

    @Test
    void everyZoneSwingsBetweenGlowingAndGoingOutRatherThanHoldingOneShade() {
        CoalScene scene = new CoalScene(1L);
        for (int zone = 0; zone < CoalScene.ZONES; zone++) {
            float hottest = 0.0f;
            float coldest = 1.0f;
            for (int frame = 0; frame < 900; frame++) {
                scene.advance(16, CONTENT);
                hottest = Math.max(hottest, scene.zoneHeat(zone));
                coldest = Math.min(coldest, scene.zoneHeat(zone));
            }
            assertTrue(hottest > 0.75f, "zone " + zone + " never got hot, peak " + hottest);
            assertTrue(coldest < 0.25f, "zone " + zone + " never went dark, floor " + coldest);
        }
    }

    @Test
    void theZonesAreOutOfPhaseSoTheHeatTravelsInsteadOfBlinkingAtOnce() {
        CoalScene scene = run(2L, 30);
        long shades = IntStream.range(0, CoalScene.ZONES)
                .map(zone -> Math.round(scene.zoneHeat(zone) * 40)).distinct().count();
        assertTrue(shades >= 5, "only " + shades + " distinct zone temperatures, the bed pulses as one");
    }

    @Test
    void aZoneTintReachesBothARealGlowAndANearBlack() {
        CoalScene scene = new CoalScene(3L);
        int brightest = 0;
        int darkest = 255;
        for (int frame = 0; frame < 900; frame++) {
            scene.advance(16, CONTENT);
            int red = (scene.zoneTint(0) >> 16) & 0xFF;
            brightest = Math.max(brightest, red);
            darkest = Math.min(darkest, red);
        }
        assertTrue(brightest > 200, "the hot end never lit up, peak red " + brightest);
        assertTrue(darkest < 90, "the cold end never went dark, floor red " + darkest);
    }

    @Test
    void smokeWalksItsTextureFramesFromFirstToLast() {
        CoalScene scene = new CoalScene(4L);
        int puff = CoalScene.SPARKS + CoalScene.LAVAS;
        int lowest = CoalScene.SMOKE_FRAMES;
        int highest = -1;
        for (int frame = 0; frame < 200; frame++) {
            scene.advance(16, CONTENT);
            int index = scene.smokeFrame(puff);
            assertTrue(index >= 0 && index < CoalScene.SMOKE_FRAMES, "frame out of range: " + index);
            lowest = Math.min(lowest, index);
            highest = Math.max(highest, index);
        }
        assertTrue(highest > lowest, "the smoke never advanced past its first frame");
    }

    @Test
    void theBedIsDrawnOnceAcrossTheWholeWidthSoNothingIsEverRepeated() {
        CoalScene scene = new CoalScene(1L);
        for (int width : new int[] {200, 512, 900, 1920, 3400}) {
            Rect content = new Rect(40, 10, width, 600);
            Rect bed = scene.bedRect(content);
            assertEquals(content.x(), bed.x(), "width " + width + " left a margin");
            assertEquals(content.width(), bed.width(), "width " + width + " did not span the page");
            assertEquals(content.bottom(), bed.bottom(), "width " + width + " floated off the bottom");
        }
    }

    @Test
    void theBedKeepsTheArtworksProportionsUntilItWouldSwallowThePage() {
        CoalScene scene = new CoalScene(2L);
        Rect roomy = new Rect(0, 0, 900, 900);
        assertEquals(Math.round(CoalArt.TEX_H * 900.0f / CoalArt.TEX_W), scene.bedHeight(roomy),
                "with room to spare the bed should keep the artwork's aspect");

        Rect squat = new Rect(0, 0, 2000, 200);
        assertTrue(scene.bedHeight(squat) <= 100,
                "on a short wide page the bed must give way rather than fill it");
        assertTrue(scene.bedHeight(squat) >= 1);
    }

    @Test
    void anEmptyPageProducesNoBedAtAll() {
        CoalScene scene = new CoalScene(3L);
        assertTrue(scene.bedRect(new Rect(0, 0, 0, 100)).isEmpty());
        assertTrue(scene.bedRect(new Rect(0, 0, 100, 0)).isEmpty());
    }

    @Test
    void theParticlesGrowWithTheBedSoTheyNeverLookLostOnAWidePage() {
        CoalScene scene = new CoalScene(4L);
        float narrow = scene.particleScale(new Rect(0, 0, 400, 600));
        float wide = scene.particleScale(new Rect(0, 0, 1800, 900));
        assertTrue(wide > narrow, "a wider page must not leave the particles tiny");
        assertTrue(narrow >= 0.7f && wide <= 1.5f, "the scale must stay inside sane bounds");
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
            assertTrue(scene.sizeOf(index) <= 5, "a spark must stay small");
        }
        for (int index = CoalScene.SPARKS; index < CoalScene.SPARKS + CoalScene.LAVAS; index++) {
            assertTrue(scene.sizeOf(index) >= 5, "a lava blob must be chunkier than a spark");
        }
        for (int index = CoalScene.SPARKS + CoalScene.LAVAS; index < CoalScene.PARTICLES; index++) {
            assertTrue(scene.sizeOf(index) >= 7, "a puff of smoke must be the fattest of the three");
        }
    }

    @Test
    void smokeStaysFaintWhileSparksBurnBright() {
        CoalScene scene = run(7L, 20);
        for (int index = CoalScene.SPARKS + CoalScene.LAVAS; index < CoalScene.PARTICLES; index++) {
            assertTrue((scene.argbOf(index) >>> 24) <= 90,
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
        int alive = 0;
        for (int index = 0; index < CoalScene.PARTICLES; index++) {
            assertTrue(scene.sizeOf(index) >= 1, "particle " + index + " decayed to nothing");
            if ((scene.argbOf(index) >>> 24) > 0) {
                alive++;
            }
        }
        assertTrue(alive > 0, "after nine seconds every particle had faded out and none came back");
    }

    @Test
    void aSparkOrAPopWaitsBetweenAppearancesInsteadOfFiringBackToBack() {
        CoalScene scene = new CoalScene(21L);
        int hidden = 0;
        for (int frame = 0; frame < 600; frame++) {
            scene.advance(16, CONTENT);
            for (int index = 0; index < CoalScene.SPARKS + CoalScene.LAVAS; index++) {
                if (scene.waiting(index)) {
                    hidden++;
                }
            }
        }
        assertTrue(hidden > 0, "every spark and pop was on screen the whole time, nothing is rationed");
    }

    @Test
    void smokeNeverWaitsBecauseAFireAlwaysSmokes() {
        CoalScene scene = new CoalScene(22L);
        for (int frame = 0; frame < 400; frame++) {
            scene.advance(16, CONTENT);
            for (int index = CoalScene.SPARKS + CoalScene.LAVAS; index < CoalScene.PARTICLES; index++) {
                assertTrue(!scene.waiting(index), "a puff of smoke was held back");
            }
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
