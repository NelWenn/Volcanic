package net.vulkanmod.mixin.render;

import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LevelRenderer;
import net.vulkanmod.Initializer;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LevelRenderer.class)
public class SkySunMixin {

    @Redirect(
            method = "renderSky",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/BufferUploader;drawWithShader(Lcom/mojang/blaze3d/vertex/MeshData;)V",
                    ordinal = 1
            )
    )
    private void skipSun(MeshData meshData) {
        if (Initializer.CONFIG.shadersEnabled && Initializer.CONFIG.isCamille()) {
            meshData.close();
            return;
        }
        BufferUploader.drawWithShader(meshData);
    }

    @Redirect(
            method = "renderSky",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/PoseStack;mulPose(Lorg/joml/Quaternionf;)V",
                    ordinal = 3
            )
    )
    private void tiltCelestial(PoseStack posestack, Quaternionf ypMinus90) {
        if (Initializer.CONFIG.shadersEnabled && Initializer.CONFIG.isCamille()) {
            posestack.mulPose(Axis.XP.rotationDegrees((float) Math.toDegrees(net.vulkanmod.vulkan.VRenderSystem.SUN_TILT)));
        }
        posestack.mulPose(ypMinus90);
    }
}
