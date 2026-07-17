package net.vulkanmod.mixin.render;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.dimension.DimensionType;
import net.vulkanmod.Initializer;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Rebuilds the 16x16 lightmap with a custom brightness curve.
@Mixin(LightTexture.class)
public abstract class LightTextureMixin {

    @Shadow @Final private DynamicTexture lightTexture;
    @Shadow @Final private NativeImage lightPixels;
    @Shadow @Final private Minecraft minecraft;
    @Shadow @Final private GameRenderer renderer;
    @Shadow private boolean updateLightTexture;
    @Shadow private float blockLightRedFlicker;

    @Shadow protected abstract float getDarknessGamma(float partialTick);

    @Shadow protected abstract float calculateDarknessScale(net.minecraft.world.entity.LivingEntity entity, float darknessGamma, float partialTick);

    @Shadow private float notGamma(float value) { throw new AssertionError(); }

    @Shadow public static float getBrightness(DimensionType dimensionType, int lightLevel) { throw new AssertionError(); }

    private static void volcanic$clampColor(Vector3f color) {
        color.set(Mth.clamp(color.x, 0.0F, 1.0F), Mth.clamp(color.y, 0.0F, 1.0F), Mth.clamp(color.z, 0.0F, 1.0F));
    }

    @Inject(method = "updateLightTexture", at = @At("HEAD"), cancellable = true)
    private void volcanic$updateLightTexture(float partialTick, CallbackInfo ci) {
        if (!Initializer.CONFIG.customLightmap) {
            return;
        }

        if (!this.updateLightTexture) {
            ci.cancel();
            return;
        }

        this.updateLightTexture = false;
        this.minecraft.getProfiler().push("lightTex");
        ClientLevel level = this.minecraft.level;
        if (level != null) {
            final float brightness = Initializer.CONFIG.lightBrightness;
            final float nightDarkness = Initializer.CONFIG.nightDarkness;
            final float torchIntensity = Initializer.CONFIG.torchIntensity;
            final float caveAmbient = Initializer.CONFIG.caveAmbient;

            // Vanilla sky brightness term
            float skyDarken = level.getSkyDarken(1.0F);
            float skyTerm;
            if (level.getSkyFlashTime() > 0) {
                skyTerm = 1.0F;
            } else {
                skyTerm = skyDarken * 0.95F + 0.05F;
            }

            // Darkness mob effect
            float darknessScale = this.minecraft.options.darknessEffectScale().get().floatValue();
            float darknessGamma = this.getDarknessGamma(partialTick) * darknessScale;
            float darknessSub = this.calculateDarknessScale(this.minecraft.player, darknessGamma, partialTick) * darknessScale;

            // Night-vision / conduit power factor
            float waterVision = this.minecraft.player.getWaterVision();
            float nightVision;
            if (this.minecraft.player.hasEffect(MobEffects.NIGHT_VISION)) {
                nightVision = GameRenderer.getNightVisionScale(this.minecraft.player, partialTick);
            } else if (waterVision > 0.0F && this.minecraft.player.hasEffect(MobEffects.CONDUIT_POWER)) {
                nightVision = waterVision;
            } else {
                nightVision = 0.0F;
            }

            // Night amount: 0 at full day, 1 at deep night. Applied to the sky term only.
            float nightAmount = Mth.clamp(1.0F - skyDarken, 0.0F, 1.0F);
            float skyNightMul = Mth.lerp(nightAmount, 1.0F, 1.0F - nightDarkness);

            // Sky-light colour with a cool tint at night
            Vector3f skyColor = new Vector3f(skyDarken, skyDarken, 1.0F).lerp(new Vector3f(1.0F, 1.0F, 1.0F), 0.35F);
            skyColor.mul(0.98F * skyNightMul, 0.99F * skyNightMul, 1.0F * skyNightMul);

            // Warm block light, tint gradient from ember-orange edge to warm-white source
            float blockFlicker = this.blockLightRedFlicker + 1.5F;
            Vector3f torchNear = new Vector3f(1.05F, 0.93F, 0.72F);
            Vector3f torchFar = new Vector3f(1.00F, 0.52F, 0.24F);

            Vector3f color = new Vector3f();

            for (int sky = 0; sky < 16; sky++) {
                for (int block = 0; block < 16; block++) {
                    float skyBrightness = getBrightness(level.dimensionType(), sky) * skyTerm;
                    // Reshaped block-light curve (pow < 1 lifts the mid levels)
                    float blockBase = (float) Math.pow(getBrightness(level.dimensionType(), block), 0.70);
                    float blockBrightness = blockBase * blockFlicker;

                    boolean forceBright = level.effects().forceBrightLightmap();
                    if (forceBright) {
                        // Nether-style flat bright lightmap, vanilla look
                        float f10 = blockBrightness * ((blockBrightness * 0.6F + 0.4F) * 0.6F + 0.4F);
                        float f11 = blockBrightness * (blockBrightness * blockBrightness * 0.6F + 0.4F);
                        color.set(blockBrightness, f10, f11);
                        color.lerp(new Vector3f(0.99F, 1.12F, 1.0F), 0.25F);
                        volcanic$clampColor(color);
                    } else {
                        // Warm-tinted block light, ember-orange → warm-white toward the source
                        Vector3f torchTint = new Vector3f(torchFar).lerp(torchNear, block / 15.0F);
                        Vector3f blockTerm = torchTint.mul(blockBrightness * torchIntensity);

                        Vector3f skyTermColor = new Vector3f(skyColor).mul(skyBrightness);

                        // Additive combine over an ambient floor
                        color.set(caveAmbient, caveAmbient, caveAmbient);
                        color.add(blockTerm);
                        color.add(skyTermColor);

                        color.mul(brightness);

                        // Vanilla desaturating nudge
                        color.lerp(new Vector3f(0.75F, 0.75F, 0.75F), 0.04F);

                        // World-darkening (boss fights / bad omen)
                        if (this.renderer.getDarkenWorldAmount(partialTick) > 0.0F) {
                            float darken = this.renderer.getDarkenWorldAmount(partialTick);
                            Vector3f darkened = new Vector3f(color).mul(0.7F, 0.6F, 0.6F);
                            color.lerp(darkened, darken);
                        }
                    }

                    // Night vision / conduit: brighten toward white
                    if (nightVision > 0.0F) {
                        float maxComponent = Math.max(color.x(), Math.max(color.y(), color.z()));
                        if (maxComponent > 0.0F && maxComponent < 1.0F) {
                            float inv = 1.0F / maxComponent;
                            Vector3f brightened = new Vector3f(color).mul(inv);
                            color.lerp(brightened, nightVision);
                        }
                    }

                    if (!forceBright) {
                        if (darknessSub > 0.0F) {
                            color.add(-darknessSub, -darknessSub, -darknessSub);
                        }
                        volcanic$clampColor(color);
                    }

                    // Gamma (vanilla notGamma curve) then desaturating nudge
                    float gamma = this.minecraft.options.gamma().get().floatValue();
                    Vector3f gammaColor = new Vector3f(this.notGamma(color.x), this.notGamma(color.y), this.notGamma(color.z));
                    color.lerp(gammaColor, Math.max(0.0F, gamma - darknessGamma));
                    color.lerp(new Vector3f(0.75F, 0.75F, 0.75F), 0.04F);

                    // Post-gamma night & cave darkening, so a high gamma cannot wash it out.
                    // Torch-lit cells are spared; night vision bypasses it.
                    if (!forceBright) {
                        float skyFrac = sky / 15.0F;
                        float blockFrac = block / 15.0F;
                        float nightMul = Mth.lerp(nightAmount * skyFrac * (1.0F - blockFrac * 0.85F),
                                1.0F, 1.0F - nightDarkness * 0.8F);
                        float caveness = (1.0F - skyFrac) * (1.0F - blockFrac);
                        float caveMul = Mth.lerp(caveness, 1.0F,
                                Mth.clamp(caveAmbient * 5.0F + 0.20F, 0.0F, 1.0F));
                        float darken = nightMul * caveMul;
                        color.mul(Mth.lerp(nightVision, darken, 1.0F));
                    }

                    volcanic$clampColor(color);
                    color.mul(255.0F);

                    int r = (int) color.x();
                    int g = (int) color.y();
                    int b = (int) color.z();
                    this.lightPixels.setPixelRGBA(block, sky, 0xFF000000 | b << 16 | g << 8 | r);
                }
            }

            this.lightTexture.upload();
            this.minecraft.getProfiler().pop();
        }

        ci.cancel();
    }
}
