package net.vulkanmod.config.ui.core;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class ProfileMatcher {
    private ProfileMatcher() {
    }

    public static Optional<String> match(Map<String, Map<SettingId, Object>> profiles, Map<SettingId, Object> current) {
        if (profiles == null) {
            throw new IllegalArgumentException("profiles must not be null");
        }
        if (current == null) {
            throw new IllegalArgumentException("current must not be null");
        }
        for (Map.Entry<String, Map<SettingId, Object>> profile : profiles.entrySet()) {
            Map<SettingId, Object> governed = profile.getValue();
            if (governed == null || governed.isEmpty()) {
                throw new IllegalArgumentException("profile governs no setting: " + profile.getKey());
            }
            if (matches(governed, current)) {
                return Optional.of(profile.getKey());
            }
        }
        return Optional.empty();
    }

    private static boolean matches(Map<SettingId, Object> governed, Map<SettingId, Object> current) {
        for (Map.Entry<SettingId, Object> entry : governed.entrySet()) {
            if (!Objects.equals(entry.getValue(), current.get(entry.getKey()))) {
                return false;
            }
        }
        return true;
    }
}
