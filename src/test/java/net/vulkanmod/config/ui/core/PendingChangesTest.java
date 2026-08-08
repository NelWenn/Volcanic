package net.vulkanmod.config.ui.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PendingChangesTest {
    private static final SettingId A = SettingId.parse("vulkanmod:a");
    private static final SettingId B = SettingId.parse("vulkanmod:b");

    @Test
    void nothingIsPendingToStart() {
        PendingChanges pending = new PendingChanges();
        assertTrue(pending.isEmpty());
        assertEquals(0, pending.count());
        assertEquals(ApplyScope.INSTANT, pending.heaviestScope());
    }

    @Test
    void unmarkingDropsOnlyThatSetting() {
        PendingChanges pending = new PendingChanges();
        pending.mark(A, ApplyScope.RESTART);
        pending.mark(B, ApplyScope.WINDOW);

        pending.unmark(A);

        assertEquals(1, pending.count());
        assertFalse(pending.isChanged(A));
        assertTrue(pending.isChanged(B));
        assertEquals(ApplyScope.WINDOW, pending.heaviestScope());
    }

    @Test
    void unmarkingSomethingThatWasNeverMarkedChangesNothing() {
        PendingChanges pending = new PendingChanges();
        pending.mark(A, ApplyScope.RESTART);
        pending.unmark(B);
        assertEquals(1, pending.count());
        assertThrows(IllegalArgumentException.class, () -> pending.unmark(null));
    }

    @Test
    void markingTwiceCountsOneSettingAndKeepsTheHeavierScope() {
        PendingChanges pending = new PendingChanges();
        pending.mark(A, ApplyScope.INSTANT);
        pending.mark(A, ApplyScope.RESTART);
        assertEquals(1, pending.count());
        assertEquals(ApplyScope.RESTART, pending.heaviestScope());
    }

    @Test
    void announcesHowManyChangesReachAtLeastAGivenTier() {
        PendingChanges pending = new PendingChanges();
        pending.mark(A, ApplyScope.RESTART);
        pending.mark(B, ApplyScope.INSTANT);
        assertEquals(1, pending.countAtLeast(ApplyScope.RESTART));
        assertEquals(2, pending.countAtLeast(ApplyScope.INSTANT));
        assertTrue(pending.isChanged(A));
        assertFalse(pending.isChanged(SettingId.parse("vulkanmod:c")));
    }

    @Test
    void clearingResetsEverything() {
        PendingChanges pending = new PendingChanges();
        pending.mark(A, ApplyScope.WINDOW);
        pending.clear();
        assertTrue(pending.isEmpty());
        assertEquals(ApplyScope.INSTANT, pending.heaviestScope());
    }

    @Test
    void rejectsNullInput() {
        PendingChanges pending = new PendingChanges();
        assertThrows(IllegalArgumentException.class, () -> pending.mark(null, ApplyScope.INSTANT));
        assertThrows(IllegalArgumentException.class, () -> pending.mark(A, null));
        assertThrows(IllegalArgumentException.class, () -> pending.isChanged(null));
        assertThrows(IllegalArgumentException.class, () -> pending.countAtLeast(null));
    }
}
