package dev.bobodado.acquiredutils.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.bobodado.acquiredutils.AcquiredUtils;
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
    public boolean recipeUnlockHighlightEnabled = true;

    public boolean rarityCircleEnabled = true;
    public float rarityCircleSize = 5.0f;
    public float rarityCircleOpacity = 1.0f;

    public boolean itemComparisonEnabled = true;

    public boolean slotLockEnabled = false;
    public int slotLockKey = -1;
    public int inventorySearchKey = 76;



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

    public void sanitize() {
        if (lockedSlots == null) {
            lockedSlots = new LinkedHashSet<>();
        }
        notificationDuration = Math.max(0.5f, Math.min(10.0f, notificationDuration));
        notificationPositionX = Math.max(0.0f, Math.min(1.0f, notificationPositionX));
        notificationPositionY = Math.max(0.0f, Math.min(1.0f, notificationPositionY));
        menuScale = Math.max(0.5f, Math.min(1.5f, menuScale));
        rarityCircleSize = Math.max(3.0f, Math.min(8.0f, rarityCircleSize));
        rarityCircleOpacity = Math.max(0.15f, Math.min(1.0f, rarityCircleOpacity));


        lockedSlots.removeIf(idx -> idx == null || idx < 0 || idx > 40);
    }

    public static void load() {
        Path path = configPath();
        if (!Files.exists(path)) {
            INSTANCE = new AcquiredUtilsConfig();
            INSTANCE.sanitize();
            save();
            return;
        }

        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            AcquiredUtilsConfig loaded = GSON.fromJson(reader, AcquiredUtilsConfig.class);
            INSTANCE = (loaded != null) ? loaded : new AcquiredUtilsConfig();
            INSTANCE.sanitize();
            AcquiredUtils.LOGGER.info("[AcquiredUtils] Loaded config from {}", path);
        } catch (Exception e) {
            AcquiredUtils.LOGGER.error("[AcquiredUtils] Failed to read config, falling back to defaults", e);
            INSTANCE = new AcquiredUtilsConfig();
            INSTANCE.sanitize();
        }
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