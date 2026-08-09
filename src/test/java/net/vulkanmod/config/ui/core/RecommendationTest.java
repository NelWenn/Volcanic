package net.vulkanmod.config.ui.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RecommendationTest {
    private static Recommendation.Signals signals(BoundVerdict verdict, double headroom, double stutter) {
        return new Recommendation.Signals(1000, verdict, headroom, stutter, false, false, false);
    }

    @Test
    void beforeThreeHundredFramesItAsksForFramesRatherThanGuessing() {
        Recommendation.Advice advice = Recommendation.of(
                new Recommendation.Signals(299, BoundVerdict.GPU, -0.5, 1.0, true, true, true));

        assertEquals("vulkanmod.overview.advice.sampling", advice.messageKey());
        assertNull(advice.routeKey());
        assertNull(advice.modHintKey(), "no diagnosis means no modpack blame either");
    }

    @Test
    void sittingOnTheCapWithRoomSaysSoRatherThanSuggestingCuts() {
        assertEquals("vulkanmod.overview.advice.capped_roomy",
                Recommendation.of(signals(BoundVerdict.CAPPED, 0.5, 1.0)).messageKey());
        assertEquals("vulkanmod.overview.advice.capped",
                Recommendation.of(signals(BoundVerdict.CAPPED, 0.1, 1.0)).messageKey());
    }

    @Test
    void eachBottleneckPointsAtThePageThatCanFixIt() {
        assertEquals("rendering.resolution", Recommendation.of(signals(BoundVerdict.GPU, -0.2, 1.0)).routeKey());
        assertEquals("rendering.culling", Recommendation.of(signals(BoundVerdict.RENDER_CPU, -0.2, 1.0)).routeKey());
        assertEquals("rendering.general", Recommendation.of(signals(BoundVerdict.MESHING, -0.2, 1.0)).routeKey());
        assertNull(Recommendation.of(signals(BoundVerdict.SERVER_TICK, -0.2, 1.0)).routeKey(),
                "no graphics page fixes a tick-bound world");
    }

    @Test
    void theModpackHintOnlyAppearsWhenItExplainsTheVerdict() {
        Recommendation.Signals shaderGpu =
                new Recommendation.Signals(1000, BoundVerdict.GPU, -0.2, 1.0, true, false, false);
        assertEquals("vulkanmod.overview.hint.shader_pack", Recommendation.of(shaderGpu).modHintKey());

        Recommendation.Signals shaderTick =
                new Recommendation.Signals(1000, BoundVerdict.SERVER_TICK, -0.2, 1.0, true, false, false);
        assertNull(Recommendation.of(shaderTick).modHintKey(), "a shader pack does not slow the server tick");
    }

    @Test
    void unevenFramesAreCalledOutEvenWhenTheAverageIsFine() {
        assertEquals("vulkanmod.overview.advice.uneven",
                Recommendation.of(signals(BoundVerdict.UNKNOWN, 0.5, 3.0)).messageKey());
        assertEquals("vulkanmod.overview.advice.headroom",
                Recommendation.of(signals(BoundVerdict.UNKNOWN, 0.5, 1.2)).messageKey());
        assertEquals("vulkanmod.overview.advice.balanced",
                Recommendation.of(signals(BoundVerdict.UNKNOWN, 0.1, 1.2)).messageKey());
    }

    @Test
    void everyVerdictProducesAdviceAndNoBranchEmitsANumber() {
        for (BoundVerdict verdict : BoundVerdict.values()) {
            Recommendation.Advice advice = Recommendation.of(signals(verdict, 0.2, 1.5));
            assertNotNull(advice.messageKey());
            assertTrue(Recommendation.keys().contains(advice.messageKey()),
                    verdict + " emitted an undeclared key: " + advice.messageKey());
            assertFalse(advice.messageKey().matches(".*\\d.*"), "advice keys must not carry figures");
        }
    }

    @Test
    void rejectsInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> Recommendation.of(null));
        assertThrows(IllegalArgumentException.class,
                () -> new Recommendation.Signals(-1, BoundVerdict.GPU, 0, 1, false, false, false));
        assertThrows(IllegalArgumentException.class,
                () -> new Recommendation.Signals(10, null, 0, 1, false, false, false));
    }
}
