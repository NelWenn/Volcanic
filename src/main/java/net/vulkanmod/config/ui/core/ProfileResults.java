package net.vulkanmod.config.ui.core;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class ProfileResults {

    public record Result(int fps, float medianMs, float lowMs, int frames) {
        public Result {
            if (fps <= 0 || frames <= 0) {
                throw new IllegalArgumentException("a result must carry real figures");
            }
        }
    }

    private final Map<String, Result> byProfile = new LinkedHashMap<>();

    public void record(String profileKey, Result result) {
        if (profileKey == null || profileKey.isBlank()) {
            throw new IllegalArgumentException("profileKey must not be blank");
        }
        if (result == null) {
            throw new IllegalArgumentException("result must not be null");
        }
        Result existing = byProfile.get(profileKey);
        if (existing == null || result.frames() >= existing.frames()) {
            byProfile.put(profileKey, result);
        }
    }

    public Optional<Result> of(String profileKey) {
        return profileKey == null ? Optional.empty() : Optional.ofNullable(byProfile.get(profileKey));
    }

    public boolean has(String profileKey) {
        return of(profileKey).isPresent();
    }

    public int size() {
        return byProfile.size();
    }

    public void forget(String profileKey) {
        byProfile.remove(profileKey);
    }

    public void clear() {
        byProfile.clear();
    }
}
