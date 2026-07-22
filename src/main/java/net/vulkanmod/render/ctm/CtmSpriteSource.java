package net.vulkanmod.render.ctm;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.renderer.texture.atlas.SpriteSourceType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.vulkanmod.Initializer;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public class CtmSpriteSource implements SpriteSource {

    private static final SpriteSourceType TYPE = new SpriteSourceType(MapCodec.unit(CtmSpriteSource::new));
    private static final AtomicBoolean WARNED = new AtomicBoolean(false);

    @Override
    public void run(ResourceManager resourceManager, SpriteSource.Output output) {
        for (ResourceLocation id : CtmAtlasRegistrar.additionalSprites()) {
            try {
                ResourceLocation file = id.withPath(id.getPath() + ".png");
                Optional<Resource> resource = resourceManager.getResource(file);
                if (resource.isPresent()) {
                    output.add(id, resource.get());
                }
            } catch (Throwable t) {
                if (WARNED.compareAndSet(false, true)) {
                    Initializer.LOGGER.error("Failed to load CTM sprite {}", id, t);
                }
            }
        }
    }

    @Override
    public SpriteSourceType type() {
        return TYPE;
    }
}
