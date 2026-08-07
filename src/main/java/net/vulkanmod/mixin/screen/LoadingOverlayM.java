package net.vulkanmod.mixin.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.vulkanmod.Initializer;
import net.vulkanmod.render.gui.splash.SplashParticles;
import net.vulkanmod.render.gui.splash.SplashParticles.Ember;
import net.vulkanmod.render.gui.splash.SplashParticles.Smoke;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntSupplier;

@Mixin(LoadingOverlay.class)
public class LoadingOverlayM {

    @Shadow
    private long fadeInStart;

    @Shadow
    private float currentProgress;

    @Shadow
    @Final
    @Mutable
    private static IntSupplier BRAND_BACKGROUND;

    @Unique
    private static ResourceLocation volcanic$splash;

    @Unique
    private final List<Ember> volcanic$embers = new ArrayList<>();
    @Unique
    private long volcanic$lastEmberSpawn = 0L;

    @Unique
    private final List<Smoke> volcanic$smokes = new ArrayList<>();
    @Unique
    private long volcanic$lastSmokeSpawn = 0L;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void modifyBrandBackground(CallbackInfo ci) {
        BRAND_BACKGROUND = () -> 0xFFD1360A; // change background color :D
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    public void init(CallbackInfo ci) {
        volcanic$splash = ResourceLocation.fromNamespaceAndPath(
                Initializer.MOD_ID, "volcanic.png"
        );
    }

    @Inject(method = "render", at = @At("TAIL"))
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        long now = Util.getMillis();
        float f = this.fadeInStart > -1L ? (float) (now - this.fadeInStart) / 1000.0F : -1.0F;
        float fAlpha = 1.0f - Mth.clamp(f - 1.0f, 0.0f, 1.0f);

        RenderSystem.defaultBlendFunc();
        RenderSystem.enableBlend();
        
        volcanic$updateSmoke(guiGraphics, now, fAlpha);
        volcanic$update(guiGraphics, now, fAlpha);
        volcanic$renderGlow(guiGraphics, now, fAlpha);

        int texW = 32;
        int texH = 32;
        int margin = 10;
        int x = guiGraphics.guiWidth() - texW - margin;
        int y = guiGraphics.guiHeight() - texH - margin;

        guiGraphics.setColor(1.0f, 1.0f, 1.0f, 0.5f * fAlpha);
        guiGraphics.blit(volcanic$splash, x, y, 0, 0, texW, texH, texW, texH);
        guiGraphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);

        RenderSystem.disableBlend();
    }

    @Inject(method = "drawProgressBar", at = @At("HEAD"), cancellable = true)
    private void volcanic$drawBar(GuiGraphics guiGraphics, int minX, int minY, int maxX, int maxY, float partialTick, CallbackInfo ci) {
        ci.cancel();

        int alphaMul = Math.round(255 * Mth.clamp(partialTick, 0.0f, 1.0f));
        if (alphaMul <= 0) return;

        int width = maxX - minX;
        int fillWidth = Mth.ceil((width - 2) * Mth.clamp(this.currentProgress, 0.0f, 1.0f));

        int dark = (Math.round(200 * (alphaMul / 255f)) << 24) | 0x2A1108;
        int darker = (Math.round(200 * (alphaMul / 255f)) << 24) | 0x150A05;

        guiGraphics.fillGradient(minX, minY, maxX, maxY, dark, darker);

        int border = (alphaMul << 24) | 0xFF9A3C;

        guiGraphics.fill(minX, minY, maxX, minY + 1, border);
        guiGraphics.fill(minX, maxY - 1, maxX, maxY, border);
        guiGraphics.fill(minX, minY, minX + 1, maxY, border);
        guiGraphics.fill(maxX - 1, minY, maxX, maxY, border);

        if (fillWidth > 0) {
            int lStart = (alphaMul << 24) | 0xFFE066;
            int lEnd = (alphaMul << 24) | 0xB8320A;

            guiGraphics.fillGradient(minX + 2, minY + 2, minX + 2 + fillWidth, maxY - 2, lEnd, lStart);
        }
    }

    @Unique
    private void volcanic$update(GuiGraphics guiGraphics, long now, float fAlpha) {
        int w = guiGraphics.guiWidth();
        int h = guiGraphics.guiHeight();

        if (now - volcanic$lastEmberSpawn > 120L && volcanic$embers.size() < 60) {
            volcanic$lastEmberSpawn = now;

            float startX = SplashParticles.RANDOM.nextFloat() * w;
            float speed = 0.15f + SplashParticles.RANDOM.nextFloat() * 0.25f;
            float drift = (SplashParticles.RANDOM.nextFloat() - 0.5f) * 0.08f;
            float size = 1.5f + SplashParticles.RANDOM.nextFloat() * 2.0f;

            volcanic$embers.add(new Ember(startX, h + 10, speed, drift, size));
        }

        List<Ember> toRemove = new ArrayList<>();

        for (Ember ember : volcanic$embers) {
            ember.y -= ember.speed * 4f;
            ember.x += ember.drift;
            ember.life += 0.006f;

            if (ember.y < -20 || ember.life >= 1f) {
                toRemove.add(ember);
                continue;
            }

            float flicker = 0.6f + 0.4f * Mth.sin((now % 500L) / 500.0f * (float) Math.PI * 2f + ember.x);
            float lifeFade = 1.0f - Mth.clamp((ember.life - 0.7f) / 0.3f, 0f, 1f);
            int a = (int) (255 * flicker * lifeFade * fAlpha);
            if (a <= 0) continue;

            int r = 255;
            int g = (int) Mth.lerp(ember.life, 140, 40);
            int b = 0;
            int color = (a << 24) | (r << 16) | (g << 8) | b;

            int sx = (int) ember.x;
            int sy = (int) ember.y;
            int s = Math.round(ember.size);

            guiGraphics.fill(sx, sy, sx + s, sy + s, color);
        }
        volcanic$embers.removeAll(toRemove);
    }

    @Unique
    private void volcanic$updateSmoke(GuiGraphics guiGraphics, long now, float fAlpha) {
        int w = guiGraphics.guiWidth();
        int h = guiGraphics.guiHeight();

        if (now - volcanic$lastSmokeSpawn > 450L && volcanic$smokes.size() < 14) {
            volcanic$lastSmokeSpawn = now;

            float startX = w * 0.15f + SplashParticles.RANDOM.nextFloat() * w * 0.7f;
            float speed = 0.02f + SplashParticles.RANDOM.nextFloat() * 0.02f;
            float drift = (SplashParticles.RANDOM.nextFloat() - 0.5f) * 0.015f;
            float baseSize = 20f + SplashParticles.RANDOM.nextFloat() * 14f;

            volcanic$smokes.add(new Smoke(startX, h + 20, speed, drift, baseSize));
        }

        List<Smoke> toRemove = new ArrayList<>();

        for (Smoke smoke : volcanic$smokes) {
            smoke.y -= smoke.speed * 4f;
            smoke.x += smoke.drift + Mth.sin(smoke.wobblePhase + now / 600.0f) * 0.15f;
            smoke.life += 0.0025f;

            if (smoke.life >= 1f) {
                toRemove.add(smoke);
                continue;
            }

            float fadeIn = Mth.clamp(smoke.life / 0.15f, 0f, 1f);
            float fadeOut = 1.0f - Mth.clamp((smoke.life - 0.7f) / 0.3f, 0f, 1f);
            float alpha = 0.35f * fadeIn * fadeOut * fAlpha;
            if (alpha <= 0.01f) continue;

            float scale = 1.0f + smoke.life * 1.8f;
            int size = Math.round(smoke.baseSize * scale);

            int frame = Mth.floor(smoke.life * SplashParticles.SMOKE_FRAMES.length) % SplashParticles.SMOKE_FRAMES.length;
            ResourceLocation tex = SplashParticles.SMOKE_FRAMES[frame];

            guiGraphics.setColor(0.85f, 0.85f, 0.85f, alpha);
            guiGraphics.blit(tex, Math.round(smoke.x - size / 2f), Math.round(smoke.y - size / 2f), 0, 0, size, size, size, size);
            guiGraphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        }

        volcanic$smokes.removeAll(toRemove);
    }

    @Unique
    private void volcanic$renderGlow(GuiGraphics guiGraphics, long now, float fAlpha) {
        int w = guiGraphics.guiWidth();
        int h = guiGraphics.guiHeight();
        int cx = w / 2;
        int cy = h / 2;
        int glowRadius = 160;

        float phase = (float) ((now % 2400L) / 2400.0) * (float) (Math.PI * 2);
        float pulse = (Mth.sin(phase) + 1.0f) * 0.5f;

        int glowAlpha = Math.round((18 + 10 * pulse) * fAlpha);
        int glowColor = (glowAlpha << 24) | 0xFF6A00;

        guiGraphics.fillGradient(cx - glowRadius, cy - glowRadius, cx + glowRadius, cy + glowRadius, glowColor, 0);
    }
}