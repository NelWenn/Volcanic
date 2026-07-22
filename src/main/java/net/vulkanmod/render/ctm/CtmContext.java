package net.vulkanmod.render.ctm;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

public record CtmContext(TextureAtlasSprite sprite, BlockState state, BlockPos pos,
                         Direction face, ResourceLocation biome) {}
