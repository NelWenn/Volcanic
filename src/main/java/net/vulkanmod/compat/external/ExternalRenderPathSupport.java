package net.vulkanmod.compat.external;

import net.vulkanmod.compat.capabilities.ExternalRenderPathOptions;

import java.util.Locale;

public final class ExternalRenderPathSupport {
    private static final Mode MODE = Mode.fromProperty(ExternalRenderPathOptions.externalLodMode());

    private ExternalRenderPathSupport() {
    }

    public static Mode mode() {
        return MODE;
    }

    public static boolean isExternalLodBridgeEnabled() {
        return MODE == Mode.EXPERIMENTAL;
    }

    public static boolean shouldDrawExternalLodBridge() {
        return shouldDrawExternalLodBridge(MODE, ExternalRenderPathOptions.externalLodDrawEnabled());
    }

    static boolean shouldDrawExternalLodBridge(Mode mode, boolean draw) {
        return mode == Mode.EXPERIMENTAL && draw;
    }

    public static boolean shouldDrawExternalLodBridgeDirectlyToMainFramebuffer() {
        return shouldDrawExternalLodBridgeDirectlyToMainFramebuffer(MODE,
                ExternalRenderPathOptions.externalLodDrawEnabled());
    }

    static boolean shouldDrawExternalLodBridgeDirectlyToMainFramebuffer(Mode mode, boolean draw) {
        return shouldDrawExternalLodBridge(mode, draw);
    }

    public static boolean shouldSkipExternalLodApplyPass() {
        return shouldSkipExternalLodApplyPass(MODE);
    }

    static boolean shouldSkipExternalLodApplyPass(Mode mode) {
        return mode == Mode.SAFE || mode == Mode.EXPERIMENTAL;
    }

    public static boolean shouldBypassExternalRenderer() {
        return shouldBypassExternalRenderer(MODE);
    }

    static boolean shouldBypassExternalRenderer(Mode mode) {
        return mode == Mode.SAFE;
    }

    public static boolean shouldApplyMixin() {
        return shouldApplyMixin(MODE);
    }

    static boolean shouldApplyMixin(Mode mode) {
        return mode != Mode.OFF;
    }

    public static boolean shouldCreateLodPipeline() {
        return MODE == Mode.EXPERIMENTAL;
    }

    public static boolean shouldCreateExternalLodPipeline() {
        return shouldCreateLodPipeline();
    }

    public enum Mode {
        SAFE,
        EXPERIMENTAL,
        OFF;

        public static Mode fromProperty(String value) {
            if (value == null || value.isBlank()) {
                return OFF;
            }

            return switch (value.trim().toLowerCase(Locale.ROOT)) {
                case "experimental", "bridge", "render", "on", "true" -> EXPERIMENTAL;
                case "off", "disabled", "disable", "false", "none" -> OFF;
                case "safe", "bypass", "compat", "compatibility" -> SAFE;
                default -> OFF;
            };
        }
    }
}
