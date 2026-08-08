package net.vulkanmod.config.ui.core;

public record SettingId(String namespace, String path) {
    private static final char SEPARATOR = ':';

    public SettingId {
        if (namespace == null || namespace.isBlank()) {
            throw new IllegalArgumentException("namespace must not be blank");
        }
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("path must not be blank");
        }
        if (namespace.indexOf(SEPARATOR) >= 0) {
            throw new IllegalArgumentException("namespace must not contain '" + SEPARATOR + "': " + namespace);
        }
    }

    public static SettingId of(String namespace, String path) {
        return new SettingId(namespace, path);
    }

    public static SettingId parse(String value) {
        if (value == null) {
            throw new IllegalArgumentException("value must not be null");
        }
        int split = value.indexOf(SEPARATOR);
        if (split < 0) {
            throw new IllegalArgumentException("missing '" + SEPARATOR + "' in setting id: " + value);
        }
        return new SettingId(value.substring(0, split), value.substring(split + 1));
    }

    @Override
    public String toString() {
        return namespace + SEPARATOR + path;
    }
}
