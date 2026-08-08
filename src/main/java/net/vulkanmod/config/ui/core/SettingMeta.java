package net.vulkanmod.config.ui.core;

public record SettingMeta(SettingId id, RouteId route, String titleKey, String descriptionKey,
                          SettingType type, SettingSource source, ApplyScope scope,
                          boolean advanced, boolean experimental, boolean recommended) {

    public static final class Builder {
        private final SettingId id;
        private final RouteId route;
        private final String titleKey;
        private final SettingType type;
        private final SettingSource source;
        private String descriptionKey;
        private ApplyScope scope = ApplyScope.INSTANT;
        private boolean advanced;
        private boolean experimental;
        private boolean recommended;

        public Builder(SettingId id, RouteId route, String titleKey, SettingType type, SettingSource source) {
            if (id == null) {
                throw new IllegalArgumentException("id must not be null");
            }
            if (route == null) {
                throw new IllegalArgumentException("route must not be null");
            }
            if (titleKey == null || titleKey.isBlank()) {
                throw new IllegalArgumentException("titleKey must not be blank");
            }
            if (type == null) {
                throw new IllegalArgumentException("type must not be null");
            }
            if (source == null) {
                throw new IllegalArgumentException("source must not be null");
            }
            this.id = id;
            this.route = route;
            this.titleKey = titleKey;
            this.type = type;
            this.source = source;
        }

        public Builder descriptionKey(String key) {
            if (key == null || key.isBlank()) {
                throw new IllegalArgumentException("descriptionKey must not be blank; omit it instead");
            }
            this.descriptionKey = key;
            return this;
        }

        public Builder scope(ApplyScope scope) {
            if (scope == null) {
                throw new IllegalArgumentException("scope must not be null");
            }
            this.scope = scope;
            return this;
        }

        public Builder advanced(boolean value) {
            this.advanced = value;
            return this;
        }

        public Builder experimental(boolean value) {
            this.experimental = value;
            return this;
        }

        public Builder recommended(boolean value) {
            this.recommended = value;
            return this;
        }

        public SettingMeta build() {
            return new SettingMeta(id, route, titleKey, descriptionKey, type, source, scope,
                    advanced, experimental, recommended);
        }
    }
}
