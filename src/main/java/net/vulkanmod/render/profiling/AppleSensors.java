package net.vulkanmod.render.profiling;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

import java.util.ArrayList;
import java.util.List;

public final class AppleSensors {
    private static final long PAGE = 0xff00L;
    private static final long USAGE = 5L;
    private static final int EVENT_TEMPERATURE = 15;
    private static final int FIELD_TEMPERATURE = 15 << 16;

    private static boolean tried;
    private static boolean usable;
    private static Pointer client;
    private static final List<Pointer> SERVICES = new ArrayList<>();

    private interface CoreFoundation extends Library {
        CoreFoundation INSTANCE = Native.load("CoreFoundation", CoreFoundation.class);

        Pointer CFStringCreateWithCString(Pointer alloc, String value, int encoding);

        Pointer CFNumberCreate(Pointer alloc, int type, long[] value);

        Pointer CFDictionaryCreate(Pointer alloc, Pointer[] keys, Pointer[] values, long count,
                                   Pointer keyCallbacks, Pointer valueCallbacks);

        long CFArrayGetCount(Pointer array);

        Pointer CFArrayGetValueAtIndex(Pointer array, long index);

        void CFRelease(Pointer reference);
    }

    private interface IOKit extends Library {
        IOKit INSTANCE = Native.load("IOKit", IOKit.class);

        Pointer IOHIDEventSystemClientCreate(Pointer alloc);

        int IOHIDEventSystemClientSetMatching(Pointer client, Pointer matching);

        Pointer IOHIDEventSystemClientCopyServices(Pointer client);

        Pointer IOHIDServiceClientCopyEvent(Pointer service, long type, int options, long timestamp);

        double IOHIDEventGetFloatValue(Pointer event, int field);
    }

    private AppleSensors() {
    }

    public static synchronized double dieTemperature() {
        if (!prepare()) {
            return -1.0;
        }
        double hottest = -1.0;
        for (Pointer service : SERVICES) {
            try {
                Pointer event = IOKit.INSTANCE.IOHIDServiceClientCopyEvent(service,
                        EVENT_TEMPERATURE, 0, 0L);
                if (event == null) {
                    continue;
                }
                double value = IOKit.INSTANCE.IOHIDEventGetFloatValue(event, FIELD_TEMPERATURE);
                CoreFoundation.INSTANCE.CFRelease(event);
                if (value > 1.0 && value < 150.0) {
                    hottest = Math.max(hottest, value);
                }
            } catch (Throwable failure) {
                usable = false;
                SERVICES.clear();
                return -1.0;
            }
        }
        return hottest;
    }

    private static boolean prepare() {
        if (tried) {
            return usable;
        }
        tried = true;
        try {
            CoreFoundation cf = CoreFoundation.INSTANCE;
            Pointer pageKey = cf.CFStringCreateWithCString(null, "PrimaryUsagePage", 0x08000100);
            Pointer usageKey = cf.CFStringCreateWithCString(null, "PrimaryUsage", 0x08000100);
            Pointer pageValue = cf.CFNumberCreate(null, 4, new long[] {PAGE});
            Pointer usageValue = cf.CFNumberCreate(null, 4, new long[] {USAGE});
            if (pageKey == null || usageKey == null || pageValue == null || usageValue == null) {
                return false;
            }
            Pointer matching = cf.CFDictionaryCreate(null,
                    new Pointer[] {pageKey, usageKey}, new Pointer[] {pageValue, usageValue},
                    2L, null, null);
            if (matching == null) {
                return false;
            }

            client = IOKit.INSTANCE.IOHIDEventSystemClientCreate(null);
            if (client == null) {
                return false;
            }
            IOKit.INSTANCE.IOHIDEventSystemClientSetMatching(client, matching);
            Pointer services = IOKit.INSTANCE.IOHIDEventSystemClientCopyServices(client);
            if (services == null) {
                return false;
            }
            long count = Math.min(cf.CFArrayGetCount(services), 96L);
            for (long index = 0; index < count; index++) {
                Pointer service = cf.CFArrayGetValueAtIndex(services, index);
                if (service != null) {
                    SERVICES.add(service);
                }
            }
            usable = !SERVICES.isEmpty();
            return usable;
        } catch (Throwable unavailable) {
            usable = false;
            return false;
        }
    }
}
