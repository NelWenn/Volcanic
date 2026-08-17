package net.vulkanmod.render.chunk.build;

import it.unimi.dsi.fastutil.objects.Object2ByteLinkedOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.MushroomBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.TallGrassBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.common.util.TriState;
import net.vulkanmod.compat.PolytoneCompat;
import net.vulkanmod.render.chunk.build.light.LightPipeline;
import net.vulkanmod.render.chunk.build.light.data.QuadLightData;
import net.vulkanmod.render.chunk.build.thread.BuilderResources;
import net.vulkanmod.render.ctm.Ctm;
import net.vulkanmod.render.ctm.CtmOverlayEmitter;
import net.vulkanmod.render.ctm.CtmResult;
import net.vulkanmod.render.ctm.CtmUvQuad;
import net.vulkanmod.render.model.quad.QuadUtils;
import net.vulkanmod.render.model.quad.QuadView;
import net.vulkanmod.render.vertex.TerrainBufferBuilder;
import net.vulkanmod.render.vertex.VertexUtil;
import net.vulkanmod.vulkan.util.ColorUtil;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BlockRenderer {

    static final Direction[] DIRECTIONS = Direction.values();
    private static BlockColors blockColors;

    RandomSource randomSource = RandomSource.createNewThreadLocalInstance();

    Vector3f pos;
    BlockPos blockPos;
    BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();

    final Map<BlockState, List<BakedQuad>[]> quadCache = new HashMap<>(); // indexed by ordinal()

    final Map<BlockState, Boolean> cacheableMap = new HashMap<>();

    BuilderResources resources;

    BlockState blockState;

    int waveCode;
    float blockBaseY;
    boolean upperHalf;

    public void setResources(BuilderResources resources) {
        this.resources = resources;
    }

    static int waveCode(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof LeavesBlock) {
            return 2;
        }
        if (block instanceof FlowerBlock
                || block instanceof TallGrassBlock
                || block instanceof DoublePlantBlock
                || block instanceof CropBlock
                || block instanceof StemBlock
                || block instanceof SugarCaneBlock
                || block instanceof SweetBerryBushBlock
                || block instanceof SaplingBlock
                || block instanceof NetherWartBlock) {
            return 1;
        }
        if (block instanceof BushBlock && !(block instanceof MushroomBlock)) {
            return 1;
        }
        return 0;
    }

    final Object2ByteLinkedOpenHashMap<Block.BlockStatePairKey> occlusionCache = new Object2ByteLinkedOpenHashMap<>(2048, 0.25F) {
        protected void rehash(int i) {
        }
    };

    public BlockRenderer() {
        occlusionCache.defaultReturnValue((byte) 127);
    }

    public static void setBlockColors(BlockColors blockColors) {
        BlockRenderer.blockColors = blockColors;
    }

    public void renderBlock(BlockState blockState, BlockPos blockPos, Vector3f pos, TerrainBufferBuilder bufferBuilder,
                            BakedModel model, ModelData modelData, RenderType renderType) {
        this.pos = pos;
        this.blockPos = blockPos;
        this.blockState = blockState;

        long seed = blockState.getSeed(blockPos);

        tessellateBlock(model, modelData, renderType, bufferBuilder, seed);
    }

    private boolean quadsEqual(List<BakedQuad> a, List<BakedQuad> b) {
        if (a.size() != b.size())
            return false;

        for (int i = 0; i < a.size(); i++)
            if (!a.get(i).equals(b.get(i)))
                return false;

        return true;
    }

    private boolean isCacheable(BlockState state, ModelData modelData, BakedModel model, Direction dir, RenderType type) {
        if (!modelData.getProperties().isEmpty()) return false;

        Boolean known = cacheableMap.get(state);
        if (known != null)
            return known;

        // test two seeds
        randomSource.setSeed(42L);
        List<BakedQuad> a = model.getQuads(state, dir, randomSource, modelData, type);

        randomSource.setSeed(1337L);
        List<BakedQuad> b = model.getQuads(state, dir, randomSource, modelData, type);

        boolean cacheable = quadsEqual(a, b);
        cacheableMap.put(state, cacheable);
        return cacheable;
    }

    // adding some cache 0_0
    private List<BakedQuad> getOrComp(BakedModel model, BlockState state, ModelData modelData, long seed, Direction dir, RenderType type) {
        if (!isCacheable(state, modelData, model, dir, type)) {
            randomSource.setSeed(seed);
            return model.getQuads(state, dir, randomSource, modelData, type);
        }

        // Unchecked but it's fine
        @SuppressWarnings("unchecked")
        List<BakedQuad>[] perDir = quadCache.computeIfAbsent(state, s -> new List[DIRECTIONS.length + 1]);
        int index = dir != null ? dir.ordinal() : DIRECTIONS.length;

        List<BakedQuad> cached = perDir[index];
        if (cached != null)
            return cached;

        randomSource.setSeed(seed);
        List<BakedQuad> comp = model.getQuads(state, dir, randomSource, modelData, type);
        perDir[index] = comp;
        return comp;
    }

    public void tessellateBlock(BakedModel bakedModel, ModelData modelData, RenderType renderType,
                                TerrainBufferBuilder bufferBuilder, long seed) {
        Vec3 offset = blockState.getOffset(resources.region, blockPos);
        offset = PolytoneCompat.modifyOffset(offset, blockState, resources.region, blockPos);

        pos.add((float) offset.x, (float) offset.y, (float) offset.z);

        this.waveCode = waveCode(blockState);
        this.blockBaseY = blockPos.getY() & 15;
        this.upperHalf = blockState.hasProperty(DoublePlantBlock.HALF)
                && blockState.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.UPPER;

        TriState ambientOcclusion = bakedModel.useAmbientOcclusion(blockState, modelData, renderType);

        boolean modelUsesAO = ambientOcclusion.isDefault() ? bakedModel.useAmbientOcclusion() : ambientOcclusion.isTrue();
        boolean useAO = Minecraft.useAmbientOcclusion() && blockState.getLightEmission() == 0 && modelUsesAO;

        LightPipeline lightPipeline = useAO ? resources.smoothLightPipeline : resources.flatLightPipeline;

        for (int i = 0; i < DIRECTIONS.length; ++i) {
            Direction direction = DIRECTIONS[i];

            // fix, get quads method was called before checking occlusion. GetQuads contain internal iterators
            // which create an extreme heap allocation, the garbage collector will be overwhelmed
            // which creates huge froze loading chunks
            mutableBlockPos.setWithOffset(blockPos, direction);
            if (!shouldRenderFace(blockState, direction, mutableBlockPos))
                continue;

            randomSource.setSeed(seed);
            List<BakedQuad> quads = getOrComp(bakedModel, blockState, modelData, seed, direction, renderType);

            if (!quads.isEmpty())
                renderModelFace(bufferBuilder, quads, lightPipeline, direction);
        }

        randomSource.setSeed(seed);

        List<BakedQuad> quads = bakedModel.getQuads(blockState, null, randomSource, modelData, renderType);

        if (!quads.isEmpty())
            renderModelFace(bufferBuilder, quads, lightPipeline, null);
    }

    private void renderModelFace(TerrainBufferBuilder bufferBuilder, List<BakedQuad> quads, LightPipeline lightPipeline, Direction cullFace) {
        QuadLightData quadLightData = resources.quadLightData;

        for (int i = 0; i < quads.size(); ++i) {
            BakedQuad bakedQuad = quads.get(i);

            bakedQuad = PolytoneCompat.maybeModifyQuad(bakedQuad, resources.region, blockState, blockPos);
            QuadView quadView = (QuadView) bakedQuad;
            CtmResult ctm = CtmResult.none();

            try {
                if (Ctm.isActive()) {
                    ctm = Ctm.resolve(bakedQuad.getSprite(), blockState, blockPos, bakedQuad.getDirection(), resources.region);
                    if (ctm.kind() == CtmResult.Kind.SWAP) {
                        quadView = new CtmUvQuad(quadView, bakedQuad.getSprite(), ctm.sprite());
                    }
                }
            } catch (Throwable t) {
                ctm = CtmResult.none();
            }

            lightPipeline.calculate(quadView, blockPos, quadLightData, cullFace, bakedQuad.getDirection(), bakedQuad.isShade());
            putQuadData(bufferBuilder, quadView, quadLightData);

            if (ctm.kind() == CtmResult.Kind.OVERLAY) {
                try {
                    TerrainBufferBuilder overlayBuffer = resources.builderPack.builder(ctm.layer());
                    CtmUvQuad overlayQuad = new CtmUvQuad((QuadView) bakedQuad, bakedQuad.getSprite(), ctm.sprite());
                    CtmOverlayEmitter.emit(overlayBuffer, overlayQuad, quadLightData, this.pos, this.waveCode, this.blockBaseY, this.upperHalf, ctm.tintIndex(), blockState, resources.region, blockPos);
                } catch (Throwable ignored) {}
            }
        }
    }

    private void putQuadData(TerrainBufferBuilder bufferBuilder, QuadView quadView, QuadLightData quadLightData) {
        float r, g, b;

        if (quadView.isTinted()) {
            int color = blockColors.getColor(blockState, resources.region, blockPos, quadView.getColorIndex());

            r = ColorUtil.ARGB.unpackR(color);
            g = ColorUtil.ARGB.unpackG(color);
            b = ColorUtil.ARGB.unpackB(color);
        } else {
            r = 1.0F;
            g = 1.0F;
            b = 1.0F;
        }

        putQuadData(bufferBuilder, pos, quadView, quadLightData, r, g, b, this.waveCode, this.blockBaseY, this.upperHalf);
    }

    public static int tint(BlockState state, net.minecraft.world.level.BlockAndTintGetter region, BlockPos pos, int tintIndex) {
        return blockColors.getColor(state, region, pos, tintIndex);
    }

    public static void putQuadData(TerrainBufferBuilder bufferBuilder, Vector3f pos, QuadView quad, QuadLightData quadLightData, float red, float green, float blue, int waveCode, float blockBaseY, boolean upperHalf) {
        Vec3i normal = quad.getFacingDirection().getNormal();
        int packedNormal = VertexUtil.packNormal(normal.getX(), normal.getY(), normal.getZ());

        float[] brightnessArr = quadLightData.br;
        int[] lights = quadLightData.lm;

        int idx = QuadUtils.getIterationStartIdx(brightnessArr, lights);

        bufferBuilder.ensureCapacity();

        float minU = quad.getU(0), maxU = minU, minV = quad.getV(0), maxV = minV;

        for (int k = 1; k < 4; ++k) {
            float qu = quad.getU(k), qv = quad.getV(k);
            minU = Math.min(minU, qu); maxU = Math.max(maxU, qu);
            minV = Math.min(minV, qv); maxV = Math.max(maxV, qv);
        }

        bufferBuilder.setQuadMidUV((minU + maxU) * 0.5f, (minV + maxV) * 0.5f);

        for (byte i = 0; i < 4; ++i) {
            final float x = pos.x() + quad.getX(idx);
            final float y = pos.y() + quad.getY(idx);
            final float z = pos.z() + quad.getZ(idx);

            final float r, g, b;
            final float quadR, quadG, quadB;

            final int quadColor = quad.getColor(idx);
            quadR = ColorUtil.RGBA.unpackR(quadColor);
            quadG = ColorUtil.RGBA.unpackG(quadColor);
            quadB = ColorUtil.RGBA.unpackB(quadColor);

            final float brightness = brightnessArr[idx];
            r = quadR * brightness * red;
            g = quadG * brightness * green;
            b = quadB * brightness * blue;

            final int color = ColorUtil.RGBA.pack(r, g, b, 1.0f);
            int light = (lights[idx] & ~0xF) | waveCode;

            if (waveCode != 0) {
                float localHeight = y - blockBaseY;
                float weight = upperHalf ? (1.0f + localHeight) * 0.5f : localHeight;
                weight = weight < 0.0f ? 0.0f : (weight > 1.0f ? 1.0f : weight);
                light = (light & ~0xF0000) | (Math.round(weight * 15.0f) << 16);
            }

            final float u = quad.getU(idx);
            final float v = quad.getV(idx);

            bufferBuilder.vertex(x, y, z, color, u, v, light, packedNormal);

            idx = (idx + 1) & 0b11;
        }

    }

    public boolean shouldRenderFace(BlockState blockState, Direction direction, BlockPos adjPos) {
        BlockGetter blockGetter = resources.region;
        BlockState adjBlockState = blockGetter.getBlockState(adjPos);

        if (net.vulkanmod.Initializer.CONFIG.leavesQuality > 0
                && blockState.getBlock() instanceof LeavesBlock
                && adjBlockState.getBlock() instanceof LeavesBlock) {
            return false;
        }

        if (blockState.skipRendering(adjBlockState, direction)) {
            return false;
        }

        if (adjBlockState.canOcclude()) {
            VoxelShape shape = blockState.getFaceOcclusionShape(blockGetter, blockPos, direction);

            if (shape.isEmpty())
                return true;

            VoxelShape adjShape = adjBlockState.getFaceOcclusionShape(blockGetter, adjPos, direction.getOpposite());

            if (adjShape.isEmpty())
                return true;

            if (shape == Shapes.block() && adjShape == Shapes.block()) {
                return false;
            }

            Block.BlockStatePairKey blockStatePairKey = new Block.BlockStatePairKey(blockState, adjBlockState, direction);

            byte b = occlusionCache.getAndMoveToFirst(blockStatePairKey);
            if (b != 127) {
                return b != 0;
            } else {
                boolean bl = Shapes.joinIsNotEmpty(shape, adjShape, BooleanOp.ONLY_FIRST);

                if (occlusionCache.size() == 2048) {
                    occlusionCache.removeLastByte();
                }

                occlusionCache.putAndMoveToFirst(blockStatePairKey, (byte) (bl ? 1 : 0));
                return bl;
            }
        }

        return true;
    }

}

