package net.vulkanmod.config.ui.settings;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.vulkanmod.Initializer;
import net.vulkanmod.config.ui.core.BoundVerdict;
import net.vulkanmod.config.ui.core.FrameSamples;
import net.vulkanmod.config.ui.core.PresetSuggestion;
import net.vulkanmod.config.ui.core.ProfileResults;
import net.vulkanmod.render.chunk.WorldRenderer;
import net.vulkanmod.vulkan.FrameTimer;
import net.vulkanmod.vulkan.SessionSamples;

import java.util.List;

public final class OverviewSignals {
    private static net.vulkanmod.config.ui.core.BoundVerdict held;
    private static net.vulkanmod.config.ui.core.BoundVerdict pending;
    private static int agreed;

    public static net.vulkanmod.config.ui.core.BoundVerdict stickyVerdict() {
        net.vulkanmod.config.ui.core.BoundVerdict now = verdict();
        if (held == null) {
            held = now;
            return held;
        }
        if (now == held) {
            pending = null;
            agreed = 0;
            return held;
        }
        if (now != pending) {
            pending = now;
            agreed = 1;
            return held;
        }
        if (++agreed >= 20) {
            held = pending;
            pending = null;
            agreed = 0;
        }
        return held;
    }

    private static final String PRESET = "vulkanmod.options.performancePreset.";
    private static final int UNLIMITED_FPS = 260;

    private OverviewSignals() {
    }

    public static BoundVerdict verdict() {
        FrameSamples samples = SessionSamples.samples();
        if (!samples.ready()) {
            return BoundVerdict.UNKNOWN;
        }
        return BoundVerdict.of(new BoundVerdict.Signals(samples.median(), Math.max(0.0, FrameTimer.cpuBusyMs()),
                FrameTimer.gpuMs(), capPeriodMs(), meshingSettles(), serverTickMs()));
    }

    public static String suggestedPresetKey(String playingKey) {
        ensureLoaded();
        FrameSamples samples = SessionSamples.samples();
        if (SessionSamples.describesCurrent() && samples.ready()) {
            return PresetSuggestion.suggest(new PresetSuggestion.Reading(samples.count(),
                    samples.median(), samples.hasLowPercentile() ? samples.p1() : 0.0f,
                    targetFps(), playingKey));
        }
        return RESULTS.of(playingKey)
                .map(stored -> PresetSuggestion.suggest(new PresetSuggestion.Reading(stored.frames(),
                        stored.medianMs(), stored.lowMs(), targetFps(), playingKey)))
                .orElse(null);
    }

    private static double targetFps() {
        Minecraft minecraft = Minecraft.getInstance();
        int limit = minecraft.options.framerateLimit().get();
        if (limit > 0 && limit < UNLIMITED_FPS) {
            return limit;
        }
        return minecraft.getWindow().getRefreshRate() > 0 ? minecraft.getWindow().getRefreshRate() : 0.0;
    }

    private static final ProfileResults RESULTS = new ProfileResults();
    private static final List<String> PROFILE_KEYS = List.of(
            PRESET + "performance", PRESET + "balanced", PRESET + "quality", PRESET + "ultra");
    private static int loadedContext = Integer.MIN_VALUE;

    private static void ensureLoaded() {
        int context = SessionSamples.contextFingerprint();
        if (loadedContext == context) {
            return;
        }
        RESULTS.clear();
        loadedContext = context;
        BenchmarkStore.load(BenchmarkStore.PATH, context, RESULTS, Initializer.LOGGER::info);
    }

    public static void harvest(String playingKey) {
        ensureLoaded();
        FrameSamples samples = SessionSamples.samples();
        if (playingKey == null || !samples.ready() || !SessionSamples.describesCurrent()) {
            return;
        }
        ProfileResults.Result fresh = new ProfileResults.Result(Math.round(samples.fps()),
                samples.median(), samples.hasLowPercentile() ? samples.p1() : samples.p95(),
                samples.count());
        int before = RESULTS.of(playingKey).map(ProfileResults.Result::frames).orElse(0);
        RESULTS.record(playingKey, fresh);
        if (RESULTS.of(playingKey).map(ProfileResults.Result::frames).orElse(0) != before) {
            BenchmarkStore.save(BenchmarkStore.PATH, loadedContext, RESULTS, PROFILE_KEYS, Initializer.LOGGER::warn);
        }
    }

    public static String fpsOf(String profileKey) {
        ensureLoaded();
        return RESULTS.of(profileKey)
                .map(result -> I18n.get("vulkanmod.overview.you_get", result.fps()))
                .orElse(null);
    }

    private static double capPeriodMs() {
        Minecraft minecraft = Minecraft.getInstance();
        int limit = minecraft.options.framerateLimit().get();
        return limit <= 0 || limit >= UNLIMITED_FPS ? 0.0 : 1000.0 / limit;
    }

    private static boolean meshingSettles() {
        WorldRenderer renderer = WorldRenderer.getInstance();
        return renderer == null || renderer.getTaskDispatcher() == null
                || renderer.getTaskDispatcher().isIdle();
    }

    private static double serverTickMs() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getSingleplayerServer() == null) {
            return 0.0;
        }
        return minecraft.getSingleplayerServer().getAverageTickTimeNanos() / 1.0e6;
    }

}
