package net.vulkanmod.config;

public final class ConfigVersion {
    public static final int CURRENT = 1;

    private ConfigVersion() {
    }

    public static int migrated(int storedVersion) {
        if (storedVersion < 0) {
            throw new IllegalStateException("config version must not be negative: " + storedVersion);
        }
        if (storedVersion > CURRENT) {
            throw new IllegalStateException("config version " + storedVersion
                    + " is newer than this build supports (" + CURRENT + ")");
        }
        return CURRENT;
    }
}
