package dev.ii8we.acquiredutils.client;

import dev.ii8we.acquiredutils.AcquiredUtils;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

/** Sends a compact security report to the private AcquiredUtils relay. */
public final class CompatibilityReportSender {
    private static final String REPORT_ENDPOINT =
        "https://acquiredutils.bombadzalge.workers.dev/report";

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build();

    private CompatibilityReportSender() {
    }

    public static void sendAsync(List<ClientCompatibilityChecker.SecurityDetection> detections) {
        if (detections == null || detections.isEmpty()) {
            return;
        }

        Thread.startVirtualThread(() -> send(detections));
    }

    private static void send(List<ClientCompatibilityChecker.SecurityDetection> detections) {
        try {
            Minecraft minecraft = Minecraft.getInstance();
            String playerName = minecraft.getUser() != null
                ? minecraft.getUser().getName()
                : "Unknown";
            String minecraftVersion = FabricLoader.getInstance().getRawGameVersion();
            String loaderVersion = FabricLoader.getInstance()
                .getModContainer("fabricloader")
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("Unknown");
            String acquiredUtilsVersion = FabricLoader.getInstance()
                .getModContainer(AcquiredUtils.MOD_ID)
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("Unknown");

            String reportId = UUID.randomUUID().toString();

            StringBuilder json = new StringBuilder(1024);
            json.append('{');
            appendString(json, "reportId", reportId);
            appendString(json, "playerName", playerName);
            appendString(json, "minecraftVersion", minecraftVersion);
            appendString(json, "loaderVersion", loaderVersion);
            appendString(json, "acquiredUtilsVersion", acquiredUtilsVersion);
            appendString(json, "method", "Fabric mod/security fingerprints");
            appendString(json, "status", "SUS");
            // Keep the first detection available as top-level fields too.
            // This makes the relay backward-compatible with older report formats.
            ClientCompatibilityChecker.SecurityDetection firstDetection = detections.get(0);
            appendString(json, "modName", firstDetection.modName());
            appendString(json, "modId", firstDetection.modId());
            appendString(json, "modVersion", firstDetection.modVersion());
            appendString(json, "detectionMethod", firstDetection.method());
            appendString(json, "detectionAction", firstDetection.action());
            json.append(",\"detections\":[");

            int reportCount = Math.min(detections.size(), 8);
            for (int index = 0; index < reportCount; index++) {
                if (index > 0) {
                    json.append(',');
                }

                ClientCompatibilityChecker.SecurityDetection detection = detections.get(index);
                json.append('{');
                appendString(json, "modName", detection.modName());
                appendString(json, "modId", detection.modId());
                appendString(json, "modVersion", detection.modVersion());
                appendString(json, "method", detection.method());
                appendString(json, "action", detection.action());
                json.append('}');
            }

            json.append(']');
            json.append('}');

            if (detections.size() > reportCount) {
                AcquiredUtils.LOGGER.warn(
                    "[AcquiredUtils Security] {} additional detection(s) were omitted from the Discord payload because the relay accepts at most 8 detections per report.",
                    detections.size() - reportCount
                );
            }

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(REPORT_ENDPOINT))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .header("User-Agent", "AcquiredUtils-Security/1")
                .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                .build();

            HttpResponse<String> response = null;
            for (int attempt = 1; attempt <= 3; attempt++) {
                try {
                    response = HTTP_CLIENT.send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                    );
                } catch (IOException exception) {
                    if (attempt == 3) {
                        throw exception;
                    }
                    Thread.sleep(750L * attempt);
                    continue;
                }

                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    AcquiredUtils.LOGGER.info(
                        "[AcquiredUtils Security] Security report sent successfully for {} detection(s).",
                        detections.size()
                    );
                    return;
                }

                // Retry only transient relay/server failures. A 4xx response
                // is normally a bad request/configuration and should not be
                // retried three times.
                if (response.statusCode() < 500 || attempt == 3) {
                    AcquiredUtils.LOGGER.warn(
                        "[AcquiredUtils Security] Security report relay returned HTTP {}: {}",
                        response.statusCode(),
                        response.body()
                    );
                    return;
                }

                Thread.sleep(750L * attempt);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            AcquiredUtils.LOGGER.warn(
                "[AcquiredUtils Security] Security report sending was interrupted."
            );
        } catch (IOException | RuntimeException exception) {
            AcquiredUtils.LOGGER.warn(
                "[AcquiredUtils Security] Failed to send security report.",
                exception
            );
        }
    }

    private static void appendString(StringBuilder json, String key, String value) {
        if (json.length() > 1 && json.charAt(json.length() - 1) != '{') {
            json.append(',');
        }

        json.append(quote(key)).append(':').append(quote(value));
    }

    private static String quote(String value) {
        if (value == null) {
            return "null";
        }

        StringBuilder builder = new StringBuilder(value.length() + 2);
        builder.append('"');

        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '\\' -> builder.append("\\\\");
                case '"' -> builder.append("\\\"");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> {
                    if (character < 0x20) {
                        builder.append(String.format("\\u%04x", (int) character));
                    } else {
                        builder.append(character);
                    }
                }
            }
        }

        builder.append('"');
        return builder.toString();
    }
}
