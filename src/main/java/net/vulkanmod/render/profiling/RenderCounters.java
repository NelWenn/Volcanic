package net.vulkanmod.render.profiling;

public final class RenderCounters {
    private static int terrainDraws;
    private static int boundPipelines;
    private static int lastTerrainDraws;
    private static int lastBoundPipelines;
    private static int uploadsThisFrame;
    private static int pipelineBuildsThisFrame;
    private static int meshQueue = -1;
    private static int uploadQueue = -1;
    private static int idleBuilders = -1;
    private static int builders = -1;
    private static int visibleSections = -1;

    private RenderCounters() {
    }

    public static void terrainDraw(int calls) {
        terrainDraws += calls;
    }

    public static void uploads(int count) {
        uploadsThisFrame = count;
    }

    public static void pipelineBuilt() {
        pipelineBuildsThisFrame++;
    }

    public static int pipelineBuilds() {
        int built = pipelineBuildsThisFrame;
        pipelineBuildsThisFrame = 0;
        return built;
    }

    public static void endFrame(int pipelines) {
        lastTerrainDraws = terrainDraws;
        lastBoundPipelines = pipelines;
        terrainDraws = 0;
        boundPipelines = pipelines;
    }

    public static int terrainDraws() {
        return lastTerrainDraws;
    }

    public static int boundPipelines() {
        return lastBoundPipelines;
    }

    public static int uploadsLastFrame() {
        return uploadsThisFrame;
    }

    public static void snapshotTerrain(int pendingTasks, int pendingUploads, int idle, int total,
                                       int visible) {
        meshQueue = pendingTasks;
        uploadQueue = pendingUploads;
        idleBuilders = idle;
        builders = total;
        visibleSections = visible;
    }

    public static int meshQueue() {
        return meshQueue;
    }

    public static int uploadQueue() {
        return uploadQueue;
    }

    public static int idleBuilders() {
        return idleBuilders;
    }

    public static int builders() {
        return builders;
    }

    public static int visibleSections() {
        return visibleSections;
    }
}
