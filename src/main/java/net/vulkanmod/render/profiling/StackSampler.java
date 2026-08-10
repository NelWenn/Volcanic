package net.vulkanmod.render.profiling;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class StackSampler {
    public static final int PERIOD_MS = 20;
    public static final int DEPTH = 24;
    private static final int CAPACITY = 6144;
    private static final int MIN_SAMPLES = 2;

    private static final long[] STAMPS = new long[CAPACITY];
    private static final String[][] FRAMES = new String[CAPACITY][];
    private static final Object LOCK = new Object();

    private static volatile boolean running;
    private static volatile long targetThread = -1L;
    private static volatile boolean gameplay;
    private static int next;
    private static int filled;
    private static Thread worker;

    public record Frame(String name, int samples, float share) {
    }

    private StackSampler() {
    }

    public static void watch(long threadId) {
        targetThread = threadId;
    }

    public static void setGameplay(boolean active) {
        gameplay = active;
    }

    public static boolean running() {
        return running;
    }

    public static synchronized void setRunning(boolean enabled) {
        if (enabled == running) {
            return;
        }
        running = enabled;
        if (!enabled) {
            worker = null;
            return;
        }
        Thread thread = new Thread(StackSampler::loop, "Volcanic stack sampler");
        thread.setDaemon(true);
        thread.setPriority(Thread.MIN_PRIORITY);
        worker = thread;
        thread.start();
    }

    public static void clear() {
        synchronized (LOCK) {
            next = 0;
            filled = 0;
        }
    }

    public static List<Frame> profile(long fromMs, long toMs) {
        List<String[]> window = new ArrayList<>();
        synchronized (LOCK) {
            for (int index = 0; index < filled; index++) {
                int at = Math.floorMod(next - 1 - index, CAPACITY);
                if (STAMPS[at] < fromMs) {
                    break;
                }
                if (STAMPS[at] <= toMs && FRAMES[at] != null) {
                    window.add(FRAMES[at]);
                }
            }
        }
        if (window.size() < MIN_SAMPLES) {
            return List.of();
        }


        Map<String, Integer> hits = new LinkedHashMap<>();
        for (String[] stack : window) {
            String running = self(stack);
            if (running != null) {
                hits.merge(running, 1, Integer::sum);
            }
        }
        List<Frame> out = new ArrayList<>(hits.size());
        for (Map.Entry<String, Integer> entry : hits.entrySet()) {
            out.add(new Frame(entry.getKey(), entry.getValue(),
                    entry.getValue() / (float) window.size()));
        }
        out.sort((left, right) -> Integer.compare(right.samples(), left.samples()));
        return List.copyOf(out.subList(0, Math.min(6, out.size())));
    }

    public static int recorded() {
        synchronized (LOCK) {
            return filled;
        }
    }

    public static int samplesIn(long fromMs, long toMs) {
        int found = 0;
        synchronized (LOCK) {
            for (int index = 0; index < filled; index++) {
                int at = Math.floorMod(next - 1 - index, CAPACITY);
                if (STAMPS[at] < fromMs) {
                    break;
                }
                if (STAMPS[at] <= toMs) {
                    found++;
                }
            }
        }
        return found;
    }

    private static String self(String[] stack) {
        for (String frame : stack) {
            if (frame != null && !runtime(frame)) {
                return frame;
            }
        }
        return stack.length == 0 ? null : stack[0];
    }

    private static boolean runtime(String frame) {
        return frame.startsWith("Unsafe.") || frame.startsWith("Thread.sleep")
                || frame.startsWith("LockSupport.") || frame.startsWith("AbstractQueuedSynchronizer.")
                || frame.startsWith("Native") || frame.startsWith("VarHandle");
    }

    private static void loop() {
        ThreadMXBean threads = ManagementFactory.getThreadMXBean();
        while (running) {
            try {
                Thread.sleep(PERIOD_MS);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return;
            }
            long id = targetThread;
            if (!gameplay || id < 0) {
                continue;
            }
            try {
                ThreadInfo info = threads.getThreadInfo(id, DEPTH);
                if (info == null) {
                    continue;
                }
                store(System.currentTimeMillis(), info.getStackTrace());
            } catch (Throwable unavailable) {
                running = false;
                return;
            }
        }
    }

    private static void store(long stampMs, StackTraceElement[] trace) {
        if (trace == null || trace.length == 0) {
            return;
        }
        String[] frames = new String[Math.min(trace.length, DEPTH)];
        for (int index = 0; index < frames.length; index++) {
            frames[index] = shortName(trace[index]);
        }
        synchronized (LOCK) {
            STAMPS[next] = stampMs;
            FRAMES[next] = frames;
            next = (next + 1) % CAPACITY;
            if (filled < CAPACITY) {
                filled++;
            }
        }
    }

    private static String shortName(StackTraceElement element) {
        String type = element.getClassName();
        int dot = type.lastIndexOf('.');
        return (dot < 0 ? type : type.substring(dot + 1)) + "." + element.getMethodName();
    }
}
