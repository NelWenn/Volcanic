package net.vulkanmod.render.profiling;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("StackSampler")
class StackSamplerTest {
    @Test
    @DisplayName("an empty window profiles to nothing rather than throwing")
    void anEmptyWindowIsSafe() {
        StackSampler.clear();

        assertTrue(StackSampler.profile(0L, 1L).isEmpty());
        assertEquals(0, StackSampler.samplesIn(0L, 1L));
    }

    @Test
    @DisplayName("the sampler records its own thread and reports where the time went")
    void theSamplerAttributesTimeToRealMethods() throws Exception {
        StackSampler.clear();
        StackSampler.watch(Thread.currentThread().threadId());
        StackSampler.setGameplay(true);
        StackSampler.setRunning(true);

        long from = System.currentTimeMillis();
        long spin = 0L;
        while (System.currentTimeMillis() - from < 220L) {
            spin += burn();
        }
        long to = System.currentTimeMillis();
        StackSampler.setRunning(false);
        assertTrue(spin >= 0L);

        List<StackSampler.Frame> profile = StackSampler.profile(from, to);
        assertFalse(profile.isEmpty(), "a fifth of a second of work must leave samples behind");

        float total = 0.0f;
        for (StackSampler.Frame frame : profile) {
            assertTrue(frame.share() > 0.0f && frame.share() <= 1.0f);
            total += frame.share();
        }
        assertTrue(total <= 1.01f,
                "shares are self time, so they must not all read 100% — they sum to one");
        assertTrue(profile.get(0).share() >= profile.get(profile.size() - 1).share(),
                "the hottest method must come first");
    }

    private static long burn() {
        long sum = 0L;
        for (int i = 0; i < 20_000; i++) {
            sum += i * 31L;
        }
        return sum;
    }
}
