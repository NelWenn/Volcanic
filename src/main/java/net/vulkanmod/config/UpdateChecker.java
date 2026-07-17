package net.vulkanmod.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.vulkanmod.Initializer;
import org.apache.maven.artifact.versioning.ComparableVersion;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.concurrent.CompletableFuture;

public abstract class UpdateChecker {
    public static final String RELEASES_URL = "https://github.com/TrulyRin/VulkanMod-Reforged/releases";
    private static final String LATEST_RELEASE_API = "https://api.github.com/repos/TrulyRin/VulkanMod-Reforged/releases/latest";

    private static boolean updateAvailable = false;

    public static void checkForUpdates() {
        CompletableFuture.runAsync(() -> {
            try {
                String current = Initializer.getVersion();
                if (current.contains("dev") || current.contains("ALPHA") || current.contains("BETA")) {
                    Initializer.LOGGER.info("Pre-release version, skipping update check.");
                    return;
                }

                HttpURLConnection http = (HttpURLConnection) URI.create(LATEST_RELEASE_API).toURL().openConnection();
                http.setConnectTimeout(5000);
                http.setReadTimeout(5000);

                JsonObject data;
                try (var inputStream = http.getInputStream()) {
                    data = JsonParser.parseString(new String(inputStream.readAllBytes())).getAsJsonObject();
                } finally {
                    http.disconnect();
                }

                String latest = data.get("tag_name").getAsString().replaceFirst("^v", "");

                updateAvailable = new ComparableVersion(current).compareTo(new ComparableVersion(latest)) < 0;

                if (updateAvailable) {
                    Initializer.LOGGER.info("Update available!");
                }
            } catch (IOException | RuntimeException e) {
                Initializer.LOGGER.info("Skipping update check: {}", e.toString());
            }
        });
    }

    public static boolean isUpdateAvailable() {
        return updateAvailable;
    }
}
