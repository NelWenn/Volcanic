package net.vulkanmod.config.ui.core;

import java.util.List;

public final class PresetSuggestion {
    public static final double COMFORTABLE = 1.0;
    public static final double ROOM_TO_SPARE = 2.0;
    public static final double PLENTY = 3.0;
    public static final double STUTTER_RATIO = 2.5;
    public static final int FALLBACK_TARGET_FPS = 60;

    private static final List<String> LADDER = List.of(
            "vulkanmod.options.performancePreset.performance",
            "vulkanmod.options.performancePreset.balanced",
            "vulkanmod.options.performancePreset.quality",
            "vulkanmod.options.performancePreset.ultra");

    public record Reading(int sampleCount, float medianMs, float lowMs, double targetFps, String playingKey) {
        public Reading {
            if (sampleCount < 0) {
                throw new IllegalArgumentException("sampleCount must not be negative: " + sampleCount);
            }
        }
    }

    private PresetSuggestion() {
    }

    public static int rung(String presetKey) {
        return presetKey == null ? -1 : LADDER.indexOf(presetKey);
    }

    public static String suggest(Reading reading) {
        if (reading == null) {
            throw new IllegalArgumentException("reading must not be null");
        }
        if (reading.sampleCount() < FrameSamples.READY_AT || reading.medianMs() <= 0.0f) {
            return null;
        }
        int playing = rung(reading.playingKey());
        if (playing < 0) {
            return null;
        }

        double target = reading.targetFps() > 0.0 ? reading.targetFps() : FALLBACK_TARGET_FPS;
        double budgetMs = 1000.0 / target;
        double headroom = budgetMs / reading.medianMs();
        double lowHeadroom = reading.lowMs() > 0.0f ? budgetMs / reading.lowMs() : headroom;
        boolean uneven = reading.lowMs() > 0.0f && reading.lowMs() / reading.medianMs() > STUTTER_RATIO;

        int step = 0;
        if (headroom >= PLENTY && lowHeadroom >= ROOM_TO_SPARE && !uneven) {
            step = 2;
        } else if (headroom >= ROOM_TO_SPARE && lowHeadroom >= COMFORTABLE && !uneven) {
            step = 1;
        } else if (headroom < COMFORTABLE || lowHeadroom < 0.5) {
            step = -1;
        }

        int wanted = Math.max(0, Math.min(LADDER.size() - 1, playing + step));
        return wanted == playing ? null : LADDER.get(wanted);
    }
}
