package net.vulkanmod.config.ui.core;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ApplyScopeTest {
    @Test
    void declaresTheSixRealTiersInSeverityOrder() {
        assertArrayEquals(new ApplyScope[]{
                ApplyScope.INSTANT, ApplyScope.CHUNK_REBUILD, ApplyScope.TEXTURE_RELOAD,
                ApplyScope.SWAPCHAIN, ApplyScope.WINDOW, ApplyScope.RESTART},
                ApplyScope.values());
    }

    @Test
    void theHeaviestScopeWins() {
        assertEquals(ApplyScope.RESTART, ApplyScope.heaviest(
                List.of(ApplyScope.INSTANT, ApplyScope.RESTART, ApplyScope.WINDOW)));
        assertEquals(ApplyScope.CHUNK_REBUILD, ApplyScope.heaviest(
                List.of(ApplyScope.INSTANT, ApplyScope.CHUNK_REBUILD)));
    }

    @Test
    void nothingPendingMeansNothingToAnnounce() {
        assertEquals(ApplyScope.INSTANT, ApplyScope.heaviest(List.of()));
    }

    @Test
    void rejectsNullInput() {
        assertThrows(IllegalArgumentException.class, () -> ApplyScope.heaviest(null));
    }
}
