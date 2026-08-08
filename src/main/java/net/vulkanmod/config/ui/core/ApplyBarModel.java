package net.vulkanmod.config.ui.core;

import java.util.Locale;

public record ApplyBarModel(boolean visible, ApplyScope scope, int count) {
    private static final String KEY_PREFIX = "vulkanmod.applybar.";

    public static final ApplyBarModel NONE = new ApplyBarModel(false, ApplyScope.INSTANT, 0);

    public ApplyBarModel {
        if (scope == null) {
            throw new IllegalArgumentException("scope must not be null");
        }
        if (count < 0) {
            throw new IllegalArgumentException("count must not be negative: " + count);
        }
    }

    public String messageKey() {
        if (!visible) {
            throw new IllegalStateException("an invisible apply bar has no message");
        }
        return KEY_PREFIX + scope.name().toLowerCase(Locale.ROOT);
    }

    public static ApplyBarModel of(PendingChanges pending) {
        if (pending == null) {
            throw new IllegalArgumentException("pending must not be null");
        }
        ApplyScope heaviest = pending.heaviestScope();
        if (heaviest == ApplyScope.INSTANT) {
            return NONE;
        }
        return new ApplyBarModel(true, heaviest, pending.countAtLeast(heaviest));
    }
}
