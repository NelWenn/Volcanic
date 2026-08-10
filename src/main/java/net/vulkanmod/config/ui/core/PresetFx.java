package net.vulkanmod.config.ui.core;

public final class PresetFx {
    public static final int TICK_MS = 66;
    public static final int TILT_STEPS = 5;
    public static final int TILT_DEGREES = 4;
    public static final int BLOCKS = 12;
    public static final int GHOSTS = 2;
    public static final int STREAKS = 7;
    public static final int BANDS = 8;

    public static final int NONE = -1;
    public static final int SKIP = 0;
    public static final int ROCK = 1;
    public static final int HAZE = 2;
    public static final int ERUPT = 3;
    public static final int SCAN = 4;

    private static final int[] LENGTH = {7, 17, 18, 20, 9};
    private static final int[] ROCK_ANGLES = {-4, -4, 3, 3, -2, -2, 1, 1};

    private int kind = NONE;
    private int card = -1;
    private int frame;
    private long carry;

    public void trigger(int cardIndex, int effect) {
        if (cardIndex < 0) {
            throw new IllegalArgumentException("cardIndex must not be negative: " + cardIndex);
        }
        if (effect < SKIP || effect > SCAN) {
            throw new IllegalArgumentException("unknown effect " + effect);
        }
        this.card = cardIndex;
        this.kind = effect;
        this.frame = 0;
        this.carry = 0L;
    }

    public void advance(long deltaMs) {
        if (deltaMs < 0) {
            throw new IllegalArgumentException("deltaMs must not be negative: " + deltaMs);
        }
        this.carry += Math.min(deltaMs, 250L);
        while (carry >= TICK_MS) {
            carry -= TICK_MS;
            if (kind != NONE && ++frame >= LENGTH[kind]) {
                this.kind = NONE;
                this.card = -1;
            }
        }
    }

    public boolean playing(int cardIndex) {
        return kind != NONE && card == cardIndex;
    }

    public int effect(int cardIndex) {
        return playing(cardIndex) ? kind : NONE;
    }

    public int frame(int cardIndex) {
        return playing(cardIndex) ? frame : 0;
    }

    public int length(int effect) {
        return LENGTH[effect];
    }

    public static int effectFor(String key) {
        if (key == null) {
            return SCAN;
        }
        String name = key.substring(key.lastIndexOf('.') + 1).toLowerCase(java.util.Locale.ROOT);
        return switch (name) {
            case "performance" -> SKIP;
            case "balanced" -> ROCK;
            case "quality" -> HAZE;
            case "ultra" -> ERUPT;
            default -> SCAN;
        };
    }

    public static int tiltStep(Rect card, int mouseX) {
        if (card.width() <= 0) {
            return 0;
        }
        float across = (mouseX - card.x()) / (float) card.width() - 0.5f;
        int step = Math.round(across * 2.0f * TILT_STEPS);
        return Math.max(-TILT_STEPS, Math.min(TILT_STEPS, step));
    }

    public static float tiltDegrees(int step) {
        return step * (TILT_DEGREES / (float) TILT_STEPS);
    }

    public int shakeX(int cardIndex) {
        if (!playing(cardIndex)) {
            return 0;
        }
        if (kind == SKIP) {
            return frame == 0 ? -7 : frame == 1 ? 5 : frame == 2 ? 2 : 0;
        }
        if (kind == ERUPT && frame < 8) {
            return new int[] {-3, 4, -2, 3, -1, 2, -1, 0}[frame];
        }
        return 0;
    }

    public int shakeY(int cardIndex) {
        if (!playing(cardIndex) || kind != ERUPT || frame >= 8) {
            return 0;
        }
        return new int[] {2, -3, -2, 1, 1, -1, 0, 0}[frame];
    }

    public int ghostOffset(int cardIndex, int ghost) {
        if (!playing(cardIndex) || kind != SKIP || frame > 3) {
            return 0;
        }
        return -3 * (ghost + 1) - frame;
    }

    public int streakY(int cardIndex, int streak, int height) {
        return height <= 0 ? 0 : ((frame(cardIndex) * 5 + streak * 11) % height);
    }

    public int rockAngle(int cardIndex) {
        if (!playing(cardIndex) || kind != ROCK || frame >= ROCK_ANGLES.length) {
            return 0;
        }
        return ROCK_ANGLES[frame];
    }

    public int convergeStep(int cardIndex) {
        if (!playing(cardIndex) || kind != ROCK || frame < ROCK_ANGLES.length) {
            return 0;
        }
        int step = frame - ROCK_ANGLES.length + 1;
        return step <= 4 ? step : 0;
    }

    public int blastAge(int cardIndex) {
        if (!playing(cardIndex) || kind != ROCK || frame < ROCK_ANGLES.length + 4) {
            return -1;
        }
        return frame - ROCK_ANGLES.length - 4;
    }

    public int heat(int cardIndex) {
        if (!playing(cardIndex) || kind != HAZE) {
            return 0;
        }
        return frame < 3 ? 1 : frame < 6 ? 2 : frame < 11 ? 3 : frame < 14 ? 2 : 1;
    }

    public int bandShift(int cardIndex, int band) {
        if (!playing(cardIndex) || kind != HAZE) {
            return 0;
        }
        float wave = (float) Math.sin(band * 0.9f + frame * 0.7f);
        int reach = frame < 14 ? 3 : 1;
        return Math.round(wave * reach);
    }

    public boolean flashing(int cardIndex) {
        return playing(cardIndex) && kind == ERUPT && frame < 2;
    }

    public boolean shattered(int cardIndex) {
        return playing(cardIndex) && kind == ERUPT && frame >= 2 && frame < 17;
    }

    public int blockX(int cardIndex, int block, Rect card) {
        return card.x() + card.width() / 2 + Math.round(spread(block) * blockTravel(cardIndex)
                * (float) Math.cos(angle(block)));
    }

    public int blockY(int cardIndex, int block, Rect card) {
        float t = blockTravel(cardIndex);
        return card.y() + card.height() / 2
                + Math.round(spread(block) * t * (float) Math.sin(angle(block)) + t * t * 0.55f);
    }

    public int scanY(int cardIndex, Rect card) {
        if (!playing(cardIndex) || kind != SCAN) {
            return -1;
        }
        return card.y() + Math.min(card.height() - 1, frame * card.height() / LENGTH[SCAN]);
    }

    private float blockTravel(int cardIndex) {
        return playing(cardIndex) && kind == ERUPT ? Math.max(0, frame - 2) * 3.4f : 0.0f;
    }

    private static double angle(int block) {
        return block * (Math.PI * 2.0 / BLOCKS) + (block % 3) * 0.21;
    }

    private static float spread(int block) {
        return 0.7f + (block % 4) * 0.24f;
    }
}
