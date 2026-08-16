package dev.ii8we.acquiredutils.client;

import dev.ii8we.acquiredutils.AcquiredUtils;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

/**
 * Early client-side security check for known hacked/cheat Fabric clients.
 *
 * <p>The check never crashes Minecraft and never modifies another mod's files.
 * Every detected blocked mod is added to one combined security report that is
 * sent after the scan finishes.
 *
 * <p>Detection intentionally uses a small set of verified Fabric IDs plus
 * exact metadata-name fingerprints for confirmed clients whose published
 * Fabric ID is not yet independently verified here. Ambiguous utility mods,
 * security mods, and display-name-only candidates are not automatically reported.
 */
public final class ClientCompatibilityChecker {
    /** Verified/known Fabric IDs that should be reported automatically. */
    private static final Set<String> BLOCKED_MOD_IDS = Set.of(
        "meteor-client",
        "wurst",
        "liquidbounce",
        "bleachhack",
        "aristois",
        "inertia",
        "impact",
        "future",
        "rusherhack",
        "thunderhack",
        "astran",
        "cheatutils",
        "aoba",
        "prestige"
    );

    /** Exact normalized display-name fingerprints for confirmed cheats. */
    private static final Set<String> BLOCKED_MOD_NAME_FINGERPRINTS = Set.of(
        normalizeName("Meteor Client"),
        normalizeName("Wurst Client"),
        normalizeName("LiquidBounce"),
        normalizeName("BleachHack"),
        normalizeName("Aristois"),
        normalizeName("Inertia Client"),
        normalizeName("Impact Client"),
        normalizeName("Future Client"),
        normalizeName("RusherHack"),
        normalizeName("ThunderHack"),
        normalizeName("Astran"),
        normalizeName("CheatUtils"),
        normalizeName("Aoba Client"),
        normalizeName("Prestige Client"),
        normalizeName("nyxi's Cheat Mod"),
        normalizeName("22QQ Client")
    );

    private ClientCompatibilityChecker() {
    }

    public static void check() {
        FabricLoader loader = FabricLoader.getInstance();
        List<SecurityDetection> detections = new ArrayList<>();
        Set<String> detectedContainers = new HashSet<>();

        for (ModContainer container : loader.getAllMods()) {
            String modId = container.getMetadata().getId();
            String displayName = safeName(container, modId);
            String normalizedName = normalizeName(displayName);

            boolean blockedById = BLOCKED_MOD_IDS.contains(modId);
            boolean blockedByName = BLOCKED_MOD_NAME_FINGERPRINTS.contains(normalizedName);
            if (!blockedById && !blockedByName) {
                continue;
            }

            String uniqueKey = modId + "|" + normalizedName;
            if (!detectedContainers.add(uniqueKey)) {
                continue;
            }

            String version = container.getMetadata().getVersion().getFriendlyString();
            DetectionMethod method = blockedById
                ? DetectionMethod.FABRIC_MOD_ID
                : DetectionMethod.METADATA_NAME_FINGERPRINT;

            String action = "Detected; no files modified";

            AcquiredUtils.LOGGER.warn(
                "[AcquiredUtils Security] SUS player detected: {} ({}) v{}; detection={}; action={}",
                displayName,
                modId,
                version,
                method.label,
                action
            );

            detections.add(new SecurityDetection(
                displayName,
                modId,
                version,
                method.label,
                action
            ));
        }

        if (!detections.isEmpty()) {
            CompatibilityReportSender.sendAsync(detections);
        }
    }

    private static String safeName(ModContainer container, String fallback) {
        String name = container.getMetadata().getName();
        return name == null || name.isBlank() ? fallback : name;
    }

    private static String normalizeName(String value) {
        if (value == null) {
            return "";
        }

        return value
            .trim()
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+", "");
    }

    public record SecurityDetection(
        String modName,
        String modId,
        String modVersion,
        String method,
        String action
    ) {
    }

    private enum DetectionMethod {
        FABRIC_MOD_ID("Fabric mod ID"),
        METADATA_NAME_FINGERPRINT("Fabric metadata name fingerprint");

        private final String label;

        DetectionMethod(String label) {
            this.label = label;
        }
    }
}
