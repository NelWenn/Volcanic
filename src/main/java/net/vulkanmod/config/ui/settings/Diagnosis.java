package net.vulkanmod.config.ui.settings;

import net.vulkanmod.config.ui.core.FrameHistory;
import net.vulkanmod.config.ui.core.FrameSamples;
import net.vulkanmod.render.profiling.RenderCounters;
import net.vulkanmod.render.profiling.Telemetry;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class Diagnosis {
    public record Finding(String titleKey, String detail, boolean severe) {
    }

    private Diagnosis() {
    }

    public static List<Finding> of(FrameHistory history, FrameSamples.Summary play,
                                   List<FrameHistory.Bucket> stutters,
                                   List<StatsReport.Cell> terrain, List<StatsReport.Cell> memory,
                                   List<StatsReport.Cell> machine) {
        List<Finding> found = new ArrayList<>();
        addGcFinding(found, stutters);
        addUploadFinding(found, stutters, terrain);
        addAllocationFinding(found, history);
        addBatchingFinding(found);
        addSwapFinding(found, machine);
        addHeapFinding(found, memory);
        addSteadyFinding(found, play, stutters);
        return List.copyOf(found);
    }

    private static void addGcFinding(List<Finding> found, List<FrameHistory.Bucket> stutters) {
        if (stutters.isEmpty()) {
            return;
        }
        FrameHistory.Bucket worst = stutters.get(0);
        if (worst.gcMs() < 1.0f) {
            return;
        }
        int share = Math.round(Math.min(100.0f, worst.gcMs() / worst.max() * 100.0f));
        found.add(new Finding("vulkanmod.ui.stats.finding.gc",
                String.format(Locale.ROOT,
                        "%d ms of the %.0f ms stutter was a collection, %d%% of it. "
                                + "Cut allocation on the render path, not draw calls.",
                        Math.round(worst.gcMs()), worst.max(), share),
                share >= 50));
    }

    private static void addUploadFinding(List<Finding> found, List<FrameHistory.Bucket> stutters,
                                         List<StatsReport.Cell> terrain) {
        boolean saturated = terrain.stream().anyMatch(StatsReport.Cell::alert);
        int uploads = stutters.stream().mapToInt(FrameHistory.Bucket::uploads).max().orElse(0);
        if (!saturated && uploads < 12) {
            return;
        }
        found.add(new Finding("vulkanmod.ui.stats.finding.upload",
                saturated
                        ? "Builder threads park at 128 pending uploads, so meshing stalls until the "
                                + "renderer drains the queue. This stutter is the queue, not your shader."
                        : uploads + " sections were uploaded inside one stutter. Lower the per-frame "
                                + "upload budget to spread the cost.",
                saturated));
    }

    private static void addAllocationFinding(List<Finding> found, FrameHistory history) {
        float rate = history.allocationRateMbPerSecond();
        if (rate < 150.0f) {
            return;
        }
        found.add(new Finding("vulkanmod.ui.stats.finding.allocation",
                String.format(Locale.ROOT,
                        "The heap is growing by %.0f MB every second. That rate is what schedules "
                                + "the collections above; the profiler rows name the callers.", rate),
                rate >= 400.0f));
    }

    private static void addBatchingFinding(List<Finding> found) {
        int draws = RenderCounters.terrainDraws();
        int binds = RenderCounters.boundPipelines();
        if (draws < 400 || binds < 32) {
            return;
        }
        found.add(new Finding("vulkanmod.ui.stats.finding.batching",
                draws + " terrain draws and " + binds + " pipeline binds in one frame. "
                        + "Every bind costs on MoltenVK: group passes that share a pipeline.",
                binds >= 96));
    }

    private static void addSwapFinding(List<Finding> found, List<StatsReport.Cell> machine) {
        boolean swapping = machine.stream()
                .anyMatch(cell -> cell.alert() && cell.note() != null && cell.note().startsWith("swap"));
        if (!swapping) {
            return;
        }
        found.add(new Finding("vulkanmod.ui.stats.finding.swap",
                "The system is swapping. Nothing you change in the renderer will help until memory "
                        + "pressure drops; close something or lower render distance.", true));
    }

    private static void addHeapFinding(List<Finding> found, List<StatsReport.Cell> memory) {
        boolean tight = memory.stream()
                .anyMatch(cell -> cell.alert() && "vulkanmod.ui.developer.info.heap".equals(cell.labelKey()));
        if (!tight) {
            return;
        }
        found.add(new Finding("vulkanmod.ui.stats.finding.heap",
                "The heap is close to its limit, so collections get more frequent and longer. "
                        + "Raise -Xmx before chasing anything else.", true));
    }

    private static void addSteadyFinding(List<Finding> found, FrameSamples.Summary play,
                                         List<FrameHistory.Bucket> stutters) {
        if (!found.isEmpty() || play.count() < FrameSamples.READY_AT) {
            return;
        }
        found.add(new Finding("vulkanmod.ui.stats.finding.steady",
                stutters.isEmpty()
                        ? "No stutter crossed the threshold in this window. The frame time is steady."
                        : "The stutters here are small and have no single cause; they read as normal "
                                + "variation rather than a defect.",
                false));
    }

    public static boolean thermalDrift() {
        Telemetry.Snapshot snapshot = Telemetry.latest();
        return snapshot.dieTemp() > 95.0;
    }
}
