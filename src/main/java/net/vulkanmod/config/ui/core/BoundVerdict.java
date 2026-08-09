package net.vulkanmod.config.ui.core;

public enum BoundVerdict {
    CAPPED,
    IDLE,
    GPU,
    RENDER_CPU,
    MESHING,
    SERVER_TICK,
    UNKNOWN;

    public static final double CAP_TOLERANCE = 0.10;
    public static final double GPU_SHARE = 0.60;
    public static final double CPU_SHARE = 0.70;
    public static final double IDLE_SHARE = 0.90;
    public static final double SERVER_TICK_MS = 50.0;

    public record Signals(double wallMs, double cpuBusyMs, double gpuMs, double capPeriodMs,
                          boolean meshingSettles, double serverTickMs) {
        public Signals {
            if (wallMs <= 0.0) {
                throw new IllegalArgumentException("wallMs must be positive: " + wallMs);
            }
        }
    }

    public static BoundVerdict of(Signals signals) {
        if (signals == null) {
            throw new IllegalArgumentException("signals must not be null");
        }
        double wall = signals.wallMs();

        if (signals.capPeriodMs() > 0.0
                && Math.abs(wall - signals.capPeriodMs()) <= signals.capPeriodMs() * CAP_TOLERANCE) {
            return CAPPED;
        }
        if (signals.serverTickMs() > SERVER_TICK_MS) {
            return SERVER_TICK;
        }
        if (!signals.meshingSettles()) {
            return MESHING;
        }
        if (signals.gpuMs() >= 0.0 && signals.gpuMs() >= signals.cpuBusyMs()
                && signals.gpuMs() >= GPU_SHARE * wall) {
            return GPU;
        }
        if (signals.cpuBusyMs() >= CPU_SHARE * wall) {
            return RENDER_CPU;
        }
        if (idleShare(signals) >= IDLE_SHARE) {
            return IDLE;
        }
        return UNKNOWN;
    }

    private static double idleShare(Signals signals) {
        double busy = Math.max(signals.cpuBusyMs(), Math.max(0.0, signals.gpuMs()));
        return (signals.wallMs() - busy) / signals.wallMs();
    }

    public String messageKey() {
        return "vulkanmod.overview.bound." + name().toLowerCase(java.util.Locale.ROOT);
    }
}
