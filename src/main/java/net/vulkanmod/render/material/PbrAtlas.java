package net.vulkanmod.render.material;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.vulkanmod.Initializer;
import net.vulkanmod.mixin.texture.image.NativeImageAccessor;
import net.vulkanmod.vulkan.texture.VulkanImage;
import org.lwjgl.system.MemoryUtil;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.lwjgl.vulkan.VK10.VK_IMAGE_USAGE_SAMPLED_BIT;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_USAGE_TRANSFER_DST_BIT;

public final class PbrAtlas {
    private static final int FLAT_NORMAL = 0xFFFF8080;

    private static final Map<ResourceLocation, VulkanImage> NORMAL_ATLASES = new HashMap<>();

    private PbrAtlas() {
    }

    public static void build(ResourceLocation atlasLocation, SpriteLoader.Preparations preparations) {
        if (!TextureAtlas.LOCATION_BLOCKS.equals(atlasLocation)) {
            return;
        }
        try {
            ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
            int w = preparations.width();
            int h = preparations.height();

            NativeImage normal = new NativeImage(NativeImage.Format.RGBA, w, h, false);
            fill(normal, FLAT_NORMAL);

            int found = 0;
            int total = 0;
            for (TextureAtlasSprite sprite : preparations.regions().values()) {
                total++;
                NativeImage variant = loadVariant(resourceManager, sprite.contents().name(), "_n");
                if (variant == null) {
                    continue;
                }
                int sw = sprite.contents().width();
                int sh = sprite.contents().height();
                if (variant.getWidth() == sw && variant.getHeight() >= sh) {
                    blit(normal, variant, sprite.getX(), sprite.getY(), sw, sh);
                    found++;
                }
                variant.close();
            }

            VulkanImage image = upload(normal, w, h);
            normal.close();

            VulkanImage old = NORMAL_ATLASES.put(atlasLocation, image);
            if (old != null) {
                old.free();
            }
            Initializer.LOGGER.info("[PBR] Normal atlas '{}': {}/{} sprites had _n ({}x{})", atlasLocation, found, total, w, h);
        } catch (Exception e) {
            Initializer.LOGGER.error("[PBR] Failed to build normal atlas for {}: {}", atlasLocation, e.toString());
        }
    }

    public static VulkanImage getBlockNormalAtlas() {
        return NORMAL_ATLASES.get(TextureAtlas.LOCATION_BLOCKS);
    }

    private static void fill(NativeImage image, int abgr) {
        long pixels = ((NativeImageAccessor) (Object) image).getPixels();
        int count = image.getWidth() * image.getHeight();
        ByteBuffer buffer = MemoryUtil.memByteBuffer(pixels, count * 4);
        for (int i = 0; i < count; i++) {
            buffer.putInt(i * 4, abgr);
        }
    }

    private static void blit(NativeImage dst, NativeImage src, int dx, int dy, int w, int h) {
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                dst.setPixelRGBA(dx + x, dy + y, src.getPixelRGBA(x, y));
            }
        }
    }

    private static NativeImage loadVariant(ResourceManager resourceManager, ResourceLocation spriteName, String suffix) {
        ResourceLocation location = ResourceLocation.fromNamespaceAndPath(
                spriteName.getNamespace(), "textures/" + spriteName.getPath() + suffix + ".png");
        Optional<Resource> resource = resourceManager.getResource(location);
        if (resource.isEmpty()) {
            return null;
        }
        try (InputStream stream = resource.get().open()) {
            return NativeImage.read(stream);
        } catch (Exception e) {
            return null;
        }
    }

    private static VulkanImage upload(NativeImage image, int w, int h) {
        VulkanImage vulkanImage = VulkanImage.builder(w, h)
                .setFormat(VulkanImage.DefaultFormat)
                .setUsage(VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_SAMPLED_BIT)
                .setLinearFiltering(false)
                .setClamp(false)
                .createVulkanImage();
        long pixels = ((NativeImageAccessor) (Object) image).getPixels();
        ByteBuffer buffer = MemoryUtil.memByteBuffer(pixels, w * h * 4);
        vulkanImage.uploadSubTextureAsync(0, w, h, 0, 0, 0, 0, 0, buffer);
        return vulkanImage;
    }
}
