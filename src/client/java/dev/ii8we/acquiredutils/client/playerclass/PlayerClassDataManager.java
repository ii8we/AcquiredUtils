package dev.ii8we.acquiredutils.client.playerclass;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.ii8we.acquiredutils.AcquiredUtils;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.EnumMap;
import java.util.Map;

/**
 * Stores only the player class and its abilities locally. Nothing is uploaded or shared.
 * Runtime HUD values such as pet and level are never written to class data files.
 * Each class has its own folder and data.json file.
 */
public final class PlayerClassDataManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String ROOT_FOLDER = "acquiredutils";
    private static final String CLASS_FOLDER = "player_classes";

    private static final Map<PlayerClass, PlayerClassData> DATA = new EnumMap<>(PlayerClass.class);
    private static PlayerClass activeClass = PlayerClass.WARRIOR;
    private static boolean initialized;

    private PlayerClassDataManager() {}

    public static void initialize() {
        if (initialized) return;
        initialized = true;
        try {
            Files.deleteIfExists(rootDirectory().resolve("active_class.json"));
        } catch (Exception ignored) {
            // Compatibility cleanup only; class data remains unaffected.
        }
        for (PlayerClass playerClass : PlayerClass.values()) {
            loadClass(playerClass);
        }
    }

    public static PlayerClass getActiveClass() {
        initialize();
        return activeClass;
    }

    public static PlayerClassData getActiveData() {
        initialize();
        return DATA.get(activeClass);
    }

    public static void setActiveClass(PlayerClass playerClass) {
        initialize();
        if (playerClass == null) return;
        activeClass = playerClass;
    }

    public static PlayerClassData getData(PlayerClass playerClass) {
        initialize();
        return DATA.get(playerClass);
    }

    public static void saveClass(PlayerClass playerClass) {
        initialize();
        PlayerClassData data = DATA.get(playerClass);
        if (data == null) return;
        data.sanitize(playerClass);
        Path file = classDirectory(playerClass).resolve("data.json");
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
                GSON.toJson(data, writer);
            }
        } catch (Exception e) {
            AcquiredUtils.LOGGER.error("[AcquiredUtils] Failed to save {} class data", playerClass.displayName(), e);
        }
    }

    private static void loadClass(PlayerClass playerClass) {
        Path file = classDirectory(playerClass).resolve("data.json");
        PlayerClassData data = null;
        if (Files.exists(file)) {
            try {
                String json = Files.readString(file, StandardCharsets.UTF_8);
                data = parseClassData(json, playerClass);
            } catch (Exception e) {
                AcquiredUtils.LOGGER.warn("[AcquiredUtils] Failed to read {} class data; recreating it", playerClass.displayName(), e);
            }
        }
        if (data == null) {
            data = PlayerClassData.defaults(playerClass);
        }
        data.sanitize(playerClass);
        DATA.put(playerClass, data);
        saveClassWithoutInitialize(playerClass, data);
    }

    private static PlayerClassData parseClassData(String json, PlayerClass playerClass) {
        JsonObject object = JsonParser.parseString(json).getAsJsonObject();
        JsonElement abilitiesElement = object.get("abilities");
        if (abilitiesElement != null && abilitiesElement.isJsonArray()) {
            JsonArray abilities = abilitiesElement.getAsJsonArray();
            boolean legacyStrings = abilities.size() > 0 && abilities.get(0).isJsonPrimitive();
            if (legacyStrings) {
                PlayerClassData data = GSON.fromJson(object, PlayerClassData.class);
                data.abilities.clear();
                for (JsonElement element : abilities) {
                    if (element.isJsonPrimitive()) {
                        data.abilities.add(new PlayerAbilityData(element.getAsString(), "Not set"));
                    }
                }
                return data;
            }
        }
        return GSON.fromJson(object, PlayerClassData.class);
    }

    private static void saveClassWithoutInitialize(PlayerClass playerClass, PlayerClassData data) {
        Path file = classDirectory(playerClass).resolve("data.json");
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
                GSON.toJson(data, writer);
            }
        } catch (Exception e) {
            AcquiredUtils.LOGGER.error("[AcquiredUtils] Failed to create {} class data", playerClass.displayName(), e);
        }
    }

    private static Path rootDirectory() {
        return FabricLoader.getInstance().getConfigDir().resolve(ROOT_FOLDER).resolve(CLASS_FOLDER);
    }

    private static Path classDirectory(PlayerClass playerClass) {
        return rootDirectory().resolve(playerClass.folderName());
    }

}
