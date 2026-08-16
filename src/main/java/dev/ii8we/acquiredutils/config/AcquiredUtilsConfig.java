package dev.ii8we.acquiredutils.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.ii8we.acquiredutils.AcquiredUtils;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashSet;
import java.util.Set;

public final class AcquiredUtilsConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "acquiredutils.json";
    private static AcquiredUtilsConfig INSTANCE = new AcquiredUtilsConfig();

    public boolean itemPickupNotifierEnabled = true;
    public float notificationDuration = 3.0f;
    public float notificationPositionX = 0.5f;
    public float notificationPositionY = 0.25f;
    public float menuScale = 1.0f;

    // First-person held-item position controls. Values are offsets applied relative
    // to vanilla hand placement once the render hook is enabled.
    public boolean itemPositionEnabled = true;
    public float mainHandPositionX = 0.0f;
    public float mainHandPositionY = 0.0f;
    public float mainHandPositionZ = 0.0f;
    public float offhandPositionX = 0.0f;
    public float offhandPositionY = 0.0f;
    public float offhandPositionZ = 0.0f;
    public float mainHandRotationX = 0.0f;
    public float mainHandRotationY = 0.0f;
    public float mainHandRotationZ = 0.0f;
    public float offhandRotationX = 0.0f;
    public float offhandRotationY = 0.0f;
    public float offhandRotationZ = 0.0f;
    public float mainHandScale = 1.0f;
    public float offhandScale = 1.0f;
    public boolean recipeUnlockHighlightEnabled = true;

    public boolean rarityCircleEnabled = true;

    public boolean itemComparisonEnabled = true;
    public boolean inventorySearchEnabled = true;
    public boolean inventoryFullWarningEnabled = true;

    public boolean slotLockEnabled = false;
    public int slotLockKey = -1;


    public Set<Integer> lockedSlots = new LinkedHashSet<>();

    private transient boolean dirty = false;

    private AcquiredUtilsConfig() {
    }

    public static AcquiredUtilsConfig get() {
        return INSTANCE;
    }

    private static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
    }

    public void markDirty() {
        this.dirty = true;
    }

    public static AcquiredUtilsConfig defaults() {
        AcquiredUtilsConfig config = new AcquiredUtilsConfig();
        config.sanitize();
        return config;
    }

    public static void resetToDefaults() {
        INSTANCE = defaults();
    }

    public void sanitize() {
        if (lockedSlots == null) {
            lockedSlots = new LinkedHashSet<>();
        }
        notificationDuration = Math.max(0.5f, Math.min(10.0f, notificationDuration));
        notificationPositionX = Math.max(0.0f, Math.min(1.0f, notificationPositionX));
        notificationPositionY = Math.max(0.0f, Math.min(1.0f, notificationPositionY));
        menuScale = clampMenuScale(menuScale);
        if (Float.isNaN(menuScale) || Float.isInfinite(menuScale)) menuScale = 1.0f;

        mainHandPositionX = clampItemOffset(mainHandPositionX);
        mainHandPositionY = clampItemOffset(mainHandPositionY);
        mainHandPositionZ = clampItemOffset(mainHandPositionZ);
        offhandPositionX = clampItemOffset(offhandPositionX);
        offhandPositionY = clampItemOffset(offhandPositionY);
        offhandPositionZ = clampItemOffset(offhandPositionZ);
        mainHandRotationX = clampRotation(mainHandRotationX);
        mainHandRotationY = clampRotation(mainHandRotationY);
        mainHandRotationZ = clampRotation(mainHandRotationZ);
        offhandRotationX = clampRotation(offhandRotationX);
        offhandRotationY = clampRotation(offhandRotationY);
        offhandRotationZ = clampRotation(offhandRotationZ);
        mainHandScale = clampScale(mainHandScale);
        offhandScale = clampScale(offhandScale);


        lockedSlots.removeIf(idx -> idx == null || idx < 0 || idx > 40);
    }

    private static float clampMenuScale(float value) {
        if (!Float.isFinite(value)) return 1.0f;
        return Math.max(0.5f, Math.min(1.5f, value));
    }

    private static float clampRotation(float value) {
        if (!Float.isFinite(value)) return 0.0f;
        return Math.max(-180.0f, Math.min(180.0f, value));
    }

    private static float clampScale(float value) {
        if (!Float.isFinite(value)) return 1.0f;
        return Math.max(0.25f, Math.min(2.0f, value));
    }

    private static float clampItemOffset(float value) {
        if (!Float.isFinite(value)) {
            return 0.0f;
        }
        return Math.max(-1.0f, Math.min(1.0f, value));
    }

    public static void load() {
        Path path = configPath();
        if (!Files.exists(path)) {
            INSTANCE = defaults();
            save();
            return;
        }

        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            AcquiredUtilsConfig loaded = GSON.fromJson(reader, AcquiredUtilsConfig.class);
            INSTANCE = (loaded != null) ? loaded : defaults();
            INSTANCE.sanitize();
            AcquiredUtils.LOGGER.info("[AcquiredUtils] Loaded config from {}", path);
        } catch (Exception e) {
            AcquiredUtils.LOGGER.error("[AcquiredUtils] Failed to read config, falling back to defaults", e);
            INSTANCE = defaults();
        }
    }


    public static void replace(AcquiredUtilsConfig loaded) {
        if (loaded == null) return;
        loaded.sanitize();
        loaded.dirty = false;
        INSTANCE = loaded;
    }

    public static void save() {
        Path path = configPath();
        Path temp = path.resolveSibling(FILE_NAME + ".tmp");
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
                GSON.toJson(INSTANCE, writer);
            }
            Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
            INSTANCE.dirty = false;
            AcquiredUtils.LOGGER.info("[AcquiredUtils] Saved config to {}", path);
        } catch (Exception e) {
            AcquiredUtils.LOGGER.error("[AcquiredUtils] Failed to write config", e);
            try {
                Files.deleteIfExists(temp);
            } catch (Exception ignored) {
            }
        }
    }

    public static void saveIfDirty() {
        if (INSTANCE != null && INSTANCE.dirty) {
            save();
        }
    }
}