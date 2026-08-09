package net.vulkanmod.config.ui.core;

import java.util.List;

public final class Recommendation {
    public static final double STUTTER_RATIO = 2.0;
    public static final double ROOMY = 0.40;
    public static final double CAP_ROOMY = 0.30;

    public record Advice(String messageKey, String routeKey, String modHintKey) {
        public Advice {
            if (messageKey == null || messageKey.isBlank()) {
                throw new IllegalArgumentException("messageKey must not be blank");
            }
        }
    }

    public record Signals(int sampleCount, BoundVerdict verdict, double headroom, double stutter,
                          boolean shaderPack, boolean farTerrainMod, boolean tickHeavyMod) {
        public Signals {
            if (sampleCount < 0) {
                throw new IllegalArgumentException("sampleCount must not be negative: " + sampleCount);
            }
            if (verdict == null) {
                throw new IllegalArgumentException("verdict must not be null");
            }
        }
    }

    private Recommendation() {
    }

    public static Advice of(Signals signals) {
        if (signals == null) {
            throw new IllegalArgumentException("signals must not be null");
        }
        if (signals.sampleCount() < FrameSamples.READY_AT) {
            return new Advice("vulkanmod.overview.advice.sampling", null, null);
        }
        return switch (signals.verdict()) {
            case CAPPED -> signals.headroom() > CAP_ROOMY
                    ? new Advice("vulkanmod.overview.advice.capped_roomy", null, null)
                    : new Advice("vulkanmod.overview.advice.capped", null, null);
            case SERVER_TICK -> new Advice("vulkanmod.overview.advice.server_tick", null,
                    signals.tickHeavyMod() ? "vulkanmod.overview.hint.tick_heavy" : null);
            case MESHING -> new Advice("vulkanmod.overview.advice.meshing", "rendering.general",
                    signals.farTerrainMod() ? "vulkanmod.overview.hint.far_terrain" : null);
            case GPU -> new Advice("vulkanmod.overview.advice.gpu", "rendering.resolution",
                    signals.shaderPack() ? "vulkanmod.overview.hint.shader_pack" : null);
            case RENDER_CPU -> new Advice("vulkanmod.overview.advice.render_cpu", "rendering.culling",
                    signals.farTerrainMod() ? "vulkanmod.overview.hint.far_terrain" : null);
            case IDLE -> new Advice("vulkanmod.overview.advice.idle", null, null);
            case UNKNOWN -> unremarkable(signals);
        };
    }

    private static Advice unremarkable(Signals signals) {
        if (signals.stutter() > STUTTER_RATIO) {
            return new Advice("vulkanmod.overview.advice.uneven", "performance.chunks", null);
        }
        if (signals.headroom() > ROOMY) {
            return new Advice("vulkanmod.overview.advice.headroom", null, null);
        }
        return new Advice("vulkanmod.overview.advice.balanced", null, null);
    }

    public static List<String> keys() {
        return List.of("vulkanmod.overview.advice.sampling",
                "vulkanmod.overview.advice.capped_roomy",
                "vulkanmod.overview.advice.capped",
                "vulkanmod.overview.advice.server_tick",
                "vulkanmod.overview.advice.meshing",
                "vulkanmod.overview.advice.gpu",
                "vulkanmod.overview.advice.render_cpu",
                "vulkanmod.overview.advice.idle",
                "vulkanmod.overview.advice.uneven",
                "vulkanmod.overview.advice.headroom",
                "vulkanmod.overview.advice.balanced",
                "vulkanmod.overview.hint.shader_pack",
                "vulkanmod.overview.hint.far_terrain",
                "vulkanmod.overview.hint.tick_heavy");
    }
}
