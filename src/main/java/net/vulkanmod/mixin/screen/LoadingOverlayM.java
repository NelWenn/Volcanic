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
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.IntSupplier;

@Mixin(LoadingOverlay.class)
public class LoadingOverlayM {

    @Unique
    private static final int VOLCANIC_SURFACE = 0x241511;
    @Unique
    private static final int VOLCANIC_SURFACE_DEEP = 0x0E0A09;
    @Unique
    private static final int VOLCANIC_CALDERA = 0x5A3025;
    @Unique
    private static final int VOLCANIC_BORDER = 0x3A231D;
    @Unique
    private static final int VOLCANIC_ACCENT = 0xFF5A1F;

    @Unique
    private static final int VOLCANIC_TITLE_W = 512;
    @Unique
    private static final int VOLCANIC_TITLE_H = 96;

    @Shadow
    private long fadeInStart;

    @Shadow
    private long fadeOutStart;

    @Shadow
    @Final
    private boolean fadeIn;

    @Shadow
    private float currentProgress;

    @Shadow
    @Final
    @Mutable
    private static IntSupplier BRAND_BACKGROUND;

    @Unique
    private ResourceLocation volcanic$titleTex;

    @Unique
    private final List<Ember> volcanic$embers = new ArrayList<>();
    @Unique
    private final List<Smoke> volcanic$smokes = new ArrayList<>();

    @Unique
    private float volcanic$emberBudget;
    @Unique
    private float volcanic$smokeBudget;

    @Unique
    private long volcanic$lastMillis;
    @Unique
    private float volcanic$time;

    @Unique
    private float volcanic$ventX;
    @Unique
    private float volcanic$ventY;
    @Unique
    private float volcanic$ventSpread;

    @Unique
    private float volcanic$barTipX;
    @Unique
    private float volcanic$barTipY;
    @Unique
    private long volcanic$barTipMillis;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void modifyBrandBackground(CallbackInfo ci) {
        BRAND_BACKGROUND = () -> 0xFF000000 | VOLCANIC_SURFACE;
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    public void init(CallbackInfo ci) {
        this.volcanic$titleTex = ResourceLocation.fromNamespaceAndPath(Initializer.MOD_ID, "textures/gui/volcanic_wordmark.png");
    }

    @Inject(method = "render", at = @At("TAIL"))
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        long now = Util.getMillis();
        float dt = this.volcanic$lastMillis == 0L ? 0.0f : Mth.clamp((now - this.volcanic$lastMillis) / 1000.0f, 0.0f, 0.1f);
        this.volcanic$lastMillis = now;
        this.volcanic$time += dt;

        float alpha = this.volcanic$overlayAlpha(now);
        if (alpha <= 0.004f) {
            return;
        }

        int w = guiGraphics.guiWidth();
        int h = guiGraphics.guiHeight();
        if (w <= 0 || h <= 0) {
            return;
        }

        int mojangHalf = (int) (Math.min(w * 0.75, h) * 0.25 * 0.5);
        int mojangBottom = h / 2 + mojangHalf;
        int barTop = (int) (h * 0.8325) - 5;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        this.volcanic$drawCaldera(guiGraphics, w, h, mojangBottom, alpha);
        this.volcanic$drawMark(guiGraphics, w, h, alpha);
        this.volcanic$updateSmoke(guiGraphics, w, dt, alpha);
        this.volcanic$updateEmbers(guiGraphics, w, h, now, dt, alpha);
        this.volcanic$drawTitle(guiGraphics, w, mojangBottom, barTop, alpha);

        RenderSystem.disableBlend();
    }

    @Inject(method = "drawProgressBar", at = @At("HEAD"), cancellable = true)
    private void volcanic$drawBar(GuiGraphics guiGraphics, int minX, int minY, int maxX, int maxY, float partialTick, CallbackInfo ci) {
        ci.cancel();

        int a = Math.round(255.0f * Mth.clamp(partialTick, 0.0f, 1.0f));
        if (a <= 0) {
            return;
        }

        int inner = maxX - minX - 4;
        if (inner <= 0 || maxY - minY < 6) {
            return;
        }

        guiGraphics.fill(minX + 1, maxY, maxX - 1, maxY + 2, (Math.round(a * 0.5f) << 24) | VOLCANIC_SURFACE_DEEP);

        int border = (a << 24) | VOLCANIC_BORDER;
        guiGraphics.fill(minX, minY, maxX, minY + 1, border);
        guiGraphics.fill(minX, maxY - 1, maxX, maxY, border);
        guiGraphics.fill(minX, minY + 1, minX + 1, maxY - 1, border);
        guiGraphics.fill(maxX - 1, minY + 1, maxX, maxY - 1, border);

        guiGraphics.fillGradient(minX + 1, minY + 1, maxX - 1, maxY - 1,
                (a << 24) | VOLCANIC_SURFACE_DEEP, (a << 24) | 0x1B100D);

        int fillW = Mth.ceil(inner * Mth.clamp(this.currentProgress, 0.0f, 1.0f));
        if (fillW <= 0) {
            return;
        }

        int x0 = minX + 2;
        int x1 = x0 + fillW;
        int y0 = minY + 2;
        int y1 = maxY - 2;

        guiGraphics.fillGradient(x0, y0, x1, y1, (a << 24) | 0xB33F16, (a << 24) | 0x5E2110);

        int hot = Math.min(fillW, 4);
        guiGraphics.fillGradient(x1 - hot, y0, x1, y1, (a << 24) | VOLCANIC_ACCENT, (a << 24) | 0xA8330E);

        int tipA = Math.round(a * (0.7f + 0.3f * Mth.sin(this.volcanic$time * 5.4f)));
        guiGraphics.fill(x1 - 1, y0, x1, y1, (tipA << 24) | 0xFFD9A0);

        this.volcanic$barTipX = x1;
        this.volcanic$barTipY = (y0 + y1) * 0.5f;
        this.volcanic$barTipMillis = Util.getMillis();
    }

    @Unique
    private float volcanic$overlayAlpha(long now) {
        float out = this.fadeOutStart > -1L ? (float) (now - this.fadeOutStart) / 1000.0f : -1.0f;
        if (out >= 1.0f) {
            return 1.0f - Mth.clamp(out - 1.0f, 0.0f, 1.0f);
        }

        if (this.fadeIn) {
            float in = this.fadeInStart > -1L ? (float) (now - this.fadeInStart) / 500.0f : -1.0f;
            return Mth.clamp(in, 0.0f, 1.0f);
        }

        return 1.0f;
    }

    @Unique
    private void volcanic$drawCaldera(GuiGraphics guiGraphics, int w, int h, int top, float alpha) {
        if (top >= h) {
            return;
        }

        int warmA = Math.round(148.0f * alpha);
        guiGraphics.fillGradient(0, top, w, h, VOLCANIC_CALDERA, (warmA << 24) | VOLCANIC_CALDERA);

        float pulse = 0.5f + 0.5f * Mth.sin(this.volcanic$time * 1.2f);
        int glowA = Math.round((30.0f + 16.0f * pulse) * alpha);
        if (glowA <= 0) {
            return;
        }

        int glowTop = top + (h - top) / 2;
        guiGraphics.fillGradient(0, glowTop, w, h, VOLCANIC_ACCENT, (glowA << 24) | VOLCANIC_ACCENT);
    }

    @Unique
    private void volcanic$drawMark(GuiGraphics guiGraphics, int w, int h, float alpha) {
        this.volcanic$ventX = w * 0.5f;
        this.volcanic$ventY = h - 2.0f;
        this.volcanic$ventSpread = Math.max(16.0f, w * 0.10f);
    }

    @Unique
    private void volcanic$drawTitle(GuiGraphics guiGraphics, int w, int top, int bottom, float alpha) {
        if (this.volcanic$titleTex == null) {
            return;
        }

        int band = bottom - top;
        if (band < 22) {
            return;
        }

        int titleW = Math.min(Math.round(w * 0.24f), VOLCANIC_TITLE_W);
        int titleH = titleW * VOLCANIC_TITLE_H / VOLCANIC_TITLE_W;
        int maxH = band - 12;

        if (titleH > maxH) {
            titleH = maxH;
            titleW = titleH * VOLCANIC_TITLE_W / VOLCANIC_TITLE_H;
        }

        if (titleH < 9 || titleW < 48 || titleW > w) {
            return;
        }

        int x = (w - titleW) / 2;
        int y = top + (band - titleH) / 2;

        guiGraphics.setColor(1.0f, 1.0f, 1.0f, alpha);
        guiGraphics.blit(this.volcanic$titleTex, x, y, titleW, titleH,
                0.0f, 0.0f, VOLCANIC_TITLE_W, VOLCANIC_TITLE_H, VOLCANIC_TITLE_W, VOLCANIC_TITLE_H);
        guiGraphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    @Unique
    private void volcanic$updateEmbers(GuiGraphics guiGraphics, int w, int h, long now, float dt, float alpha) {
        this.volcanic$emberBudget = Math.min(this.volcanic$emberBudget + dt * 26.0f, 6.0f);

        boolean barLive = now - this.volcanic$barTipMillis < 250L;

        while (this.volcanic$emberBudget >= 1.0f && this.volcanic$embers.size() < 72) {
            this.volcanic$emberBudget -= 1.0f;

            float roll = SplashParticles.RANDOM.nextFloat();
            float sx;
            float sy;
            float vx;
            float vy;
            float span;

            if (barLive && roll < 0.22f) {
                sx = this.volcanic$barTipX;
                sy = this.volcanic$barTipY;
                vx = (SplashParticles.RANDOM.nextFloat() - 0.5f) * 26.0f;
                vy = -(30.0f + SplashParticles.RANDOM.nextFloat() * 55.0f);
                span = 0.7f + SplashParticles.RANDOM.nextFloat() * 0.8f;
            } else if (roll < 0.62f) {
                float side = SplashParticles.RANDOM.nextFloat() - 0.5f;
                sx = this.volcanic$ventX + side * this.volcanic$ventSpread;
                sy = this.volcanic$ventY + SplashParticles.RANDOM.nextFloat() * 4.0f;
                vx = side * 34.0f + (SplashParticles.RANDOM.nextFloat() - 0.5f) * 14.0f;
                vy = -(70.0f + SplashParticles.RANDOM.nextFloat() * 75.0f);
                span = 2.6f + SplashParticles.RANDOM.nextFloat() * 2.4f;
            } else {
                sx = SplashParticles.RANDOM.nextFloat() * w;
                sy = h + 2.0f;
                vx = (SplashParticles.RANDOM.nextFloat() - 0.5f) * 10.0f;
                vy = -(34.0f + SplashParticles.RANDOM.nextFloat() * 46.0f);
                span = 3.4f + SplashParticles.RANDOM.nextFloat() * 3.0f;
            }

            int size = SplashParticles.RANDOM.nextFloat() < 0.18f ? 3 : (SplashParticles.RANDOM.nextFloat() < 0.5f ? 1 : 2);
            this.volcanic$embers.add(new Ember(sx, sy, vx, vy, span, size));
        }

        Iterator<Ember> it = this.volcanic$embers.iterator();
        while (it.hasNext()) {
            Ember e = it.next();

            e.life += dt / e.lifeSpan;
            e.vy += 30.0f * dt;
            e.x += e.vx * dt;
            e.y += e.vy * dt;

            if (e.life >= 1.0f || e.y < -6.0f) {
                it.remove();
                continue;
            }

            float heat = 1.0f - e.life;
            float flicker = 0.68f + 0.32f * Mth.sin(e.swayPhase + this.volcanic$time * e.swaySpeed * 3.4f);
            float fade = e.life < 0.1f
                    ? e.life / 0.1f
                    : Mth.clamp((1.0f - e.life) / 0.42f, 0.0f, 1.0f);

            int a = Math.round(255.0f * fade * flicker * alpha);
            if (a <= 2) {
                continue;
            }

            int r = (int) Mth.lerp(heat, 122.0f, 255.0f);
            int g = (int) Mth.lerp(heat * heat, 24.0f, 206.0f);
            int b = (int) Mth.lerp(heat * heat * heat, 8.0f, 128.0f);

            int sx = Mth.floor(e.x + Mth.sin(e.swayPhase + this.volcanic$time * e.swaySpeed) * e.swayAmp);
            int sy = Mth.floor(e.y);
            int s = e.size;

            guiGraphics.fill(sx, sy, sx + s, sy + s, (a << 24) | (r << 16) | (g << 8) | b);

            if (s == 3 && heat > 0.55f) {
                guiGraphics.fill(sx + 1, sy + 1, sx + 2, sy + 2, (a << 24) | 0xFFE6BE);
            }
        }
    }

    @Unique
    private void volcanic$updateSmoke(GuiGraphics guiGraphics, int w, float dt, float alpha) {
        this.volcanic$smokeBudget = Math.min(this.volcanic$smokeBudget + dt * 1.3f, 2.0f);

        while (this.volcanic$smokeBudget >= 1.0f && this.volcanic$smokes.size() < 10) {
            this.volcanic$smokeBudget -= 1.0f;

            float sx = this.volcanic$ventX + (SplashParticles.RANDOM.nextFloat() - 0.5f) * this.volcanic$ventSpread;
            float vx = (SplashParticles.RANDOM.nextFloat() - 0.5f) * 7.0f;
            float vy = -(16.0f + SplashParticles.RANDOM.nextFloat() * 13.0f);
            float span = 5.5f + SplashParticles.RANDOM.nextFloat() * 3.5f;
            float base = Math.max(7.0f, w * 0.012f) * (0.8f + SplashParticles.RANDOM.nextFloat() * 0.7f);

            this.volcanic$smokes.add(new Smoke(sx, this.volcanic$ventY, vx, vy, span, base));
        }

        Iterator<Smoke> it = this.volcanic$smokes.iterator();
        while (it.hasNext()) {
            Smoke s = it.next();

            s.life += dt / s.lifeSpan;
            s.x += (s.vx + Mth.sin(s.wobblePhase + this.volcanic$time * 0.55f) * 5.0f) * dt;
            s.y += s.vy * dt;

            if (s.life >= 1.0f) {
                it.remove();
                continue;
            }

            float fadeIn = Mth.clamp(s.life / 0.18f, 0.0f, 1.0f);
            float fadeOut = Mth.clamp((1.0f - s.life) / 0.55f, 0.0f, 1.0f);
            float puffAlpha = 0.36f * fadeIn * fadeOut * alpha;
            if (puffAlpha <= 0.012f) {
                continue;
            }

            float spread = s.baseSize * (0.55f + s.life * 2.1f);
            int r = (int) Mth.lerp(s.life, 128.0f, 48.0f);
            int g = (int) Mth.lerp(s.life, 96.0f, 40.0f);
            int b = (int) Mth.lerp(s.life, 82.0f, 38.0f);
            int rgb = (r << 16) | (g << 8) | b;

            for (int i = 0; i < s.lobeX.length; i++) {
                float wobble = Mth.sin(s.wobblePhase + i * 1.7f + this.volcanic$time * 0.8f);
                float radius = s.lobeR[i] * spread * (0.88f + 0.12f * wobble);
                if (radius < 1.0f) {
                    continue;
                }

                int lobeA = Math.round(255.0f * puffAlpha * (0.55f + 0.45f * s.lobeR[i]));
                if (lobeA <= 2) {
                    continue;
                }

                int lx = Mth.floor(s.x + s.lobeX[i] * spread + wobble * spread * 0.07f);
                int ly = Mth.floor(s.y + s.lobeY[i] * spread);
                int half = Mth.floor(radius);

                guiGraphics.fill(lx - half, ly - half, lx + half, ly + half, (lobeA << 24) | rgb);
            }
        }
    }
}
