package net.vulkanmod.vulkan;

import net.vulkanmod.Initializer;
import net.vulkanmod.config.Platform;
import org.lwjgl.system.Library;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.SharedLibrary;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.lwjgl.system.JNI.invokePPI;
import static org.lwjgl.system.MemoryStack.stackPush;

// macOS-only MoltenVK tuning. Sets MVK_CONFIG_* env vars via libc setenv before the first Vulkan call
// (System.setProperty doesn't reach the C getenv MoltenVK reads). User-set env vars are never overwritten.
public final class MoltenVKConfig {
    private MoltenVKConfig() {}

    private static boolean applied = false;

    // Safe defaults for Apple Silicon.
    private static final Map<String, String> SAFE_CONFIG = new LinkedHashMap<>();
    static {
        SAFE_CONFIG.put("MVK_CONFIG_USE_METAL_ARGUMENT_BUFFERS", "1");
        SAFE_CONFIG.put("MVK_CONFIG_SHOULD_MAXIMIZE_CONCURRENT_COMPILATION", "1");
        SAFE_CONFIG.put("MVK_CONFIG_FAST_MATH_ENABLED", "1");
    }

    // Aggressive profile: -Dvulkanmod.mvk.aggressive=true or a moltenvk-aggressive marker file. May hurt stability.
    private static final Map<String, String> AGGRESSIVE_CONFIG = new LinkedHashMap<>();
    static {
        AGGRESSIVE_CONFIG.put("MVK_CONFIG_SYNCHRONOUS_QUEUE_SUBMITS", "0");
        AGGRESSIVE_CONFIG.put("MVK_CONFIG_PREFILL_METAL_COMMAND_BUFFERS", "1");
        AGGRESSIVE_CONFIG.put("MVK_CONFIG_USE_MTLHEAP", "1");
    }

    private static boolean aggressiveEnabled() {
        if (Boolean.getBoolean("vulkanmod.mvk.aggressive")) return true;
        try {
            java.nio.file.Path dir = net.neoforged.fml.loading.FMLPaths.GAMEDIR.get();
            return java.nio.file.Files.exists(dir.resolve("moltenvk-aggressive"));
        } catch (Throwable t) {
            return false;
        }
    }

    // Perf diagnostics toggle: -Dvulkanmod.perf=true or a moltenvk-diag marker file. Gates FrameTimer.
    public static boolean perfDiagEnabled() {
        if (Boolean.getBoolean("vulkanmod.perf")) return true;
        try {
            java.nio.file.Path dir = net.neoforged.fml.loading.FMLPaths.GAMEDIR.get();
            return java.nio.file.Files.exists(dir.resolve("moltenvk-diag"));
        } catch (Throwable t) {
            return false;
        }
    }

    private static boolean configDisabled() {
        if ("off".equalsIgnoreCase(System.getProperty("vulkanmod.mvkconfig", ""))) return true;
        try {
            java.nio.file.Path dir = net.neoforged.fml.loading.FMLPaths.GAMEDIR.get();
            return java.nio.file.Files.exists(dir.resolve("mvk-no-config"));
        } catch (Throwable t) {
            return false;
        }
    }

    // Vulkan validation toggle: -Dvulkanmod.validation=true or a vulkan-validation marker file.
    // Routes LWJGL through the loader SDK so the validation layer can be inserted.
    public static boolean validationEnabled() {
        if (Boolean.getBoolean("vulkanmod.validation")) return true;
        try {
            return java.nio.file.Files.exists(net.neoforged.fml.loading.FMLPaths.GAMEDIR.get().resolve("vulkan-validation"));
        } catch (Throwable t) {
            return false;
        }
    }

    // Newest ~/VulkanSDK/<version> with the macOS loader, or null.
    private static java.nio.file.Path findVulkanSdk() {
        java.nio.file.Path base = java.nio.file.Path.of(System.getProperty("user.home"), "VulkanSDK");
        if (!java.nio.file.Files.isDirectory(base)) return null;
        try (var s = java.nio.file.Files.list(base)) {
            return s.filter(p -> java.nio.file.Files.exists(p.resolve("macOS/lib/libvulkan.dylib")))
                    .max(java.util.Comparator.comparing(p -> p.getFileName().toString()))
                    .orElse(null);
        } catch (Throwable t) {
            return null;
        }
    }

    private static void setEnvNative(long setenv, String key, String value) {
        try (MemoryStack stack = stackPush()) {
            invokePPI(MemoryUtil.memAddress(stack.UTF8(key)), MemoryUtil.memAddress(stack.UTF8(value)), 1, setenv);
        }
    }

    // Point LWJGL at the SDK loader and set ICD/layer paths so validation layers load.
    private static void setupValidation(long setenv) {
        java.nio.file.Path sdk = findVulkanSdk();
        if (sdk == null) {
            Initializer.LOGGER.warn("Vulkan validation requested but no SDK found under ~/VulkanSDK");
            return;
        }
        java.nio.file.Path mac = sdk.resolve("macOS");
        setEnvNative(setenv, "VK_ICD_FILENAMES", mac.resolve("share/vulkan/icd.d/MoltenVK_icd.json").toString());
        setEnvNative(setenv, "VK_LAYER_PATH", mac.resolve("share/vulkan/explicit_layer.d").toString());
        // load the loader, not MoltenVK directly, so the validation layer can be inserted
        org.lwjgl.system.Configuration.VULKAN_LIBRARY_NAME.set(mac.resolve("lib/libvulkan.dylib").toString());
        Initializer.LOGGER.info("Vulkan validation: loader + layers from {}", sdk);
    }

    // No-op off macOS, on repeat calls, or if libc setenv is unreachable.
    public static void apply() {
        if (applied) return;
        applied = true;

        if (!Platform.isMacOS()) return;

        // mvk-no-config marker file (or -Dvulkanmod.mvkconfig=off) skips the tuning
        if (configDisabled()) {
            Initializer.LOGGER.info("MoltenVK config: DISABLED via toggle — running on driver defaults (A/B baseline).");
            return;
        }

        try {
            SharedLibrary libc = Library.loadNative(MoltenVKConfig.class, "net.vulkanmod.vulkan", "c");
            long setenv = libc.getFunctionAddress("setenv");
            if (setenv == 0L) {
                Initializer.LOGGER.warn("MoltenVK config: libc setenv not found; using driver defaults.");
                return;
            }

            if (validationEnabled()) {
                setupValidation(setenv);
            }

            Map<String, String> config = new LinkedHashMap<>(SAFE_CONFIG);
            boolean aggressive = aggressiveEnabled();
            if (aggressive) config.putAll(AGGRESSIVE_CONFIG);

            int set = 0;
            try (MemoryStack stack = stackPush()) {
                for (Map.Entry<String, String> entry : config.entrySet()) {
                    // don't overwrite a value set in the launch environment
                    if (System.getenv(entry.getKey()) != null) continue;

                    long name = MemoryUtil.memAddress(stack.UTF8(entry.getKey()));
                    long value = MemoryUtil.memAddress(stack.UTF8(entry.getValue()));
                    // int setenv(const char *name, const char *value, int overwrite)
                    int rc = invokePPI(name, value, 1, setenv);
                    if (rc == 0) {
                        set++;
                    } else {
                        Initializer.LOGGER.warn("MoltenVK config: setenv({}) failed rc={}", entry.getKey(), rc);
                    }
                }
            }

            Initializer.LOGGER.info("MoltenVK config: applied {} value(s): {}{}", set, config.keySet(),
                    aggressive ? " [AGGRESSIVE]" : "");
        } catch (Throwable t) {
            // don't let tuning break boot
            Initializer.LOGGER.warn("MoltenVK config: could not apply tuning ({}); using driver defaults.", t.toString());
        }
    }
}
