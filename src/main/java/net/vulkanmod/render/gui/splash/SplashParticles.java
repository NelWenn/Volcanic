package net.vulkanmod.render.gui.splash;

import java.util.Random;

public final class SplashParticles {

    public static final Random RANDOM = new Random();

    private SplashParticles() {
    }

    public static class Ember {
        public float x;
        public float y;
        public float vx;
        public float vy;
        public float life;
        public float lifeSpan;
        public float swayPhase;
        public float swaySpeed;
        public float swayAmp;
        public int size;

        public Ember(float x, float y, float vx, float vy, float lifeSpan, int size) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.lifeSpan = lifeSpan;
            this.size = size;
            this.life = 0.0f;
            this.swayPhase = RANDOM.nextFloat() * 6.2831855f;
            this.swaySpeed = 0.7f + RANDOM.nextFloat() * 1.6f;
            this.swayAmp = 3.0f + RANDOM.nextFloat() * 13.0f;
        }
    }

    public static class Smoke {
        public float x;
        public float y;
        public float vx;
        public float vy;
        public float life;
        public float lifeSpan;
        public float baseSize;
        public float wobblePhase;
        public final float[] lobeX;
        public final float[] lobeY;
        public final float[] lobeR;

        public Smoke(float x, float y, float vx, float vy, float lifeSpan, float baseSize) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.lifeSpan = lifeSpan;
            this.baseSize = baseSize;
            this.life = 0.0f;
            this.wobblePhase = RANDOM.nextFloat() * 6.2831855f;

            int lobes = 4 + RANDOM.nextInt(2);
            this.lobeX = new float[lobes];
            this.lobeY = new float[lobes];
            this.lobeR = new float[lobes];

            for (int i = 0; i < lobes; i++) {
                this.lobeX[i] = (RANDOM.nextFloat() - 0.5f) * 1.15f;
                this.lobeY[i] = (RANDOM.nextFloat() - 0.5f) * 0.85f;
                this.lobeR[i] = 0.30f + RANDOM.nextFloat() * 0.32f;
            }
        }
    }
}
