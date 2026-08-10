package net.vulkanmod.config.ui.settings;

import net.minecraft.client.resources.language.I18n;
import net.vulkanmod.config.ui.core.FrameHistory;
import net.vulkanmod.config.ui.core.FrameSamples;
import net.vulkanmod.render.profiling.StackSampler;
import net.vulkanmod.vulkan.FrameTimer;

import java.util.List;
import java.util.Locale;

public final class DiagnosticsReport {
    private static final String RULE = "----------------------------------------";

    public record Input(List<StatsReport.Cell> fingerprint, List<StatsReport.Cell> scene,
                        List<StatsReport.Cell> terrain, List<StatsReport.Cell> memory,
                        List<StatsReport.Cell> machine, List<SystemReport.Row> system,
                        List<Diagnosis.Finding> findings, List<FrameHistory.Bucket> stutters,
                        List<StackSampler.Frame> allocators, FrameSamples.Summary play,
                        FrameHistory history, long captureEndMs, String verdictKey) {
    }

    private DiagnosticsReport() {
    }

    public static String build(Input in) {
        StringBuilder out = new StringBuilder(4096);
        out.append("Volcanic diagnostics report\n");
        int buckets = in.history() == null ? 0 : in.history().size();
        out.append(String.format(Locale.ROOT, "Window: %.1f s of played frames (%d buckets of %d ms)%n",
                buckets * FrameHistory.BUCKET_MS / 1000.0f, buckets, FrameHistory.BUCKET_MS));

        section(out, "SYSTEM");
        for (SystemReport.Row row : in.system()) {
            if (row.section()) {
                out.append('\n').append(label(row.labelKey())).append('\n');
            } else {
                line(out, label(row.labelKey()), row.value(), null);
            }
        }

        section(out, "MEASUREMENT FINGERPRINT");
        cells(out, in.fingerprint());

        section(out, "FRAME TIME");
        FrameSamples.Summary play = in.play();
        if (play.count() == 0) {
            out.append("no gameplay frames recorded\n");
        } else {
            line(out, "Frames sampled", Integer.toString(play.count()), null);
            line(out, "Average", fps(play.average()), millis(play.average()));
            line(out, "Median", fps(play.median()), millis(play.median()));
            line(out, "1% low", play.low1() < 0 ? "-" : fps(play.low1()), millis(play.low1()));
            line(out, "0.1% low", play.low01() < 0 ? "-" : fps(play.low01()), millis(play.low01()));
            line(out, "P95", millis(play.p95()), null);
            line(out, "Worst frame", millis(play.max()), null);
            line(out, "Stutters in window", Integer.toString(play.spikes()), null);
        }
        line(out, "Verdict", I18n.get(in.verdictKey()), null);
        line(out, "Frame (smoothed)", millis(FrameTimer.frameMs()), null);
        line(out, "GPU", millis(FrameTimer.gpuMs()), "runs alongside the CPU, not after it");
        line(out, "CPU render thread", millis(FrameTimer.cpuBusyMs()),
                "upload " + millis(FrameTimer.uploadMs()) + " · setup " + millis(FrameTimer.setupMs())
                        + " · terrain " + millis(FrameTimer.terrainMs()));
        if (in.history() != null) {
            line(out, "Allocation rate",
                    in.history().allocationRateMbPerSecond() < 0.0f ? "-"
                            : String.format(Locale.ROOT, "%.0f MB/s",
                                    in.history().allocationRateMbPerSecond()), null);
        }

        section(out, "WHAT LOOKS WRONG");
        if (in.findings().isEmpty()) {
            out.append("nothing flagged\n");
        }
        for (Diagnosis.Finding finding : in.findings()) {
            out.append(finding.severe() ? "[!] " : "[i] ")
                    .append(I18n.get(finding.titleKey())).append('\n')
                    .append("    ").append(finding.detail()).append('\n');
        }

        section(out, "STUTTERS");
        if (in.stutters().isEmpty()) {
            out.append("none above the threshold\n");
        }
        for (FrameHistory.Bucket bucket : in.stutters()) {
            out.append(String.format(Locale.ROOT, "-%.1f s  worst %.1f ms  gc %d ms  uploads %d  builds %d%n",
                    Math.max(0.0f, (in.captureEndMs() - bucket.startMs()) / 1000.0f),
                    bucket.max(), Math.round(bucket.gcMs()), bucket.uploads(), bucket.builds()));
            long span = Math.max(FrameHistory.BUCKET_MS, Math.round(bucket.max()));
            List<StackSampler.Frame> profile = StackSampler.profile(bucket.startMs() - span,
                    bucket.startMs() + span);
            if (profile.isEmpty()) {
                out.append("    no render-thread samples in that window\n");
            }
            for (StackSampler.Frame frame : profile) {
                out.append(String.format(Locale.ROOT, "    %3d%%  %s  (%d samples)%n",
                        Math.round(frame.share() * 100), frame.name(), frame.samples()));
            }
        }

        section(out, "WHAT THE FRAME CONTAINED");
        cells(out, in.scene());
        section(out, "TERRAIN PIPELINE");
        cells(out, in.terrain());
        section(out, "MEMORY");
        cells(out, in.memory());
        if (!in.allocators().isEmpty()) {
            out.append("hot methods while the heap grew:\n");
            for (StackSampler.Frame frame : in.allocators()) {
                out.append(String.format(Locale.ROOT, "    %3d%%  %s  (%d samples)%n",
                        Math.round(frame.share() * 100), frame.name(), frame.samples()));
            }
        }
        section(out, "MACHINE CONTEXT");
        cells(out, in.machine());
        return out.toString();
    }

    private static void cells(StringBuilder out, List<StatsReport.Cell> cells) {
        if (cells.isEmpty()) {
            out.append("unavailable\n");
            return;
        }
        for (StatsReport.Cell cell : cells) {
            line(out, label(cell.labelKey()), cell.value(),
                    cell.alert() ? "ALERT" + (cell.note() == null ? "" : " · " + cell.note())
                            : cell.note() == null || cell.note().equals(cell.value()) ? null : cell.note());
        }
    }

    private static void section(StringBuilder out, String title) {
        out.append('\n').append(RULE).append('\n').append(title).append('\n').append(RULE).append('\n');
    }

    private static void line(StringBuilder out, String label, String value, String note) {
        out.append(pad(label));
        if (value != null) {
            out.append(value);
        }
        if (note != null && !note.isBlank()) {
            out.append(value == null ? "" : "   ").append(note);
        }
        out.append('\n');
    }

    private static String pad(String label) {
        StringBuilder padded = new StringBuilder(label).append(':');
        while (padded.length() < 26) {
            padded.append(' ');
        }
        return padded.toString();
    }

    private static String label(String key) {
        String text = I18n.get(key);
        return text.equals(key) ? key.substring(key.lastIndexOf('.') + 1) : text;
    }

    private static String millis(double value) {
        return value < 0.0 ? "-" : String.format(Locale.ROOT, "%.2f ms", value);
    }

    private static String fps(float ms) {
        return ms <= 0.0f ? "-" : String.format(Locale.ROOT, "%.0f fps", 1000.0f / ms);
    }
}
