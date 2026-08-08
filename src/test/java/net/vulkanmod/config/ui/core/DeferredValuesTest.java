package net.vulkanmod.config.ui.core;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DeferredValuesTest {
    private static final SettingId A = SettingId.parse("vulkanmod:a");
    private static final SettingId B = SettingId.parse("vulkanmod:b");

    @Test
    void nothingIsHeldToStart() {
        DeferredValues deferred = new DeferredValues();
        assertTrue(deferred.isEmpty());
        assertEquals(0, deferred.count());
        assertEquals("current", deferred.valueOr(A, "current"));
    }

    @Test
    void aHeldValueShadowsTheCurrentOne() {
        DeferredValues deferred = new DeferredValues();
        deferred.set(A, "chosen", "current");
        assertFalse(deferred.isEmpty());
        assertEquals(1, deferred.count());
        assertEquals("chosen", deferred.valueOr(A, "current"));
        assertEquals("other", deferred.valueOr(B, "other"));
    }

    @Test
    void settingBackToTheOriginalHoldsNothing() {
        DeferredValues deferred = new DeferredValues();
        deferred.set(A, 5, 3);
        deferred.set(A, 3, 3);
        assertTrue(deferred.isEmpty());
        assertEquals(0, deferred.count());
        assertEquals(3, deferred.valueOr(A, 3));
    }

    @Test
    void aValueEqualToTheOriginalIsNeverHeldInTheFirstPlace() {
        DeferredValues deferred = new DeferredValues();
        deferred.set(A, false, false);
        assertTrue(deferred.isEmpty());
    }

    @Test
    void equalityIsByValueNotByIdentity() {
        DeferredValues deferred = new DeferredValues();
        Object chosen = Integer.valueOf(1000);
        Object original = Integer.valueOf(1000);
        assertNotSame(chosen, original);
        deferred.set(A, chosen, original);
        assertTrue(deferred.isEmpty());
    }

    @Test
    void aSecondSetOverwritesTheFirst() {
        DeferredValues deferred = new DeferredValues();
        deferred.set(A, 5, 3);
        deferred.set(A, 7, 3);
        assertEquals(1, deferred.count());
        assertEquals(7, deferred.valueOr(A, 3));
    }

    @Test
    void clearingOneLeavesTheOthers() {
        DeferredValues deferred = new DeferredValues();
        deferred.set(A, 5, 3);
        deferred.set(B, "chosen", "current");
        deferred.clear(A);
        assertEquals(1, deferred.count());
        assertEquals(3, deferred.valueOr(A, 3));
        assertEquals("chosen", deferred.valueOr(B, "current"));
    }

    @Test
    void clearingAnUnheldSettingChangesNothing() {
        DeferredValues deferred = new DeferredValues();
        deferred.set(A, 5, 3);
        deferred.clear(B);
        assertEquals(1, deferred.count());
    }

    @Test
    void clearingDropsEverything() {
        DeferredValues deferred = new DeferredValues();
        deferred.set(A, 5, 3);
        deferred.set(B, "chosen", "current");
        deferred.clear();
        assertTrue(deferred.isEmpty());
        assertEquals(0, deferred.count());
    }

    @Test
    void setReportsWhetherTheValueEndsUpHeld() {
        DeferredValues deferred = new DeferredValues();
        assertTrue(deferred.set(A, 5, 3));
        assertTrue(deferred.set(A, 7, 3));
        assertFalse(deferred.set(A, 3, 3));
        assertFalse(deferred.set(B, "current", "current"));
    }

    @Test
    void drainingPushesEveryHeldValueInTheOrderItWasHeldAndKeepsNothing() {
        DeferredValues deferred = new DeferredValues();
        deferred.set(B, "chosen", "current");
        deferred.set(A, 5, 3);

        List<String> pushed = new ArrayList<>();
        deferred.drainTo((id, value) -> pushed.add(id + "=" + value));

        assertEquals(List.of("vulkanmod:b=chosen", "vulkanmod:a=5"), pushed);
        assertTrue(deferred.isEmpty());
        assertEquals(3, deferred.valueOr(A, 3));
    }

    @Test
    void drainingNothingPushesNothing() {
        DeferredValues deferred = new DeferredValues();
        List<String> pushed = new ArrayList<>();
        deferred.drainTo((id, value) -> pushed.add(id.toString()));
        assertEquals(List.of(), pushed);
    }

    @Test
    void aSinkThatThrowsKeepsOnlyTheValueItCouldNotPush() {
        DeferredValues deferred = new DeferredValues();
        deferred.set(A, 5, 3);
        deferred.set(B, "chosen", "current");

        assertThrows(IllegalStateException.class, () -> deferred.drainTo((id, value) -> {
            if (B.equals(id)) {
                throw new IllegalStateException("the backend refused " + id);
            }
        }));

        assertEquals(1, deferred.count());
        assertEquals(3, deferred.valueOr(A, 3));
        assertEquals("chosen", deferred.valueOr(B, "current"));
    }

    @Test
    void rejectsNullInput() {
        DeferredValues deferred = new DeferredValues();
        assertThrows(IllegalArgumentException.class, () -> deferred.drainTo(null));
        assertThrows(IllegalArgumentException.class, () -> deferred.set(null, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> deferred.set(A, null, 0));
        assertThrows(IllegalArgumentException.class, () -> deferred.set(A, 1, null));
        assertThrows(IllegalArgumentException.class, () -> deferred.valueOr(null, 1));
        assertThrows(IllegalArgumentException.class, () -> deferred.valueOr(A, null));
        assertThrows(IllegalArgumentException.class, () -> deferred.clear(null));
    }
}
