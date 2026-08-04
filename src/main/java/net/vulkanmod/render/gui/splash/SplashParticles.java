package net.vulkanmod.render.gui.splash;

import net.minecraft.resources.ResourceLocation;

import java.util.Random;

public final class SplashParticles {

    public static final Random RANDOM = new Random();

    public static final ResourceLocation[] SMOKE_FRAMES = new ResourceLocation[8];

    static {
        for (int i = 0; i < 8; i++) {
            SMOKE_FRAMES[i] = ResourceLocation.withDefaultNamespace("textures/particle/generic_" + i + ".png");
        }
    }

    public static class Ember {
        public float x, y;
        public float speed;
        public float drift;
        public float life;
        public float size;

        public Ember(float x, float y, float speed, float drift, float size) {
            this.x = x;
            this.y = y;
            this.speed = speed;
            this.drift = drift;
            this.size = size;
            this.life = 0f;
        }
    }

    public static class Smoke {
        public float x, y;
        public float speed;
        public float drift;
        public float wobblePhase;
        public float life;
        public float baseSize;

        public Smoke(float x, float y, float speed, float drift, float baseSize) {
            this.x = x;
            this.y = y;
            this.speed = speed;
            this.drift = drift;
            this.baseSize = baseSize;
            this.wobblePhase = RANDOM.nextFloat() * (float) (Math.PI * 2);
            this.life = 0f;
        }
    }
}