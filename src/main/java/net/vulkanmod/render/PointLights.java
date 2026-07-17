package net.vulkanmod.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.Vec3;
import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.util.MappedBuffer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

// Nearest emissive blocks around the camera as point lights for the post pass.
public class PointLights {

    public static final int MAX_LIGHTS = 32;
    private static final int SCAN_RADIUS = 2;                   // 5x5x5 sections
    private static final int SPAN = 2 * SCAN_RADIUS + 1;
    private static final int TOTAL_COLUMNS = SPAN * SPAN;
    private static final int COLUMNS_PER_TICK = 2;

    private static final Light[] lights = new Light[MAX_LIGHTS];
    private static int lightCount = 0;

    // Time-sliced sweep: scan a few columns per frame, publish when a full sweep finishes.
    private static int sweepIndex = TOTAL_COLUMNS;   // >= TOTAL_COLUMNS: start a fresh sweep next tick
    private static final List<Light> sweepFound = new ArrayList<>();
    private static Vec3 sweepCam;
    private static int sweepCamSecX, sweepCamSecY, sweepCamSecZ, sweepMinSection;

    // Emission (0..15) of a held emissive block; drives the handheld lightmap boost.
    private static float heldLightLevel = 0.0f;

    // vec4[MAX_LIGHTS]: xyz = pos relative to captured fog camera, w = radius
    private static final MappedBuffer posRadiusBuffer = new MappedBuffer(MAX_LIGHTS * 16);
    // vec4[MAX_LIGHTS]: rgb = colour, a = intensity (emission / 15)
    private static final MappedBuffer colorBuffer = new MappedBuffer(MAX_LIGHTS * 16);

    static {
        for (int i = 0; i < MAX_LIGHTS * 16; i += 4) {
            posRadiusBuffer.putFloat(i, 0.0f);
            colorBuffer.putFloat(i, 0.0f);
        }
    }

    public static void tick() {
        updateHeldLightLevel();
        scanSlice();
    }

    private static void updateHeldLightLevel() {
        Minecraft mc = Minecraft.getInstance();
        heldLightLevel = mc.player == null ? 0.0f
                : Math.max(handEmission(mc.player.getMainHandItem()), handEmission(mc.player.getOffhandItem()));
    }

    private static int handEmission(ItemStack stack) {
        return stack.getItem() instanceof BlockItem blockItem
                ? blockItem.getBlock().defaultBlockState().getLightEmission()
                : 0;
    }

    public static float getHeldLightLevel() {
        if (!net.vulkanmod.Initializer.CONFIG.pointLightsEnabled || !net.vulkanmod.Initializer.CONFIG.shadersEnabled)
            return 0.0f;
        return heldLightLevel;
    }

    private static void scanSlice() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            lightCount = 0;
            sweepIndex = TOTAL_COLUMNS;   // abort any in-flight sweep
            sweepFound.clear();
            fillColorBuffer();
            return;
        }

        // Anchor camera/sections at sweep start
        if (sweepIndex >= TOTAL_COLUMNS) {
            sweepCam = mc.gameRenderer.getMainCamera().getPosition();
            sweepCamSecX = SectionPos.blockToSectionCoord(Mth.floor(sweepCam.x));
            sweepCamSecY = SectionPos.blockToSectionCoord(Mth.floor(sweepCam.y));
            sweepCamSecZ = SectionPos.blockToSectionCoord(Mth.floor(sweepCam.z));
            sweepMinSection = level.getMinSection();
            sweepFound.clear();
            sweepIndex = 0;
        }

        int end = Math.min(sweepIndex + COLUMNS_PER_TICK, TOTAL_COLUMNS);
        for (; sweepIndex < end; sweepIndex++) {
            int secX = sweepCamSecX + (sweepIndex % SPAN) - SCAN_RADIUS;
            int secZ = sweepCamSecZ + (sweepIndex / SPAN) - SCAN_RADIUS;
            if (!level.hasChunk(secX, secZ))
                continue;

            LevelChunkSection[] sections = level.getChunk(secX, secZ).getSections();

            for (int secY = sweepCamSecY - SCAN_RADIUS; secY <= sweepCamSecY + SCAN_RADIUS; secY++) {
                int idx = secY - sweepMinSection;
                if (idx < 0 || idx >= sections.length)
                    continue;

                LevelChunkSection section = sections[idx];
                // Fast-skip sections with no emissive block
                if (section == null || section.hasOnlyAir()
                        || !section.maybeHas(state -> state.getLightEmission() > 0))
                    continue;

                int baseX = SectionPos.sectionToBlockCoord(secX);
                int baseY = SectionPos.sectionToBlockCoord(secY);
                int baseZ = SectionPos.sectionToBlockCoord(secZ);

                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        for (int x = 0; x < 16; x++) {
                            BlockState state = section.getBlockState(x, y, z);
                            int emission = state.getLightEmission();
                            if (emission <= 0)
                                continue;

                            sweepFound.add(createLight(baseX + x + 0.5, baseY + y + 0.5, baseZ + z + 0.5,
                                    emission, state.getBlock(), sweepCam));
                        }
                    }
                }
            }
        }

        if (sweepIndex >= TOTAL_COLUMNS) {
            publishSweep(mc);
        }
    }

    private static void publishSweep(Minecraft mc) {
        // Handheld lights use current hand state (camera-pinned in getPosRadiusBuffer)
        if (mc.player != null) {
            addHandLight(sweepFound, mc.player.getMainHandItem(), sweepCam);
            addHandLight(sweepFound, mc.player.getOffhandItem(), sweepCam);
        }

        sweepFound.sort(Comparator.comparingDouble(light -> light.distSq));
        lightCount = Math.min(sweepFound.size(), MAX_LIGHTS);
        for (int i = 0; i < lightCount; i++) {
            lights[i] = sweepFound.get(i);
        }

        fillColorBuffer();
    }

    private static void addHandLight(List<Light> found, ItemStack stack, Vec3 cam) {
        if (!(stack.getItem() instanceof BlockItem blockItem))
            return;

        int emission = blockItem.getBlock().defaultBlockState().getLightEmission();
        if (emission <= 0)
            return;

        Light light = createLight(cam.x, cam.y, cam.z, emission, blockItem.getBlock(), cam);
        light.handheld = true;
        light.emission = emission * 1.6f;
        found.add(light);
    }

    private static Light createLight(double x, double y, double z, int emission, Block block, Vec3 cam) {
        Light light = new Light();
        light.x = x;
        light.y = y;
        light.z = z;
        double dx = x - cam.x, dy = y - cam.y, dz = z - cam.z;
        light.distSq = dx * dx + dy * dy + dz * dz;
        light.emission = emission;
        setColor(light, block);
        return light;
    }

    // Light colour by block family
    private static void setColor(Light light, Block block) {
        if (block == Blocks.TORCH || block == Blocks.WALL_TORCH || block == Blocks.LANTERN
                || block == Blocks.CAMPFIRE || block == Blocks.FIRE) {
            light.set(1.0f, 0.54f, 0.21f);
        } else if (block == Blocks.LAVA || block == Blocks.LAVA_CAULDRON || block == Blocks.MAGMA_BLOCK) {
            light.set(1.0f, 0.35f, 0.12f);
        } else if (block == Blocks.SOUL_TORCH || block == Blocks.SOUL_WALL_TORCH || block == Blocks.SOUL_LANTERN
                || block == Blocks.SOUL_CAMPFIRE || block == Blocks.SOUL_FIRE) {
            light.set(0.35f, 0.75f, 1.0f);
        } else if (block == Blocks.GLOWSTONE || block == Blocks.SHROOMLIGHT) {
            light.set(1.0f, 0.70f, 0.40f);
        } else if (block == Blocks.SEA_LANTERN || block == Blocks.BEACON || block == Blocks.END_ROD
                || block == Blocks.OCHRE_FROGLIGHT || block == Blocks.VERDANT_FROGLIGHT
                || block == Blocks.PEARLESCENT_FROGLIGHT) {
            light.set(0.75f, 0.90f, 1.0f);
        } else if (block == Blocks.NETHER_PORTAL || block == Blocks.CRYING_OBSIDIAN) {
            light.set(0.65f, 0.30f, 1.0f);
        } else if (block == Blocks.REDSTONE_TORCH || block == Blocks.REDSTONE_WALL_TORCH) {
            light.set(1.0f, 0.30f, 0.20f);
        } else {
            light.set(1.0f, 0.70f, 0.45f);
        }
    }

    private static void fillColorBuffer() {
        for (int i = 0; i < MAX_LIGHTS; i++) {
            int base = i * 16;
            if (i < lightCount) {
                Light light = lights[i];
                colorBuffer.putFloat(base, light.r);
                colorBuffer.putFloat(base + 4, light.g);
                colorBuffer.putFloat(base + 8, light.b);
                colorBuffer.putFloat(base + 12, light.emission / 15.0f);
            } else {
                colorBuffer.putFloat(base, 0.0f);
                colorBuffer.putFloat(base + 4, 0.0f);
                colorBuffer.putFloat(base + 8, 0.0f);
                colorBuffer.putFloat(base + 12, 0.0f);
            }
        }
    }

    // Rebuilt every call so lights track the shader's reconstruction camera.
    public static MappedBuffer getPosRadiusBuffer() {
        MappedBuffer cam = VRenderSystem.capturedCameraPos;
        double camX = cam.getFloat(0);
        double camY = cam.getFloat(4);
        double camZ = cam.getFloat(8);

        for (int i = 0; i < MAX_LIGHTS; i++) {
            int base = i * 16;
            if (i < lightCount) {
                Light light = lights[i];
                if (light.handheld) {
                    // Held light rides the camera
                    posRadiusBuffer.putFloat(base, 0.0f);
                    posRadiusBuffer.putFloat(base + 4, -0.3f);
                    posRadiusBuffer.putFloat(base + 8, 0.0f);
                } else {
                    posRadiusBuffer.putFloat(base, (float) (light.x - camX));
                    posRadiusBuffer.putFloat(base + 4, (float) (light.y - camY));
                    posRadiusBuffer.putFloat(base + 8, (float) (light.z - camZ));
                }
                posRadiusBuffer.putFloat(base + 12, light.emission + 1.0f);
            } else {
                posRadiusBuffer.putFloat(base, 0.0f);
                posRadiusBuffer.putFloat(base + 4, 0.0f);
                posRadiusBuffer.putFloat(base + 8, 0.0f);
                posRadiusBuffer.putFloat(base + 12, 0.0f);
            }
        }
        return posRadiusBuffer;
    }

    public static MappedBuffer getColorBuffer() {
        return colorBuffer;
    }

    public static float getCount() {
        return lightCount;
    }

    private static class Light {
        double x, y, z;
        double distSq;
        float r, g, b;
        float emission;
        boolean handheld;   // pinned to the camera every frame

        void set(float r, float g, float b) {
            this.r = r;
            this.g = g;
            this.b = b;
        }
    }
}
