package net.vulkanmod.mixin.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.vulkanmod.Initializer;
import net.vulkanmod.config.ui.core.CoalArt;
import net.vulkanmod.config.ui.core.CoalScene;
import net.vulkanmod.config.ui.core.Rect;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
    private final CoalScene volcanic$coals = new CoalScene(0x5A1FL);

    @Unique
    private static final ResourceLocation VOLCANIC_BED =
            ResourceLocation.fromNamespaceAndPath("vulkanmod", "textures/gui/coalbed.png");

    @Unique
    private static final ResourceLocation VOLCANIC_SPARK =
            ResourceLocation.withDefaultNamespace("textures/particle/flame.png");

    @Unique
    private static final ResourceLocation VOLCANIC_LAVA =
            ResourceLocation.withDefaultNamespace("textures/particle/lava.png");

    @Unique
    private static final ResourceLocation[] VOLCANIC_ZONES = volcanic$zones();

    @Unique
    private static final ResourceLocation[] VOLCANIC_SMOKE = volcanic$smoke();

    @Unique
    private static ResourceLocation[] volcanic$zones() {
        ResourceLocation[] zones = new ResourceLocation[CoalScene.ZONES];
        for (int zone = 0; zone < zones.length; zone++) {
            zones[zone] = ResourceLocation.fromNamespaceAndPath("vulkanmod",
                    "textures/gui/coal_zone_" + zone + ".png");
        }
        return zones;
    }

    @Unique
    private static ResourceLocation[] volcanic$smoke() {
        ResourceLocation[] frames = new ResourceLocation[CoalScene.SMOKE_FRAMES];
        for (int frame = 0; frame < frames.length; frame++) {
            frames[frame] = ResourceLocation.withDefaultNamespace(
                    "textures/particle/big_smoke_" + frame + ".png");
        }
        return frames;
    }



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

    @Unique
    private void volcanic$drawCoals(GuiGraphics guiGraphics, int w, int h, float dt, float alpha) {
        try {
            Rect screen = new Rect(0, 0, w, h);
            this.volcanic$coals.advance(Math.round(dt * 1000.0f), screen);
            Rect bed = this.volcanic$coals.bedRect(screen);
            if (bed.isEmpty()) {
                return;
            }

            guiGraphics.setColor(1.0f, 1.0f, 1.0f, alpha);
            guiGraphics.blit(VOLCANIC_BED, bed.x(), bed.y(), bed.width(), bed.height(),
                    0.0f, 0.0f, CoalArt.TEX_W, CoalArt.TEX_H, CoalArt.TEX_W, CoalArt.TEX_H);

            for (int zone = 0; zone < CoalScene.ZONES; zone++) {
                int tint = this.volcanic$coals.zoneTint(zone);
                guiGraphics.setColor(((tint >> 16) & 0xFF) / 255.0f, ((tint >> 8) & 0xFF) / 255.0f,
                        (tint & 0xFF) / 255.0f, ((tint >>> 24) & 0xFF) / 255.0f * alpha);
                guiGraphics.blit(VOLCANIC_ZONES[zone], bed.x(), bed.y(), bed.width(), bed.height(),
                        0.0f, 0.0f, CoalArt.TEX_W, CoalArt.TEX_H, CoalArt.TEX_W, CoalArt.TEX_H);
            }

            float grow = this.volcanic$coals.particleScale(screen);
            for (int index = 0; index < CoalScene.PARTICLES; index++) {
                if (this.volcanic$coals.waiting(index)) {
                    continue;
                }
                int argb = this.volcanic$coals.argbOf(index);
                int shade = argb >>> 24;
                if (shade == 0) {
                    continue;
                }
                int side = Math.max(2, Math.round(this.volcanic$coals.sizeOf(index) * grow));
                guiGraphics.setColor(((argb >> 16) & 0xFF) / 255.0f, ((argb >> 8) & 0xFF) / 255.0f,
                        (argb & 0xFF) / 255.0f, shade / 255.0f * alpha);
                int source = this.volcanic$coals.kindOf(index) == CoalScene.SMOKE ? 16 : 8;
                ResourceLocation tex = switch (this.volcanic$coals.kindOf(index)) {
                    case CoalScene.SPARK -> VOLCANIC_SPARK;
                    case CoalScene.LAVA -> VOLCANIC_LAVA;
                    default -> VOLCANIC_SMOKE[this.volcanic$coals.smokeFrame(index)];
                };
                guiGraphics.blit(tex, this.volcanic$coals.xOf(index, screen),
                        this.volcanic$coals.yOf(index, screen) - side / 2, side, side,
                        0.0f, 0.0f, source, source, source, source);
            }
        } catch (Throwable ignored) {
            guiGraphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        }
        guiGraphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
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
        this.volcanic$drawCoals(guiGraphics, w, h, dt, alpha);
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


}
