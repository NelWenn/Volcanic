package net.vulkanmod.config.ui.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ApplyBarModelTest {
    private static final SettingId A = SettingId.parse("vulkanmod:a");
    private static final SettingId B = SettingId.parse("vulkanmod:b");
    private static final SettingId C = SettingId.parse("vulkanmod:c");

    @Test
    void nothingPendingMeansNoBar() {
        assertFalse(ApplyBarModel.of(new PendingChanges()).visible());
    }

    @Test
    void changesThatTakeEffectImmediatelyAreNotWorthAnnouncing() {
        PendingChanges pending = new PendingChanges();
        pending.mark(A, ApplyScope.INSTANT);
        pending.mark(B, ApplyScope.INSTANT);
        assertFalse(ApplyBarModel.of(pending).visible());
    }

    @Test
    void itAnnouncesTheHeaviestScopeAndHowManyChangesReachIt() {
        PendingChanges pending = new PendingChanges();
        pending.mark(A, ApplyScope.INSTANT);
        pending.mark(B, ApplyScope.RESTART);
        pending.mark(C, ApplyScope.RESTART);

        ApplyBarModel bar = ApplyBarModel.of(pending);
        assertTrue(bar.visible());
        assertEquals(ApplyScope.RESTART, bar.scope());
        assertEquals(2, bar.count());
    }

    @Test
    void aWindowChangeIsAnnouncedEvenThoughItIsLighterThanARestart() {
        PendingChanges pending = new PendingChanges();
        pending.mark(A, ApplyScope.WINDOW);
        ApplyBarModel bar = ApplyBarModel.of(pending);
        assertTrue(bar.visible());
        assertEquals(ApplyScope.WINDOW, bar.scope());
        assertEquals(1, bar.count());
    }

    @Test
    void rejectsNullInput() {
        assertThrows(IllegalArgumentException.class, () -> ApplyBarModel.of(null));
    }
}
