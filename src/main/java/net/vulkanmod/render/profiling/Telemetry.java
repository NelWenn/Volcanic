package net.vulkanmod.render.profiling;

import com.sun.management.OperatingSystemMXBean;

import java.lang.management.ManagementFactory;

public final class Telemetry {
    public record Snapshot(double systemLoad, double processCores, double dieTemp, double gpuTemp,
                           int fanRpm, long freeMemoryBytes, long totalMemoryBytes, long swapBytes,
                           int threads, int peakThreads, long uptimeMs, boolean unifiedDie) {
    }

    private static final Snapshot EMPTY = new Snapshot(-1.0, -1.0, -1.0, -1.0, -1,
            -1L, -1L, -1L, -1, -1, 0L, true);

    private static volatile Snapshot latest = EMPTY;
    private static volatile boolean running;
    private static volatile boolean unified = true;
    private static Object hardware;

    private Telemetry() {
    }

    public static Snapshot latest() {
        return latest;
    }

    public static void setUnifiedDie(boolean value) {
        unified = value;
    }

    public static synchronized void setRunning(boolean enabled) {
        if (enabled == running) {
            return;
        }
        running = enabled;
        if (!enabled) {
            return;
        }
        Thread thread = new Thread(Telemetry::loop, "Volcanic telemetry");
        thread.setDaemon(true);
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
    }

    private static void loop() {
        while (running) {
            try {
                latest = sample();
            } catch (Throwable failure) {
                latest = EMPTY;
                running = false;
                return;
            }
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private static Snapshot sample() {
        OperatingSystemMXBean os = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        double load = os.getCpuLoad();
        double cores = os.getProcessCpuLoad() * Runtime.getRuntime().availableProcessors();
        int threads = ManagementFactory.getThreadMXBean().getThreadCount();
        int peak = ManagementFactory.getThreadMXBean().getPeakThreadCount();
        long uptime = ManagementFactory.getRuntimeMXBean().getUptime();

        double die = -1.0;
        int fan = -1;
        long free = -1L;
        long total = -1L;
        long swap = -1L;
        try {
            oshi.hardware.HardwareAbstractionLayer layer = hardware();
            oshi.hardware.GlobalMemory memory = layer.getMemory();
            free = memory.getAvailable();
            total = memory.getTotal();
            swap = memory.getVirtualMemory().getSwapUsed();
            double reading = layer.getSensors().getCpuTemperature();
            if (reading > 1.0) {
                die = reading;
            }
            if (die < 0.0 && net.vulkanmod.config.Platform.isMacOS()) {
                double apple = AppleSensors.dieTemperature();
                if (apple > 1.0) {
                    die = apple;
                }
            }
            int[] speeds = layer.getSensors().getFanSpeeds();
            for (int speed : speeds) {
                fan = Math.max(fan, speed);
            }
        } catch (Throwable unavailable) {
            hardware = null;
        }
        return new Snapshot(load, cores, die, unified ? -1.0 : die, fan,
                free, total, swap, threads, peak, uptime, unified);
    }

    private static oshi.hardware.HardwareAbstractionLayer hardware() {
        if (hardware == null) {
            hardware = new oshi.SystemInfo().getHardware();
        }
        return (oshi.hardware.HardwareAbstractionLayer) hardware;
    }
}
