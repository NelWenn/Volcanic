package net.vulkanmod.config.ui.core;

public final class SliderGeometry {
    public static final int KNOB_WIDTH = 6;

    private SliderGeometry() {
    }

    public static Rect knob(Rect track, int value, int min, int max, int knobWidth) {
        requireTrack(track, min, max);
        if (knobWidth <= 0) {
            throw new IllegalArgumentException("knobWidth must be positive: " + knobWidth);
        }

        int travel = travel(track, knobWidth);
        int offset = max == min
                ? 0
                : Math.round((float) travel * (clamp(value, min, max) - min) / (max - min));
        return new Rect(track.x() + offset, track.y() + (track.height() - knobWidth) / 2,
                knobWidth, knobWidth);
    }

    public static Rect track(Rect card, int padX, int trackWidth) {
        if (card == null) {
            throw new IllegalArgumentException("card must not be null");
        }
        if (padX < 0) {
            throw new IllegalArgumentException("padX must not be negative: " + padX);
        }
        if (trackWidth <= 0) {
            throw new IllegalArgumentException("trackWidth must be positive: " + trackWidth);
        }

        int x = card.right() - padX - trackWidth;
        if (card.isEmpty() || x <= card.x() + padX) {
            return Rect.EMPTY;
        }
        return new Rect(x, card.y(), trackWidth, card.height());
    }

    public static int valueAt(Rect track, int x, int min, int max, int step) {
        requireTrack(track, min, max);
        if (step <= 0) {
            throw new IllegalArgumentException("step must be positive: " + step);
        }

        int travel = travel(track, KNOB_WIDTH);
        if (max == min || travel == 0) {
            return min;
        }

        int position = clamp(x - KNOB_WIDTH / 2 - track.x(), 0, travel);
        int steps = Math.round((float) position * (max - min) / travel / step);
        return min + Math.min(steps, (max - min) / step) * step;
    }

    private static int travel(Rect track, int knobWidth) {
        return Math.max(0, track.width() - knobWidth);
    }

    private static void requireTrack(Rect track, int min, int max) {
        if (track == null) {
            throw new IllegalArgumentException("track must not be null");
        }
        if (max < min) {
            throw new IllegalArgumentException("max " + max + " is below min " + min);
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
